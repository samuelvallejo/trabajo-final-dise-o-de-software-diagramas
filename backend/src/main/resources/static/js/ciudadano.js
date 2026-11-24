/*
 * ciudadano.js — lógica de la página del ciudadano (ciudadano.html).
 * Usa las funciones comunes de api.js y mapas.js.
 */

let mapaReporte, marcadorReporte, mapaPuntos, capaPuntos, mapaRutas, capaRutas;

// ------------------------------------------------------------------ Nuevo reporte (HU-03, HU-04)

/** Pone el marcador del reporte en el mapa y copia las coordenadas en el formulario. */
function marcarUbicacion(latitud, longitud) {
    document.getElementById('latitud').value = latitud.toFixed(6);
    document.getElementById('longitud').value = longitud.toFixed(6);
    if (marcadorReporte) {
        marcadorReporte.setLatLng([latitud, longitud]);
    } else {
        marcadorReporte = L.marker([latitud, longitud]).addTo(mapaReporte);
    }
    mapaReporte.setView([latitud, longitud], Math.max(mapaReporte.getZoom(), 16));
}

/** Pide la ubicación al GPS del celular o del navegador (el usuario debe dar permiso). */
function usarGps() {
    if (!navigator.geolocation) {
        mensaje('msgReporte', 'Este navegador no permite obtener la ubicación; toque el punto en el mapa', true);
        return;
    }
    navigator.geolocation.getCurrentPosition(
        posicion => marcarUbicacion(posicion.coords.latitude, posicion.coords.longitude),
        () => mensaje('msgReporte', 'No se pudo obtener la ubicación; toque el punto en el mapa', true));
}

document.getElementById('formReporte').addEventListener('submit', async (evento) => {
    evento.preventDefault();
    // El backend espera una petición multipart con dos partes: "datos" (JSON) y "fotos" (archivos)
    const datos = {
        descripcion: document.getElementById('descripcion').value,
        tipoResiduo: document.getElementById('tipoResiduo').value,
        latitud: parseFloat(document.getElementById('latitud').value),
        longitud: parseFloat(document.getElementById('longitud').value)
    };
    const formulario = new FormData();
    formulario.append('datos', new Blob([JSON.stringify(datos)], { type: 'application/json' }));
    for (const foto of document.getElementById('fotos').files) {
        formulario.append('fotos', foto);
    }
    const creado = await conMensaje('msgReporte', () => Api.post('/api/reportes', formulario));
    if (creado) {
        mensaje('msgReporte', 'Reporte enviado. Número de seguimiento: ' + creado.id + ' · Prioridad sugerida: ' +
            texto(creado.prioridad) + (creado.sector ? ' · Sector: ' + creado.sector : ''), false);
        document.getElementById('formReporte').reset();
        if (marcadorReporte) { mapaReporte.removeLayer(marcadorReporte); marcadorReporte = null; }
        cargarReportes();
    }
});

// ------------------------------------------------------------------ Mis reportes (HU-10)

async function cargarReportes() {
    const estado = document.getElementById('filtroEstado').value;
    const reportes = await Api.get('/api/reportes' + (estado ? '?estado=' + estado : ''));
    const contenedor = document.getElementById('reportes');
    if (reportes.length === 0) {
        contenedor.innerHTML = '<p class="ayuda">No hay reportes con ese filtro.</p>';
        return;
    }
    contenedor.innerHTML = '<div class="tabla"><table><tr><th>N.º</th><th>Fecha</th><th>Descripción</th><th>Tipo</th>' +
        '<th>Estado</th><th>Operador</th><th></th></tr>' +
        reportes.map(r => '<tr><td class="ayuda">' + escapar(r.id.slice(-6)) + '</td><td>' + fecha(r.fechaReporte) +
            '</td><td>' + escapar(r.descripcion) + '</td><td>' + texto(r.tipoResiduo) + '</td><td>' + etiqueta(r.estado) +
            '</td><td>' + escapar(r.operador ? r.operador.nombre : '—') + '</td><td><button class="pequeno" onclick="verReporte(\'' +
            r.id + '\')">Ver</button></td></tr>').join('') + '</table></div>';
}

async function verReporte(id) {
    const r = await Api.get('/api/reportes/' + id);
    document.getElementById('detalleReporte').innerHTML = '<div class="tarjeta"><h3>Reporte ' + escapar(r.id) + '</h3>' +
        '<p>' + escapar(r.descripcion) + '</p>' +
        '<p>Estado: ' + etiqueta(r.estado) + ' · Prioridad: ' + etiqueta(r.prioridad) + ' · Reportado: ' +
        fecha(r.fechaReporte) + '</p>' +
        '<p>Operador: ' + escapar(r.operador ? r.operador.nombre : 'sin asignar') + '</p>' +
        '<p>Dirección: ' + escapar(r.direccion || '—') + ' · Sector: ' + escapar(r.sector || '—') + ' · ' +
        enlaceComoLlegar(r.latitud, r.longitud) + '</p>' +
        (r.reporteOriginalId ? '<p class="ayuda">Vinculado al reporte ' + escapar(r.reporteOriginalId) +
            ' porque describe el mismo problema; el estado mostrado es el de ese reporte.</p>' : '') +
        galeria(r.fotos) +
        (r.descripcionTrabajo ? '<h3>Trabajo realizado (' + fecha(r.fechaResolucion) + ')</h3><p>' +
            escapar(r.descripcionTrabajo) + '</p>' + galeria(r.fotosEvidencia) : '') + '</div>';
}

