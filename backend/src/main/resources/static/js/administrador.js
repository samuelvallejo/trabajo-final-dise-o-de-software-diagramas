/*
 * administrador.js — página del administrador (administrador.html).
 * Clase PaginaAdministrador, subclase de Pagina (api.js); usa la clase Mapa (mapas.js).
 */
class PaginaAdministrador extends Pagina {

    static DIAS = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];

    constructor() {
        super('ADMINISTRADOR');
        this.reporteActual = null;     // id del reporte que se revisa
        this.rutaEditada = null;       // ruta, punto y contenido en edición (null = se crea uno nuevo)
        this.puntoEditado = null;
        this.contenidoEditado = null;
        this.rutas = [];
        this.puntos = [];
        this.contenidos = [];
    }

    async cargar() {
        this.mapaCalor = new Mapa('mapaCalor', 12);
        this.mapaPunto = new Mapa('mapaPunto');
        this.mapaPunto.alHacerClic((lat, lng) => this.ubicarPunto(lat, lng));
        this.$('formOperador').addEventListener('submit', e => { e.preventDefault(); this.crearOperador(); });
        this.$('formRuta').addEventListener('submit', e => { e.preventDefault(); this.guardarRuta(); });
        this.$('formPunto').addEventListener('submit', e => { e.preventDefault(); this.guardarPunto(); });
        this.$('formContenido').addEventListener('submit', e => { e.preventDefault(); this.guardarContenido(); });

        // Informe: por defecto el mes actual
        const hoy = new Date();
        this.$('desde').value = new Date(hoy.getFullYear(), hoy.getMonth(), 1).toISOString().substring(0, 10);
        this.$('hasta').value = hoy.toISOString().substring(0, 10);
        await this.cargarSectores(['fSector', 'iSector'], 'Todos');
        await this.cargarSectores(['rSectores'], null);
        this.agregarFilaHorario();
        await Promise.all([this.cargarPanel(), this.cargarBandeja(), this.cargarOperadores(), this.cargarRutas(),
            this.cargarPuntos(), this.cargarContenidos()]);
    }

    // ------------------------------------------------------------------ HU-15: panel y mapa de calor

    async cargarPanel() {
        const p = await api.get('/api/admin/estadisticas/panel');
        const indicador = (nombre, valor) => '<div class="indicador"><b>' + valor + '</b>' + nombre + '</div>';
        this.$('panel').innerHTML = indicador('Total', p.totalReportes) + indicador('Pendientes', p.pendientes) +
            indicador('Asignados', p.asignados) + indicador('En proceso', p.enProceso) + indicador('Resueltos', p.resueltos);
        this.$('frecuentes').textContent = Object.entries(p.problemasFrecuentes)
            .filter(([, n]) => n > 0).map(([tipo, n]) => Formato.texto(tipo) + ': ' + n).join(' · ') || 'Sin reportes';
        const sectores = p.sectoresMayorAcumulacion;
        this.$('sectores').innerHTML = sectores.length === 0 ? '<p class="ayuda">Sin reportes</p>'
            : '<ol>' + sectores.map(s => '<li>' + Formato.escapar(s.sector) + ': <b>' + s.reportes + '</b></li>').join('') + '</ol>';
        this.mapaCalor.mostrarCalor(sectores);
    }

    // ------------------------------------------------------------------ HU-05: bandeja de reportes

    async cargarBandeja() {
        const filtros = new URLSearchParams(); // solo se envían los filtros con valor
        for (const [parametro, id] of [['estado', 'fEstado'], ['tipoResiduo', 'fTipo'], ['prioridad', 'fPrioridad'], ['sector', 'fSector']]) {
            if (this.$(id).value) filtros.append(parametro, this.$(id).value);
        }
        const reportes = await api.get('/api/reportes?' + filtros);
        const e = Formato.escapar;
        this.$('bandeja').innerHTML = reportes.length === 0 ? '<p class="ayuda">No hay reportes con esos filtros.</p>'
            : '<div class="tabla"><table><tr><th>Fecha</th><th>Descripción</th><th>Tipo</th><th>Prioridad</th>' +
            '<th>Sector</th><th>Estado</th><th>Operador</th><th></th></tr>' +
            reportes.map(r => '<tr' + (r.id === this.reporteActual ? ' class="seleccionado"' : '') + '><td>' +
                Formato.fecha(r.fechaReporte) + '</td><td>' + e(r.descripcion) + '</td><td>' + Formato.texto(r.tipoResiduo) +
                '</td><td>' + Formato.etiqueta(r.prioridad) + '</td><td>' + e(r.sector || '—') + '</td><td>' +
                Formato.etiqueta(r.estado) + '</td><td>' + e(r.operador ? r.operador.nombre : '—') +
                '</td><td><button class="pequeno" onclick="pagina.seleccionar(\'' + r.id + '\')">Revisar</button></td></tr>').join('') +
            '</table></div>';
    }

    /** Muestra el detalle de un reporte con sus acciones: clasificar, asignar y duplicados. */
    async seleccionar(id) {
        this.reporteActual = id;
        const r = await api.get('/api/reportes/' + id);
        const e = Formato.escapar;
        this.$('seccionDetalle').classList.remove('oculto');
        this.$('msgDetalle').textContent = '';
        this.$('detalle').innerHTML = '<p><b>' + e(r.descripcion) + '</b></p>' +
            '<p>Ciudadano: ' + e(r.ciudadano ? r.ciudadano.nombre : '—') + ' · ' + Formato.fecha(r.fechaReporte) +
            ' · Estado: ' + Formato.etiqueta(r.estado) + ' · Prioridad: ' + Formato.etiqueta(r.prioridad) + '</p>' +
            '<p>' + e(r.direccion || '') + ' · Sector: ' + e(r.sector || '—') + ' · ' + Formato.comoLlegar(r.latitud, r.longitud) + '</p>' +
            (r.reporteOriginalId ? '<p>Es duplicado del reporte ' + e(r.reporteOriginalId) +
                ' <button class="pequeno secundario" onclick="pagina.desvincular()">Desvincular</button></p>' : '') +
            Formato.galeria(r.fotos);
        this.$('claseTipo').value = r.tipoResiduo;
        this.$('clasePrioridad').value = r.prioridad;
        await Promise.all([this.cargarOperadoresParaAsignar(), this.cargarDuplicados()]);
        this.$('seccionDetalle').scrollIntoView({ behavior: 'smooth' });
        this.cargarBandeja();
    }

    /** Recarga lo que cambia después de una acción sobre el reporte. */
    async refrescar() {
        await Promise.all([this.cargarBandeja(), this.cargarPanel(), this.cargarOperadores()]);
    }

    async clasificar() {
        const ok = await this.conMensaje('msgDetalle', () => api.put('/api/reportes/' + this.reporteActual + '/clasificacion', {
            tipoResiduo: this.$('claseTipo').value, prioridad: this.$('clasePrioridad').value
        }), 'Clasificación guardada');
        if (ok) this.refrescar();
    }

    // ------------------------------------------------------------------ HU-07: asignación

    async cargarOperadoresParaAsignar() {
        const operadores = await api.get('/api/admin/operadores?disponibles=true');
        this.$('operadorAsignar').innerHTML = operadores.length === 0 ? '<option value="">No hay operadores disponibles</option>'
            : operadores.map(o => '<option value="' + o.id + '">' + Formato.escapar(o.nombre) + ' (' +
                o.reportesSinResolver + ' sin resolver)</option>').join('');
    }

    async asignar() {
        const ok = await this.conMensaje('msgDetalle', () => api.put('/api/reportes/' + this.reporteActual + '/asignacion', {
            operadorId: this.$('operadorAsignar').value, reasignar: this.$('reasignar').checked
        }), 'Reporte asignado; se notificó al ciudadano');
        if (ok) this.refrescar();
    }

    // ------------------------------------------------------------------ HU-06: duplicados

    async cargarDuplicados() {
        const posibles = await api.get('/api/reportes/' + this.reporteActual + '/posibles-duplicados');
        this.$('duplicados').innerHTML = posibles.length === 0 ? '<p class="ayuda">No hay reportes abiertos del mismo tipo cerca.</p>'
            : posibles.map(p => '<div class="tarjeta">' + Formato.escapar(p.reporte.descripcion) + ' · ' +
                Formato.etiqueta(p.reporte.estado) + ' · a ' + p.distanciaMetros + ' m <button class="pequeno" ' +
                'onclick="pagina.marcarDuplicado(\'' + p.reporte.id + '\')">Marcar este reporte como duplicado de ese</button></div>').join('');
    }

    async marcarDuplicado(originalId) {
        const ok = await this.conMensaje('msgDetalle', () => api.put('/api/reportes/' + this.reporteActual + '/duplicado',
            { reporteOriginalId: originalId }), 'Marcado como duplicado');
        if (ok) { await this.seleccionar(this.reporteActual); this.refrescar(); }
    }

    async desvincular() {
        const ok = await this.conMensaje('msgDetalle', () => api.borrar('/api/reportes/' + this.reporteActual + '/duplicado'),
            'Reporte desvinculado');
        if (ok) { await this.seleccionar(this.reporteActual); this.refrescar(); }
    }

    // ------------------------------------------------------------------ HU-07: operadores

    async cargarOperadores() {
        const operadores = await api.get('/api/admin/operadores?disponibles=false');
        const e = Formato.escapar;
        this.$('operadores').innerHTML = operadores.length === 0 ? '<p class="ayuda">Aún no hay operadores.</p>'
            : '<div class="tabla"><table><tr><th>Nombre</th><th>Sin resolver</th><th>Estado</th><th></th></tr>' +
            operadores.map(o => '<tr><td>' + e(o.nombre) + '<br><span class="ayuda">' + e(o.correo) + '</span></td><td>' +
                o.reportesSinResolver + '</td><td>' + (o.disponible ? 'Disponible' : 'Ocupado') +
                '</td><td><button class="pequeno secundario" onclick="pagina.cambiarDisponibilidad(\'' + o.id + '\', ' +
                !o.disponible + ')">' + (o.disponible ? 'Marcar ocupado' : 'Marcar disponible') + '</button></td></tr>').join('') +
            '</table></div>';
    }

    async cambiarDisponibilidad(id, disponible) {
        await api.put('/api/admin/operadores/' + id + '/disponibilidad', { disponible });
        this.cargarOperadores();
    }

    async crearOperador() {
        const ok = await this.conMensaje('msgOperador', () => api.post('/api/admin/operadores', {
            nombre: this.$('opNombre').value, documento: this.$('opDocumento').value,
            correo: this.$('opCorreo').value, contrasena: this.$('opContrasena').value
        }), 'Operador creado');
        if (ok) { this.$('formOperador').reset(); this.cargarOperadores(); }
    }

    // ------------------------------------------------------------------ HU-15: informe

    parametrosInforme() {
        const p = new URLSearchParams({ desde: this.$('desde').value, hasta: this.$('hasta').value });
        if (this.$('iSector').value) p.append('sector', this.$('iSector').value);
        return p;
    }

    async generarInforme() {
        const i = await this.conMensaje('msgInforme', () => api.get('/api/admin/estadisticas/informe?' + this.parametrosInforme()), ' ');
        if (!i) return;
        this.$('informe').innerHTML = '<p>Total: <b>' + i.totalReportes + '</b> · Pendientes: ' + i.pendientes +
            ' · Resueltos: ' + i.resueltos + '</p><p>Tiempo promedio de atención: ' +
            (i.tiempoPromedioAtencionHoras === null ? '—' : i.tiempoPromedioAtencionHoras + ' horas') + '</p>';
    }

    /** El navegador descarga el archivo directamente desde la API. */
    descargarCsv() {
        location.href = '/api/admin/estadisticas/informe.csv?' + this.parametrosInforme();
    }

    // ------------------------------------------------------------------ HU-12: rutas y horarios

    async cargarRutas() {
        this.rutas = await api.get('/api/admin/rutas');
        this.$('rutas').innerHTML = this.rutas.map(r => '<div class="tarjeta"><b>' + Formato.escapar(r.nombre) +
            '</b> <span class="ayuda">' + Formato.escapar(r.sectores.join(', ')) + '</span><p>' +
            r.horarios.map(Formato.horario).join(' · ') + '</p><button class="pequeno secundario" ' +
            'onclick="pagina.editarRuta(\'' + r.id + '\')">Editar</button></div>').join('') || '<p class="ayuda">Aún no hay rutas.</p>';
    }

    /** Agrega una fila de horario (día, hora de inicio y hora de fin) al formulario. */
    agregarFilaHorario(dia, inicio, fin) {
        const fila = document.createElement('div');
        fila.className = 'fila filaHorario';
        fila.innerHTML = '<select class="hDia">' + PaginaAdministrador.DIAS.map(d => '<option value="' + d + '">' +
            Formato.DIAS[d] + '</option>').join('') + '</select><input class="hInicio" type="time" required>' +
            '<input class="hFin" type="time" required><button type="button" class="pequeno secundario" style="flex:0">Quitar</button>';
        fila.querySelector('.hDia').value = dia || 'MONDAY';
        fila.querySelector('.hInicio').value = (inicio || '06:00').substring(0, 5);
        fila.querySelector('.hFin').value = (fin || '12:00').substring(0, 5);
        fila.querySelector('button').onclick = () => fila.remove();
        this.$('filasHorario').appendChild(fila);
    }

    editarRuta(id) {
        const r = this.rutas.find(x => x.id === id);
        this.rutaEditada = r;
        this.$('tituloRuta').textContent = 'Editar ruta';
        this.$('rNombre').value = r.nombre;
        [...this.$('rSectores').options].forEach(o => o.selected = r.sectores.includes(o.value));
        this.$('filasHorario').innerHTML = '';
        r.horarios.forEach(h => this.agregarFilaHorario(h.diaSemana, h.horaInicio, h.horaFin));
        this.$('formRuta').scrollIntoView({ behavior: 'smooth' });
    }

    limpiarRuta() {
        this.rutaEditada = null;
        this.$('tituloRuta').textContent = 'Nueva ruta';
        this.$('formRuta').reset();
        this.$('filasHorario').innerHTML = '';
        this.agregarFilaHorario();
    }

    async guardarRuta() {
        const datos = {
            nombre: this.$('rNombre').value,
            sectores: [...this.$('rSectores').selectedOptions].map(o => o.value),
            horarios: [...document.querySelectorAll('.filaHorario')].map(f => ({
                diaSemana: f.querySelector('.hDia').value,
                horaInicio: f.querySelector('.hInicio').value,
                horaFin: f.querySelector('.hFin').value
            })),
            recorrido: this.rutaEditada ? this.rutaEditada.recorrido : []
        };
        const ok = await this.conMensaje('msgRuta', () => this.rutaEditada
            ? api.put('/api/admin/rutas/' + this.rutaEditada.id, datos)
            : api.post('/api/admin/rutas', datos), 'Ruta guardada; los ciudadanos ya ven el cambio');
        if (ok) { this.limpiarRuta(); this.cargarRutas(); }
    }

    // ------------------------------------------------------------------ HU-13: puntos de recolección

    async cargarPuntos() {
        this.puntos = await api.get('/api/admin/puntos');
        const e = Formato.escapar;
        this.$('puntosAdmin').innerHTML = this.puntos.map(p => '<div class="tarjeta"><b>' + e(p.nombre) +
            '</b> <span class="ayuda">' + e(p.sector || '—') + ' · ' + e(p.materiales.join(', ')) + ' · ' +
            (p.activo ? 'Activo' : 'Inactivo') + '</span><br>' +
            '<button class="pequeno secundario" onclick="pagina.editarPunto(\'' + p.id + '\')">Editar</button>' +
            '<button class="pequeno secundario" onclick="pagina.cambiarActivacion(\'' + p.id + '\', ' + !p.activo + ')">' +
            (p.activo ? 'Desactivar' : 'Activar') + '</button></div>').join('');
    }

    async cambiarActivacion(id, activar) {
        await (activar ? api.post('/api/admin/puntos/' + id + '/activacion') : api.borrar('/api/admin/puntos/' + id));
        this.cargarPuntos();
    }

    ubicarPunto(latitud, longitud) {
        this.$('pLatitud').value = latitud.toFixed(6);
        this.$('pLongitud').value = longitud.toFixed(6);
        this.mapaPunto.marcar(latitud, longitud, this.mapaPunto.mapa.getZoom());
    }

    editarPunto(id) {
        const p = this.puntos.find(x => x.id === id);
        this.puntoEditado = id;
        this.$('tituloPunto').textContent = 'Editar punto';
        this.$('pNombre').value = p.nombre;
        this.$('pDireccion').value = p.direccion;
        this.$('pHorario').value = p.horarioAtencion;
        [...this.$('pMateriales').options].forEach(o => o.selected = p.materiales.includes(o.value));
        this.$('pLatitud').value = p.latitud;
        this.$('pLongitud').value = p.longitud;
        this.mapaPunto.marcar(p.latitud, p.longitud, 15);
        this.$('formPunto').scrollIntoView({ behavior: 'smooth' });
    }

    limpiarPunto() {
        this.puntoEditado = null;
        this.$('tituloPunto').textContent = 'Nuevo punto';
        this.$('formPunto').reset();
        this.mapaPunto.quitarMarca();
    }

    async guardarPunto() {
        const datos = {
            nombre: this.$('pNombre').value,
            direccion: this.$('pDireccion').value,
            latitud: parseFloat(this.$('pLatitud').value),
            longitud: parseFloat(this.$('pLongitud').value),
            materiales: [...this.$('pMateriales').selectedOptions].map(o => o.value),
            horarioAtencion: this.$('pHorario').value
        };
        const ok = await this.conMensaje('msgPunto', () => this.puntoEditado
            ? api.put('/api/admin/puntos/' + this.puntoEditado, datos)
            : api.post('/api/admin/puntos', datos), 'Punto guardado (el sector se identificó por la ubicación)');
        if (ok) { this.limpiarPunto(); this.cargarPuntos(); }
    }

    // ------------------------------------------------------------------ HU-14: contenido educativo

    async cargarContenidos() {
        this.contenidos = await api.get('/api/admin/contenidos');
        this.$('contenidosAdmin').innerHTML = this.contenidos.map(c => '<div class="tarjeta"><b>' + Formato.escapar(c.titulo) +
            '</b> <span class="ayuda">' + Formato.escapar(Formato.texto(c.categoria)) + ' · ' +
            (c.publicado ? 'Publicado' : 'Borrador') + '</span><br>' +
            '<button class="pequeno secundario" onclick="pagina.editarContenido(\'' + c.id + '\')">Editar</button>' +
            '<button class="pequeno secundario" onclick="pagina.cambiarPublicacion(\'' + c.id + '\', ' + !c.publicado + ')">' +
            (c.publicado ? 'Retirar' : 'Publicar') + '</button></div>').join('') || '<p class="ayuda">Sin contenido.</p>';
    }

    async cambiarPublicacion(id, publicar) {
        const ruta = '/api/admin/contenidos/' + id + '/publicacion';
        await (publicar ? api.post(ruta) : api.borrar(ruta));
        this.cargarContenidos();
    }

    editarContenido(id) {
        const c = this.contenidos.find(x => x.id === id);
        this.contenidoEditado = id;
        this.$('tituloContenido').textContent = 'Editar contenido';
        this.$('cTitulo').value = c.titulo;
        this.$('cCategoria').value = c.categoria;
        this.$('cCuerpo').value = c.cuerpo;
        this.$('cImagen').value = c.imagenUrl || '';
        this.$('cPublicar').checked = c.publicado;
        this.$('formContenido').scrollIntoView({ behavior: 'smooth' });
    }

    limpiarContenido() {
        this.contenidoEditado = null;
        this.$('tituloContenido').textContent = 'Nuevo contenido';
        this.$('formContenido').reset();
    }

    async guardarContenido() {
        const datos = {
            titulo: this.$('cTitulo').value,
            categoria: this.$('cCategoria').value,
            cuerpo: this.$('cCuerpo').value,
            imagenUrl: this.$('cImagen').value || null,
            publicar: this.$('cPublicar').checked
        };
        const ok = await this.conMensaje('msgContenido', () => this.contenidoEditado
            ? api.put('/api/admin/contenidos/' + this.contenidoEditado, datos)
            : api.post('/api/admin/contenidos', datos), 'Contenido guardado');
        if (ok) { this.limpiarContenido(); this.cargarContenidos(); }
    }
}

const pagina = new PaginaAdministrador();
pagina.iniciar();
