/*
 * operador.js — página del operador de limpieza (operador.html).
 * Clase PaginaOperador, subclase de Pagina (api.js); usa la clase Mapa (mapas.js).
 *
 * Cada reporte asignado se muestra como una tarjeta. Según su estado aparece una acción:
 *   ASIGNADO   → botón "Iniciar atención"           (pasa a EN_PROCESO)
 *   EN_PROCESO → formulario "Registrar cierre"      (pasa a RESUELTO con descripción y evidencias)
 */
class PaginaOperador extends Pagina {

    constructor() {
        super('OPERADOR');
    }

    async cargar() {
        this.mapa = new Mapa('mapaAsignaciones');
        await this.cargarAsignaciones();
    }

    /** Lista y mapa de los reportes asignados, ordenados por prioridad o por fecha (HU-08). */
    async cargarAsignaciones() {
        const reportes = await api.get('/api/operador/asignaciones?orden=' + this.$('orden').value);
        this.$('asignaciones').innerHTML = reportes.length === 0
            ? '<p class="ayuda">No tiene reportes pendientes de atender.</p>'
            : reportes.map(r => this.tarjeta(r)).join('');
        this.mapa.mostrarReportes(reportes); // un marcador por reporte con el color de su prioridad
    }

    /** HTML de un reporte con la acción que corresponde a su estado. */
    tarjeta(r) {
        const e = Formato.escapar;
        let accion = '';
        if (r.estado === 'ASIGNADO') {
            accion = '<button onclick="pagina.iniciarAtencion(\'' + r.id + '\')">Iniciar atención</button>';
        } else if (r.estado === 'EN_PROCESO') {
            accion = '<h3>Registrar cierre</h3>' +
                '<label for="trabajo-' + r.id + '">Trabajo realizado</label>' +
                '<textarea id="trabajo-' + r.id + '"></textarea>' +
                '<label for="evidencias-' + r.id + '">Fotos de evidencia (de 1 a 5, JPG o PNG)</label>' +
                '<input id="evidencias-' + r.id + '" type="file" accept="image/jpeg,image/png" multiple>' +
                '<button onclick="pagina.cerrar(\'' + r.id + '\')">Marcar como resuelto</button>';
        }
        return '<div class="tarjeta">' +
            '<p><b>' + e(r.descripcion) + '</b></p>' +
            '<p>' + Formato.etiqueta(r.prioridad) + ' ' + Formato.etiqueta(r.estado) + ' · ' +
            Formato.texto(r.tipoResiduo) + ' · ' + Formato.fecha(r.fechaReporte) + '</p>' +
            '<p>' + e(r.direccion || '') + ' · Sector: ' + e(r.sector || '—') + ' · ' +
            Formato.comoLlegar(r.latitud, r.longitud) + '</p>' +
            Formato.galeria(r.fotos) + accion + '</div>';
    }

    /** ASIGNADO → EN_PROCESO; el backend avisa al ciudadano (HU-09). */
    async iniciarAtencion(id) {
        const ok = await this.conMensaje('msgAsignaciones', () => api.post('/api/operador/asignaciones/' + id + '/inicio'),
            'Atención iniciada');
        if (ok) this.cargarAsignaciones();
    }

    /** EN_PROCESO → RESUELTO con descripción y fotos (petición multipart, HU-09). */
    async cerrar(id) {
        const formulario = new FormData();
        formulario.append('descripcionTrabajo', this.$('trabajo-' + id).value);
        for (const foto of this.$('evidencias-' + id).files) formulario.append('evidencias', foto);
        const ok = await this.conMensaje('msgAsignaciones',
            () => api.post('/api/operador/asignaciones/' + id + '/cierre', formulario),
            'Reporte resuelto; se notificó al ciudadano');
        if (ok) this.cargarAsignaciones();
    }
}

const pagina = new PaginaOperador();
pagina.iniciar();
