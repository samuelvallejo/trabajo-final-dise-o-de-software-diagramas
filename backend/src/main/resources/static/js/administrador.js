/*
 * administrador.js — lógica de la página del administrador (administrador.html).
 * Usa las funciones comunes de api.js y mapas.js.
 */

/** Id del reporte que se está revisando en la sección "Reporte seleccionado". */
let reporteActual = null;
/** Ids de la ruta, el punto o el contenido que se están editando (null = se está creando uno nuevo). */
let rutaEditada = null, puntoEditado = null, contenidoEditado = null;
let rutasCargadas = [], puntosCargados = [], contenidosCargados = [];
let mapaCalor, capaCalor, mapaPunto, marcadorPunto;

const DIAS = [['MONDAY', 'Lunes'], ['TUESDAY', 'Martes'], ['WEDNESDAY', 'Miércoles'], ['THURSDAY', 'Jueves'],
    ['FRIDAY', 'Viernes'], ['SATURDAY', 'Sábado'], ['SUNDAY', 'Domingo']];

// ------------------------------------------------------------------ Panel y mapa de calor (HU-15)

async function cargarPanel() {
    const p = await Api.get('/api/admin/estadisticas/panel');
    const indicador = (nombre, valor) => '<div class="indicador"><b>' + valor + '</b>' + nombre + '</div>';
    document.getElementById('panel').innerHTML = indicador('Total', p.totalReportes) +
        indicador('Pendientes', p.pendientes) + indicador('Asignados', p.asignados) +
        indicador('En proceso', p.enProceso) + indicador('Resueltos', p.resueltos);
    document.getElementById('frecuentes').textContent = Object.entries(p.problemasFrecuentes)
        .filter(([, n]) => n > 0).map(([tipo, n]) => texto(tipo) + ': ' + n).join(' · ') || 'Sin reportes';
    const sectores = p.sectoresMayorAcumulacion;
    document.getElementById('sectores').innerHTML = sectores.length === 0 ? '<p class="ayuda">Sin reportes</p>'
        : '<ol>' + sectores.map(s => '<li>' + escapar(s.sector) + ': <b>' + s.reportes + '</b></li>').join('') + '</ol>';

    // Mapa de calor: un círculo por sector en el punto medio de sus reportes; más reportes = más grande e intenso
    capaCalor.clearLayers();
    const maximo = Math.max(1, ...sectores.map(s => s.reportes));
    sectores.filter(s => s.latitudPromedio !== 0).forEach(s => L.circle([s.latitudPromedio, s.longitudPromedio], {
        radius: 150 + 450 * s.reportes / maximo, color: '#b3261e', weight: 1,
        fillColor: '#b3261e', fillOpacity: 0.15 + 0.5 * s.reportes / maximo
    }).addTo(capaCalor).bindPopup(escapar(s.sector) + ': ' + s.reportes + ' reportes'));
    encuadrar(mapaCalor, capaCalor);
}

// ------------------------------------------------------------------ Bandeja de reportes (HU-05)

async function cargarBandeja() {
    // Solo se envían los filtros que tienen valor
    const filtros = new URLSearchParams();
    for (const [parametro, id] of [['estado', 'fEstado'], ['tipoResiduo', 'fTipo'], ['prioridad', 'fPrioridad'], ['sector', 'fSector']]) {
        const valor = document.getElementById(id).value;
        if (valor) filtros.append(parametro, valor);
    }
    const reportes = await Api.get('/api/reportes?' + filtros);
    document.getElementById('bandeja').innerHTML = reportes.length === 0
        ? '<p class="ayuda">No hay reportes con esos filtros.</p>'
        : '<div class="tabla"><table><tr><th>Fecha</th><th>Descripción</th><th>Tipo</th><th>Prioridad</th>' +
        '<th>Sector</th><th>Estado</th><th>Operador</th><th></th></tr>' +
        reportes.map(r => '<tr' + (r.id === reporteActual ? ' class="seleccionado"' : '') + '><td>' +
            fecha(r.fechaReporte) + '</td><td>' + escapar(r.descripcion) + '</td><td>' + texto(r.tipoResiduo) +
            '</td><td>' + etiqueta(r.prioridad) + '</td><td>' + escapar(r.sector || '—') + '</td><td>' +
            etiqueta(r.estado) + '</td><td>' + escapar(r.operador ? r.operador.nombre : '—') +
            '</td><td><button class="pequeno" onclick="seleccionar(\'' + r.id + '\')">Revisar</button></td></tr>').join('') +
        '</table></div>';
}

