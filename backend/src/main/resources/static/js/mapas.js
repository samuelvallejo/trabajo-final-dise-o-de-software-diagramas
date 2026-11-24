/*
 * mapas.js — clase Mapa: envuelve la librería Leaflet con las teselas de OpenStreetMap (sin clave).
 * Es el "Servicio de mapas/GPS" de los diagramas del lado del navegador; la dirección de cada reporte
 * la calcula el backend con Nominatim (OpenStreetMap).
 *
 * GRASP Variaciones protegidas / Indirección: las páginas solo usan los métodos de Mapa; si se cambia
 * de librería de mapas, solo cambia esta clase.
 */
class Mapa {
    /** Centro de Pasto (plaza de Nariño). */
    static PASTO = [1.2136, -77.2811];

    /** Colores por prioridad (los mismos de las etiquetas de la interfaz). */
    static COLOR_PRIORIDAD = { ALTA: '#b3261e', MEDIA: '#e0a800', BAJA: '#6b7280' };

    /** Crea un mapa centrado en Pasto dentro del elemento con ese id. */
    constructor(idElemento, zoom) {
        this.mapa = L.map(idElemento).setView(Mapa.PASTO, zoom || 13);
        L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
            maxZoom: 19, attribution: '&copy; colaboradores de OpenStreetMap'
        }).addTo(this.mapa);
        this.capa = L.featureGroup().addTo(this.mapa); // capa con los marcadores que se dibujan
        this.marcador = null;                          // marcador único (ubicación elegida)
    }

    /** Llama a la función con la latitud y longitud cada vez que se hace clic en el mapa. */
    alHacerClic(funcion) {
        this.mapa.on('click', e => funcion(e.latlng.lat, e.latlng.lng));
    }

    /** Pone (o mueve) el marcador único y centra el mapa en él. */
    marcar(latitud, longitud, zoom) {
        if (this.marcador) this.marcador.setLatLng([latitud, longitud]);
        else this.marcador = L.marker([latitud, longitud]).addTo(this.mapa);
        this.mapa.setView([latitud, longitud], zoom || Math.max(this.mapa.getZoom(), 16));
    }

    quitarMarca() {
        if (this.marcador) { this.mapa.removeLayer(this.marcador); this.marcador = null; }
    }

    /** Ajusta la vista para que se vean todos los elementos dibujados. */
    encuadrar() {
        const limites = this.capa.getBounds();
        if (limites.isValid()) this.mapa.fitBounds(limites.pad(0.3), { maxZoom: 15 });
    }

    /** Puntos de recolección con su información en una ventana emergente (HU-13). */
    mostrarPuntos(puntos) {
        const e = Formato.escapar;
        this.capa.clearLayers();
        puntos.forEach(p => L.marker([p.latitud, p.longitud]).addTo(this.capa).bindPopup(
            '<b>' + e(p.nombre) + '</b><br>' + e(p.direccion) + '<br>Materiales: ' + e(p.materiales.join(', ')) +
            '<br>Horario: ' + e(p.horarioAtencion) +
            (p.distanciaMetros != null ? '<br>A ' + Math.round(p.distanciaMetros) + ' m' : '')));
        this.encuadrar();
    }

    /** Recorrido de las rutas como líneas (HU-12). */
    mostrarRutas(rutas) {
        this.capa.clearLayers();
        rutas.filter(r => r.recorrido && r.recorrido.length > 1).forEach(r =>
            L.polyline(r.recorrido.map(c => [c.latitud, c.longitud]), { color: '#1b5e20', weight: 5 })
                .addTo(this.capa).bindPopup(Formato.escapar(r.nombre)));
        this.encuadrar();
    }

    /** Reportes como círculos del color de su prioridad (HU-08). */
    mostrarReportes(reportes) {
        this.capa.clearLayers();
        reportes.forEach(r => L.circleMarker([r.latitud, r.longitud], {
            radius: 9, weight: 2, color: Mapa.COLOR_PRIORIDAD[r.prioridad],
            fillColor: Mapa.COLOR_PRIORIDAD[r.prioridad], fillOpacity: 0.55
        }).addTo(this.capa).bindPopup('<b>' + Formato.escapar(r.descripcion) + '</b><br>' + Formato.texto(r.prioridad) +
            ' · ' + Formato.texto(r.estado) + '<br>' + Formato.comoLlegar(r.latitud, r.longitud)));
        this.encuadrar();
    }

    /** Mapa de calor: un círculo por sector, más grande e intenso cuantos más reportes tiene (HU-15). */
    mostrarCalor(sectores) {
        this.capa.clearLayers();
        const maximo = Math.max(1, ...sectores.map(s => s.reportes));
        sectores.filter(s => s.latitudPromedio !== 0).forEach(s => L.circle([s.latitudPromedio, s.longitudPromedio], {
            radius: 150 + 450 * s.reportes / maximo, color: '#b3261e', weight: 1,
            fillColor: '#b3261e', fillOpacity: 0.15 + 0.5 * s.reportes / maximo
        }).addTo(this.capa).bindPopup(Formato.escapar(s.sector) + ': ' + s.reportes + ' reportes'));
        this.encuadrar();
    }
}
