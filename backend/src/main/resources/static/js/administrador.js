/*
 * administrador.js — lógica de la página del administrador (administrador.html).
 * Usa las funciones comunes de api.js.
 */

/** Id del reporte que se está revisando en la sección "Reporte seleccionado". */
let reporteActual = null;

// ------------------------------------------------------------------ Panel (HU-15)

async function cargarPanel() {
    const p = await Api.get('/api/admin/estadisticas/panel');
    const indicador = (nombre, valor) => '<div class="indicador"><b>' + valor + '</b>' + nombre + '</div>';
    document.getElementById('panel').innerHTML = indicador('Total', p.totalReportes) +
        indicador('Pendientes', p.pendientes) + indicador('Asignados', p.asignados) +
        indicador('En proceso', p.enProceso) + indicador('Resueltos', p.resueltos);
    document.getElementById('frecuentes').textContent = Object.entries(p.problemasFrecuentes)
        .filter(([, n]) => n > 0).map(([tipo, n]) => texto(tipo) + ': ' + n).join(' · ') || 'Sin reportes';
    document.getElementById('sectores').textContent = p.sectoresMayorAcumulacion
        .map(s => s.sector + ': ' + s.reportes).join(' · ') || 'Sin reportes';
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
        enlaceMapa(r.latitud, r.longitud) + '</p>' +
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

// ------------------------------------------------------------------ Contenido educativo (HU-14)

async function cargarContenidos() {
    const contenidos = await Api.get('/api/admin/contenidos');
    document.getElementById('contenidos').innerHTML = contenidos.map(c => '<div class="tarjeta"><b>' + escapar(c.titulo) +
        '</b> <span class="ayuda">' + escapar(c.categoria) + ' · ' + (c.publicado ? 'Publicado' : 'Borrador') +
        '</span> <button class="pequeno secundario" onclick="cambiarPublicacion(\'' + c.id + '\', ' + !c.publicado + ')">' +
        (c.publicado ? 'Retirar' : 'Publicar') + '</button></div>').join('') || '<p class="ayuda">Sin contenido.</p>';
}

async function cambiarPublicacion(id, publicar) {
    const ruta = '/api/admin/contenidos/' + id + '/publicacion';
    await (publicar ? Api.post(ruta) : Api.borrar(ruta));
    cargarContenidos();
}

document.getElementById('formContenido').addEventListener('submit', async (evento) => {
    evento.preventDefault();
    const ok = await conMensaje('msgContenido', () => Api.post('/api/admin/contenidos', {
        titulo: document.getElementById('cTitulo').value,
        categoria: document.getElementById('cCategoria').value,
        cuerpo: document.getElementById('cCuerpo').value,
        publicar: document.getElementById('cPublicar').checked
    }), 'Contenido guardado');
    if (ok) { document.getElementById('formContenido').reset(); cargarContenidos(); }
});

// ------------------------------------------------------------------ Puntos de recolección (HU-13)

async function cargarPuntos() {
    const puntos = await Api.get('/api/admin/puntos');
    document.getElementById('puntosAdmin').innerHTML = puntos.map(p => '<div class="tarjeta"><b>' + escapar(p.nombre) +
        '</b> <span class="ayuda">' + escapar(p.sector || '—') + ' · ' + escapar(p.materiales.join(', ')) + ' · ' +
        (p.activo ? 'Activo' : 'Inactivo') + '</span> <button class="pequeno secundario" onclick="cambiarActivacion(\'' +
        p.id + '\', ' + !p.activo + ')">' + (p.activo ? 'Desactivar' : 'Activar') + '</button></div>').join('');
}

async function cambiarActivacion(id, activar) {
    await (activar ? Api.post('/api/admin/puntos/' + id + '/activacion') : Api.borrar('/api/admin/puntos/' + id));
    cargarPuntos();
}

document.getElementById('formPunto').addEventListener('submit', async (evento) => {
    evento.preventDefault();
    const materiales = [...document.getElementById('pMateriales').selectedOptions].map(o => o.value);
    const ok = await conMensaje('msgPunto', () => Api.post('/api/admin/puntos', {
        nombre: document.getElementById('pNombre').value,
        direccion: document.getElementById('pDireccion').value,
        latitud: parseFloat(document.getElementById('pLatitud').value),
        longitud: parseFloat(document.getElementById('pLongitud').value),
        materiales: materiales,
        horarioAtencion: document.getElementById('pHorario').value
    }), 'Punto creado (el sector se identificó por la ubicación)');
    if (ok) { document.getElementById('formPunto').reset(); cargarPuntos(); }
});

// ------------------------------------------------------------------ Al cargar la página
(async function iniciar() {
    if (!await exigirRol('ADMINISTRADOR')) return;
    // Informe: por defecto el mes actual
    const hoy = new Date();
    document.getElementById('desde').value = new Date(hoy.getFullYear(), hoy.getMonth(), 1).toISOString().substring(0, 10);
    document.getElementById('hasta').value = hoy.toISOString().substring(0, 10);
    await Promise.all([cargarSectores('fSector', 'Todos'), cargarSectores('iSector', 'Todos')]);
    await Promise.all([cargarPanel(), cargarBandeja(), cargarOperadores(), cargarContenidos(), cargarPuntos()]);
})();