/** Muestra el detalle de un reporte con sus acciones: clasificar, asignar y duplicados. */
async function seleccionar(id) {
    reporteActual = id;
    const r = await Api.get('/api/reportes/' + id);
    document.getElementById('seccionDetalle').classList.remove('oculto');
    document.getElementById('msgDetalle').textContent = '';
    document.getElementById('detalle').innerHTML = '<p><b>' + escapar(r.descripcion) + '</b></p>' +
        '<p>Ciudadano: ' + escapar(r.ciudadano ? r.ciudadano.nombre : '—') + ' · ' + fecha(r.fechaReporte) +
        ' · Estado: ' + etiqueta(r.estado) + ' · Prioridad: ' + etiqueta(r.prioridad) + '</p>' +
        '<p>' + escapar(r.direccion || '') + ' · Sector: ' + escapar(r.sector || '—') + ' · ' +
        enlaceComoLlegar(r.latitud, r.longitud) + '</p>' +
        (r.reporteOriginalId ? '<p>Es duplicado del reporte ' + escapar(r.reporteOriginalId) +
            ' <button class="pequeno secundario" onclick="desvincular()">Desvincular</button></p>' : '') +
        galeria(r.fotos);
    document.getElementById('claseTipo').value = r.tipoResiduo;
    document.getElementById('clasePrioridad').value = r.prioridad;
    await Promise.all([cargarOperadoresParaAsignar(), cargarDuplicados()]);
    document.getElementById('seccionDetalle').scrollIntoView({ behavior: 'smooth' });
    cargarBandeja();
}

/** Recarga lo que cambia después de una acción sobre el reporte. */
async function refrescar() {
    await Promise.all([cargarBandeja(), cargarPanel(), cargarOperadores()]);
}

async function clasificar() {
    const ok = await conMensaje('msgDetalle', () => Api.put('/api/reportes/' + reporteActual + '/clasificacion', {
        tipoResiduo: document.getElementById('claseTipo').value,
        prioridad: document.getElementById('clasePrioridad').value
    }), 'Clasificación guardada');
    if (ok) refrescar();
}

// ------------------------------------------------------------------ Asignación (HU-07)

async function cargarOperadoresParaAsignar() {
    const operadores = await Api.get('/api/admin/operadores?disponibles=true');
    document.getElementById('operadorAsignar').innerHTML = operadores.length === 0
        ? '<option value="">No hay operadores disponibles</option>'
        : operadores.map(o => '<option value="' + o.id + '">' + escapar(o.nombre) + ' (' + o.reportesSinResolver +
            ' sin resolver)</option>').join('');
}

async function asignar() {
    const ok = await conMensaje('msgDetalle', () => Api.put('/api/reportes/' + reporteActual + '/asignacion', {
        operadorId: document.getElementById('operadorAsignar').value,
        reasignar: document.getElementById('reasignar').checked
    }), 'Reporte asignado; se notificó al ciudadano');
    if (ok) refrescar();
}

// ------------------------------------------------------------------ Duplicados (HU-06)

async function cargarDuplicados() {
    const posibles = await Api.get('/api/reportes/' + reporteActual + '/posibles-duplicados');
    document.getElementById('duplicados').innerHTML = posibles.length === 0
        ? '<p class="ayuda">No hay reportes abiertos del mismo tipo cerca.</p>'
        : posibles.map(p => '<div class="tarjeta">' + escapar(p.reporte.descripcion) + ' · ' + etiqueta(p.reporte.estado) +
            ' · a ' + p.distanciaMetros + ' m <button class="pequeno" onclick="marcarDuplicado(\'' + p.reporte.id +
            '\')">Marcar este reporte como duplicado de ese</button></div>').join('');
}

async function marcarDuplicado(originalId) {
    const ok = await conMensaje('msgDetalle', () => Api.put('/api/reportes/' + reporteActual + '/duplicado',
        { reporteOriginalId: originalId }), 'Marcado como duplicado');
    if (ok) { await seleccionar(reporteActual); refrescar(); }
}

async function desvincular() {
    const ok = await conMensaje('msgDetalle', () => Api.borrar('/api/reportes/' + reporteActual + '/duplicado'),
        'Reporte desvinculado');
    if (ok) { await seleccionar(reporteActual); refrescar(); }
}

// ------------------------------------------------------------------ Operadores (HU-07)

