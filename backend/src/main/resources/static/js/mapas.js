/*
 * mapas.js — funciones comunes para los mapas del frontend.
 *
 * Usa Leaflet (librería JavaScript de mapas, cargada en fragmentos.html) con las teselas de
 * OpenStreetMap, que no necesitan clave. Es el "Servicio de mapas/GPS" de los diagramas del lado del
 * navegador; la dirección de cada reporte la calcula el backend con Nominatim (OpenStreetMap).
 */

/** Centro de Pasto (plaza de Nariño) y límites aproximados del municipio. */
const PASTO = [1.2136, -77.2811];

/** Colores por prioridad (los mismos de las etiquetas de la interfaz). */
const COLOR_PRIORIDAD = { ALTA: '#b3261e', MEDIA: '#e0a800', BAJA: '#6b7280' };

/** Crea un mapa centrado en Pasto dentro del elemento con ese id. */
function crearMapa(idElemento, zoom) {
    const mapa = L.map(idElemento).setView(PASTO, zoom || 13);
    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution: '&copy; colaboradores de OpenStreetMap'
    }).addTo(mapa);
    return mapa;
}

/** Marcador circular de color (por ejemplo, según la prioridad del reporte). */
function marcadorCirculo(latitud, longitud, color, radio) {
    return L.circleMarker([latitud, longitud], {
        radius: radio || 9, color: color, weight: 2, fillColor: color, fillOpacity: 0.55
    });
}

/** Ajusta el mapa para que se vean todos los marcadores de una capa (si hay alguno). */
function encuadrar(mapa, capa) {
    const limites = capa.getBounds();
    if (limites.isValid()) mapa.fitBounds(limites.pad(0.3), { maxZoom: 15 });
}

/** Enlace "Cómo llegar" con la ruta de OpenStreetMap hasta el punto (se abre en otra pestaña). */
function enlaceComoLlegar(latitud, longitud) {
    return '<a href="https://www.openstreetmap.org/directions?route=%3B' + latitud + '%2C' + longitud +
        '#map=17/' + latitud + '/' + longitud + '" target="_blank">Cómo llegar</a>';
}

/** Dibuja en el mapa los puntos de recolección con su información en una ventana emergente (HU-13). */
function dibujarPuntos(mapa, capa, puntos) {
    capa.clearLayers();
    puntos.forEach(p => L.marker([p.latitud, p.longitud]).addTo(capa).bindPopup(
        '<b>' + escapar(p.nombre) + '</b><br>' + escapar(p.direccion) + '<br>Materiales: ' +
        escapar(p.materiales.join(', ')) + '<br>Horario: ' + escapar(p.horarioAtencion) +
        (p.distanciaMetros != null ? '<br>A ' + Math.round(p.distanciaMetros) + ' m' : '')));
    encuadrar(mapa, capa);
}

/** Dibuja el recorrido de las rutas de un sector como líneas (HU-12). */
function dibujarRutas(mapa, capa, rutas) {
    capa.clearLayers();
    rutas.filter(r => r.recorrido && r.recorrido.length > 1).forEach(r =>
        L.polyline(r.recorrido.map(c => [c.latitud, c.longitud]), { color: '#1b5e20', weight: 5 })
            .addTo(capa).bindPopup(escapar(r.nombre)));
    encuadrar(mapa, capa);
}
