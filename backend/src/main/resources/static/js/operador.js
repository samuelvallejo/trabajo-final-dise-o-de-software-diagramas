/*
 * operador.js — lógica de la página del operador de limpieza (operador.html).
 * Usa las funciones comunes de api.js.
 *
 * Cada reporte asignado se muestra como una tarjeta. Según su estado aparece una acción:
 *   ASIGNADO   → botón "Iniciar atención"           (pasa a EN_PROCESO)
 *   EN_PROCESO → formulario "Registrar cierre"      (pasa a RESUELTO con descripción y evidencias)
 */

let mapa, capa;

async function cargarAsignaciones() {
    const orden = document.getElementById('orden').value;
    const reportes = await Api.get('/api/operador/asignaciones?orden=' + orden);
    document.getElementById('asignaciones').innerHTML = reportes.length === 0
        ? '<p class="ayuda">No tiene reportes pendientes de atender.</p>'
        : reportes.map(tarjeta).join('');
    // Vista de mapa: un marcador por reporte con el color de su prioridad (HU-08, criterio 2)
    capa.clearLayers();
    reportes.forEach(r => marcadorCirculo(r.latitud, r.longitud, COLOR_PRIORIDAD[r.prioridad]).addTo(capa)
        .bindPopup('<b>' + escapar(r.descripcion) + '</b><br>' + texto(r.prioridad) + ' · ' + texto(r.estado) +
            '<br>' + enlaceComoLlegar(r.latitud, r.longitud)));
    encuadrar(mapa, capa);
}

/** HTML de un reporte con la acción que corresponde a su estado. */
function tarjeta(r) {
    let accion = '';
    if (r.estado === 'ASIGNADO') {
        accion = '<button onclick="iniciar(\'' + r.id + '\')">Iniciar atención</button>';
    } else if (r.estado === 'EN_PROCESO') {
        accion = '<h3>Registrar cierre</h3>' +
            '<label for="trabajo-' + r.id + '">Trabajo realizado</label>' +
            '<textarea id="trabajo-' + r.id + '"></textarea>' +
            '<label for="evidencias-' + r.id + '">Fotos de evidencia (de 1 a 5, JPG o PNG)</label>' +
            '<input id="evidencias-' + r.id + '" type="file" accept="image/jpeg,image/png" multiple>' +
            '<button onclick="cerrar(\'' + r.id + '\')">Marcar como resuelto</button>';
    }
    return '<div class="tarjeta">' +
        '<p><b>' + escapar(r.descripcion) + '</b></p>' +
        '<p>' + etiqueta(r.prioridad) + ' ' + etiqueta(r.estado) + ' · ' + texto(r.tipoResiduo) + ' · ' +
        fecha(r.fechaReporte) + '</p>' +
        '<p>' + escapar(r.direccion || '') + ' · Sector: ' + escapar(r.sector || '—') + ' · ' +
        enlaceComoLlegar(r.latitud, r.longitud) + '</p>' +
        galeria(r.fotos) + accion + '</div>';
}

/** ASIGNADO → EN_PROCESO; el backend avisa al ciudadano. */
async function iniciar(id) {
    const ok = await conMensaje('msgAsignaciones', () => Api.post('/api/operador/asignaciones/' + id + '/inicio'),
        'Atención iniciada');
    if (ok) cargarAsignaciones();
}

/** EN_PROCESO → RESUELTO con descripción y fotos (petición multipart). */
async function cerrar(id) {
    const formulario = new FormData();
    formulario.append('descripcionTrabajo', document.getElementById('trabajo-' + id).value);
    for (const foto of document.getElementById('evidencias-' + id).files) {
        formulario.append('evidencias', foto);
    }
    const ok = await conMensaje('msgAsignaciones',
        () => Api.post('/api/operador/asignaciones/' + id + '/cierre', formulario),
        'Reporte resuelto; se notificó al ciudadano');
    if (ok) cargarAsignaciones();
}

(async function iniciarPagina() {
    if (!await exigirRol('OPERADOR')) return;
    mapa = crearMapa('mapaAsignaciones');
    capa = L.featureGroup().addTo(mapa);
    await cargarAsignaciones();
})();