async function cargarOperadores() {
    const operadores = await Api.get('/api/admin/operadores?disponibles=false');
    document.getElementById('operadores').innerHTML = operadores.length === 0
        ? '<p class="ayuda">Aún no hay operadores.</p>'
        : '<div class="tabla"><table><tr><th>Nombre</th><th>Sin resolver</th><th>Estado</th><th></th></tr>' +
        operadores.map(o => '<tr><td>' + escapar(o.nombre) + '<br><span class="ayuda">' + escapar(o.correo) +
            '</span></td><td>' + o.reportesSinResolver + '</td><td>' + (o.disponible ? 'Disponible' : 'Ocupado') +
            '</td><td><button class="pequeno secundario" onclick="cambiarDisponibilidad(\'' + o.id + '\', ' +
            !o.disponible + ')">' + (o.disponible ? 'Marcar ocupado' : 'Marcar disponible') + '</button></td></tr>').join('') +
        '</table></div>';
}

async function cambiarDisponibilidad(id, disponible) {
    await Api.put('/api/admin/operadores/' + id + '/disponibilidad', { disponible });
    cargarOperadores();
}

document.getElementById('formOperador').addEventListener('submit', async (evento) => {
    evento.preventDefault();
    const ok = await conMensaje('msgOperador', () => Api.post('/api/admin/operadores', {
        nombre: document.getElementById('opNombre').value,
        documento: document.getElementById('opDocumento').value,
        correo: document.getElementById('opCorreo').value,
        contrasena: document.getElementById('opContrasena').value
    }), 'Operador creado');
    if (ok) { document.getElementById('formOperador').reset(); cargarOperadores(); }
});

// ------------------------------------------------------------------ Informe (HU-15)

function parametrosInforme() {
    const p = new URLSearchParams({
        desde: document.getElementById('desde').value,
        hasta: document.getElementById('hasta').value
    });
    const sector = document.getElementById('iSector').value;
    if (sector) p.append('sector', sector);
    return p;
}

async function generarInforme() {
    const i = await conMensaje('msgInforme', () => Api.get('/api/admin/estadisticas/informe?' + parametrosInforme()), ' ');
    if (!i) return;
    document.getElementById('informe').innerHTML = '<p>Total: <b>' + i.totalReportes + '</b> · Pendientes: ' +
        i.pendientes + ' · Resueltos: ' + i.resueltos + '</p><p>Tiempo promedio de atención: ' +
        (i.tiempoPromedioAtencionHoras === null ? '—' : i.tiempoPromedioAtencionHoras + ' horas') + '</p>';
}

/** El navegador descarga el archivo directamente desde la API. */
function descargarCsv() {
    location.href = '/api/admin/estadisticas/informe.csv?' + parametrosInforme();
}

// ------------------------------------------------------------------ Rutas y horarios (HU-12)

async function cargarRutas() {
    rutasCargadas = await Api.get('/api/admin/rutas');
    const nombreDia = Object.fromEntries(DIAS);
    document.getElementById('rutas').innerHTML = rutasCargadas.map(r => '<div class="tarjeta"><b>' + escapar(r.nombre) +
        '</b> <span class="ayuda">' + escapar(r.sectores.join(', ')) + '</span><p>' +
        r.horarios.map(h => nombreDia[h.diaSemana] + ' ' + h.horaInicio.substring(0, 5) + '–' + h.horaFin.substring(0, 5)).join(' · ') +
        '</p><button class="pequeno secundario" onclick="editarRuta(\'' + r.id + '\')">Editar</button></div>').join('') ||
        '<p class="ayuda">Aún no hay rutas.</p>';
}

/** Agrega una fila de horario (día, hora de inicio y hora de fin) al formulario. */
function agregarFilaHorario(dia, inicio, fin) {
    const fila = document.createElement('div');
    fila.className = 'fila filaHorario';
    fila.innerHTML = '<select class="hDia">' + DIAS.map(([v, t]) => '<option value="' + v + '">' + t + '</option>').join('') +
        '</select><input class="hInicio" type="time" required><input class="hFin" type="time" required>' +
        '<button type="button" class="pequeno secundario" style="flex:0">Quitar</button>';
    fila.querySelector('.hDia').value = dia || 'MONDAY';
    fila.querySelector('.hInicio').value = (inicio || '06:00').substring(0, 5);
    fila.querySelector('.hFin').value = (fin || '12:00').substring(0, 5);
    fila.querySelector('button').onclick = () => fila.remove();
    document.getElementById('filasHorario').appendChild(fila);
}