// ------------------------------------------------------------------ Notificaciones (HU-11)

async function cargarNotificaciones() {
    const notificaciones = await Api.get('/api/ciudadanos/yo/notificaciones');
    document.getElementById('notificaciones').innerHTML = notificaciones.length === 0
        ? '<p class="ayuda">Sin notificaciones.</p>'
        : notificaciones.map(n => '<div class="tarjeta"><b>' + escapar(n.asunto) + '</b><p class="ayuda">' +
            fecha(n.fecha) + (n.enviada ? '' : ' · no se pudo enviar por correo') + '</p></div>').join('');
}

// ------------------------------------------------------------------ Horarios y ruta de mi sector (HU-12)

async function cargarHorarios() {
    const horarios = await conMensaje('msgHorarios', () => Api.get('/api/horarios/mi-sector'));
    document.getElementById('horarios').innerHTML = horarios ? htmlHorarios(horarios) : '';
    dibujarRutas(mapaRutas, capaRutas, horarios ? horarios.rutas : []);
}

// ------------------------------------------------------------------ Puntos de recolección (HU-13)

/** Filtros por material y sector (por defecto, el sector del ciudadano). */
async function cargarPuntos(latitud, longitud) {
    const parametros = new URLSearchParams();
    const material = document.getElementById('filtroMaterial').value;
    const sector = document.getElementById('filtroSector').value;
    if (material) parametros.append('material', material);
    if (sector) parametros.append('sector', sector);
    if (latitud != null) { parametros.append('latitud', latitud); parametros.append('longitud', longitud); }
    const puntos = await Api.get('/api/puntos?' + parametros);
    dibujarPuntos(mapaPuntos, capaPuntos, puntos);
    document.getElementById('puntos').innerHTML = htmlPuntos(puntos);
}

/** Con la ubicación del ciudadano el backend ordena los puntos del más cercano al más lejano. */
function puntosCercanos() {
    if (!navigator.geolocation) return;
    navigator.geolocation.getCurrentPosition(
        p => cargarPuntos(p.coords.latitude, p.coords.longitude),
        () => mensaje('msgPuntos', 'No se pudo obtener su ubicación', true));
}

// ------------------------------------------------------------------ Educación ambiental (HU-14)

function verContenidos() {
    return cargarContenidos('contenidos', document.getElementById('filtroCategoria').value,
        document.getElementById('buscarContenido').value.trim());
}

// ------------------------------------------------------------------ Perfil y contraseña (HU-02)

async function cargarPerfil() {
    const perfil = await Api.get('/api/ciudadanos/yo');
    document.getElementById('perfilDocumento').value = perfil.documento;
    document.getElementById('perfilNombre').value = perfil.nombre;
    document.getElementById('perfilCorreo').value = perfil.correo;
    document.getElementById('perfilSector').value = perfil.sector || '';
    document.getElementById('filtroSector').value = perfil.sector || '';
}

document.getElementById('formPerfil').addEventListener('submit', async (evento) => {
    evento.preventDefault();
    const guardado = await conMensaje('msgPerfil', () => Api.put('/api/ciudadanos/yo', {
        nombre: document.getElementById('perfilNombre').value,
        correo: document.getElementById('perfilCorreo').value,
        sector: document.getElementById('perfilSector').value || null
    }), 'Datos actualizados');
    if (guardado) {
        document.getElementById('nombreUsuario').textContent = guardado.nombre;
        document.getElementById('filtroSector').value = guardado.sector || '';
        cargarHorarios(); // si cambió el sector, cambian los horarios y los puntos
        cargarPuntos();
    }
});

document.getElementById('formContrasena').addEventListener('submit', async (evento) => {
    evento.preventDefault();
    await conMensaje('msgContrasena', () => Api.put('/api/ciudadanos/yo/contrasena', {
        actual: document.getElementById('contrasenaActual').value,
        nueva: document.getElementById('contrasenaNueva').value
    }), 'Contraseña cambiada');
    document.getElementById('formContrasena').reset();
});

// ------------------------------------------------------------------ Al cargar la página
(async function iniciar() {
    if (!await exigirRol('CIUDADANO')) return;
    // Mapa del reporte: un clic en el mapa marca la ubicación (HU-04, criterio 2)
    mapaReporte = crearMapa('mapaReporte');
    mapaReporte.on('click', e => marcarUbicacion(e.latlng.lat, e.latlng.lng));
    mapaPuntos = crearMapa('mapaPuntos');
    capaPuntos = L.featureGroup().addTo(mapaPuntos);
    mapaRutas = crearMapa('mapaRutas');
    capaRutas = L.featureGroup().addTo(mapaRutas);

    await Promise.all([cargarSectores('perfilSector', 'Sin sector'), cargarSectores('filtroSector', 'Todos los sectores')]);
    await cargarPerfil();
    await Promise.all([cargarReportes(), cargarNotificaciones(), cargarHorarios(), cargarPuntos(), verContenidos()]);
})();