function editarRuta(id) {
    const r = rutasCargadas.find(x => x.id === id);
    rutaEditada = r;
    document.getElementById('tituloRuta').textContent = 'Editar ruta';
    document.getElementById('rNombre').value = r.nombre;
    [...document.getElementById('rSectores').options].forEach(o => o.selected = r.sectores.includes(o.value));
    document.getElementById('filasHorario').innerHTML = '';
    r.horarios.forEach(h => agregarFilaHorario(h.diaSemana, h.horaInicio, h.horaFin));
    document.getElementById('formRuta').scrollIntoView({ behavior: 'smooth' });
}

function limpiarRuta() {
    rutaEditada = null;
    document.getElementById('tituloRuta').textContent = 'Nueva ruta';
    document.getElementById('formRuta').reset();
    document.getElementById('filasHorario').innerHTML = '';
    agregarFilaHorario();
}

document.getElementById('formRuta').addEventListener('submit', async (evento) => {
    evento.preventDefault();
    const datos = {
        nombre: document.getElementById('rNombre').value,
        sectores: [...document.getElementById('rSectores').selectedOptions].map(o => o.value),
        horarios: [...document.querySelectorAll('.filaHorario')].map(f => ({
            diaSemana: f.querySelector('.hDia').value,
            horaInicio: f.querySelector('.hInicio').value,
            horaFin: f.querySelector('.hFin').value
        })),
        recorrido: rutaEditada ? rutaEditada.recorrido : []
    };
    const ok = await conMensaje('msgRuta', () => rutaEditada
        ? Api.put('/api/admin/rutas/' + rutaEditada.id, datos)
        : Api.post('/api/admin/rutas', datos), 'Ruta guardada; los ciudadanos ya ven el cambio');
    if (ok) { limpiarRuta(); cargarRutas(); }
});

// ------------------------------------------------------------------ Puntos de recolección (HU-13)

async function cargarPuntos() {
    puntosCargados = await Api.get('/api/admin/puntos');
    document.getElementById('puntosAdmin').innerHTML = puntosCargados.map(p => '<div class="tarjeta"><b>' + escapar(p.nombre) +
        '</b> <span class="ayuda">' + escapar(p.sector || '—') + ' · ' + escapar(p.materiales.join(', ')) + ' · ' +
        (p.activo ? 'Activo' : 'Inactivo') + '</span><br>' +
        '<button class="pequeno secundario" onclick="editarPunto(\'' + p.id + '\')">Editar</button>' +
        '<button class="pequeno secundario" onclick="cambiarActivacion(\'' + p.id + '\', ' + !p.activo + ')">' +
        (p.activo ? 'Desactivar' : 'Activar') + '</button></div>').join('');
}

async function cambiarActivacion(id, activar) {
    await (activar ? Api.post('/api/admin/puntos/' + id + '/activacion') : Api.borrar('/api/admin/puntos/' + id));
    cargarPuntos();
}

function ubicarPunto(latitud, longitud) {
    document.getElementById('pLatitud').value = latitud.toFixed(6);
    document.getElementById('pLongitud').value = longitud.toFixed(6);
    if (marcadorPunto) marcadorPunto.setLatLng([latitud, longitud]);
    else marcadorPunto = L.marker([latitud, longitud]).addTo(mapaPunto);
}

function editarPunto(id) {
    const p = puntosCargados.find(x => x.id === id);
    puntoEditado = id;
    document.getElementById('tituloPunto').textContent = 'Editar punto';
    document.getElementById('pNombre').value = p.nombre;
    document.getElementById('pDireccion').value = p.direccion;
    document.getElementById('pHorario').value = p.horarioAtencion;
    [...document.getElementById('pMateriales').options].forEach(o => o.selected = p.materiales.includes(o.value));
    ubicarPunto(p.latitud, p.longitud);
    mapaPunto.setView([p.latitud, p.longitud], 15);
    document.getElementById('formPunto').scrollIntoView({ behavior: 'smooth' });
}

function limpiarPunto() {
    puntoEditado = null;
    document.getElementById('tituloPunto').textContent = 'Nuevo punto';
    document.getElementById('formPunto').reset();
    if (marcadorPunto) { mapaPunto.removeLayer(marcadorPunto); marcadorPunto = null; }
}

document.getElementById('formPunto').addEventListener('submit', async (evento) => {
    evento.preventDefault();
    const datos = {
        nombre: document.getElementById('pNombre').value,
        direccion: document.getElementById('pDireccion').value,
        latitud: parseFloat(document.getElementById('pLatitud').value),
        longitud: parseFloat(document.getElementById('pLongitud').value),
        materiales: [...document.getElementById('pMateriales').selectedOptions].map(o => o.value),
        horarioAtencion: document.getElementById('pHorario').value
    };
    const ok = await conMensaje('msgPunto', () => puntoEditado
        ? Api.put('/api/admin/puntos/' + puntoEditado, datos)
        : Api.post('/api/admin/puntos', datos), 'Punto guardado (el sector se identificó por la ubicación)');
    if (ok) { limpiarPunto(); cargarPuntos(); }
});

// ------------------------------------------------------------------ Contenido educativo (HU-14)

async function cargarContenidosAdmin() {
    contenidosCargados = await Api.get('/api/admin/contenidos');
    document.getElementById('contenidosAdmin').innerHTML = contenidosCargados.map(c => '<div class="tarjeta"><b>' +
        escapar(c.titulo) + '</b> <span class="ayuda">' + escapar(TEXTO_CATEGORIA[c.categoria] || c.categoria) + ' · ' +
        (c.publicado ? 'Publicado' : 'Borrador') + '</span><br>' +
        '<button class="pequeno secundario" onclick="editarContenido(\'' + c.id + '\')">Editar</button>' +
        '<button class="pequeno secundario" onclick="cambiarPublicacion(\'' + c.id + '\', ' + !c.publicado + ')">' +
        (c.publicado ? 'Retirar' : 'Publicar') + '</button></div>').join('') || '<p class="ayuda">Sin contenido.</p>';
}

async function cambiarPublicacion(id, publicar) {
    const ruta = '/api/admin/contenidos/' + id + '/publicacion';
    await (publicar ? Api.post(ruta) : Api.borrar(ruta));
    cargarContenidosAdmin();
}

function editarContenido(id) {
    const c = contenidosCargados.find(x => x.id === id);
    contenidoEditado = id;
    document.getElementById('tituloContenido').textContent = 'Editar contenido';
    document.getElementById('cTitulo').value = c.titulo;
    document.getElementById('cCategoria').value = c.categoria;
    document.getElementById('cCuerpo').value = c.cuerpo;
    document.getElementById('cImagen').value = c.imagenUrl || '';
    document.getElementById('cPublicar').checked = c.publicado;
    document.getElementById('formContenido').scrollIntoView({ behavior: 'smooth' });
}

function limpiarContenido() {
    contenidoEditado = null;
    document.getElementById('tituloContenido').textContent = 'Nuevo contenido';
    document.getElementById('formContenido').reset();
}

document.getElementById('formContenido').addEventListener('submit', async (evento) => {
    evento.preventDefault();
    const datos = {
        titulo: document.getElementById('cTitulo').value,
        categoria: document.getElementById('cCategoria').value,
        cuerpo: document.getElementById('cCuerpo').value,
        imagenUrl: document.getElementById('cImagen').value || null,
        publicar: document.getElementById('cPublicar').checked
    };
    const ok = await conMensaje('msgContenido', () => contenidoEditado
        ? Api.put('/api/admin/contenidos/' + contenidoEditado, datos)
        : Api.post('/api/admin/contenidos', datos), 'Contenido guardado');
    if (ok) { limpiarContenido(); cargarContenidosAdmin(); }
});

// ------------------------------------------------------------------ Al cargar la página
(async function iniciar() {
    if (!await exigirRol('ADMINISTRADOR')) return;
    mapaCalor = crearMapa('mapaCalor', 12);
    capaCalor = L.featureGroup().addTo(mapaCalor);
    mapaPunto = crearMapa('mapaPunto');
    mapaPunto.on('click', e => ubicarPunto(e.latlng.lat, e.latlng.lng));

    // Informe: por defecto el mes actual
    const hoy = new Date();
    document.getElementById('desde').value = new Date(hoy.getFullYear(), hoy.getMonth(), 1).toISOString().substring(0, 10);
    document.getElementById('hasta').value = hoy.toISOString().substring(0, 10);
    await Promise.all([cargarSectores('fSector', 'Todos'), cargarSectores('iSector', 'Todos'), cargarSectores('rSectores', '')]);
    document.getElementById('rSectores').querySelector('option[value=""]').remove();
    agregarFilaHorario();
    await Promise.all([cargarPanel(), cargarBandeja(), cargarOperadores(), cargarRutas(), cargarPuntos(), cargarContenidosAdmin()]);
})();
