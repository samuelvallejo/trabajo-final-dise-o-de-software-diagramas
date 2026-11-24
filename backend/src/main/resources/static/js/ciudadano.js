/*
 * ciudadano.js — página del ciudadano (ciudadano.html).
 * Clase PaginaCiudadano, subclase de Pagina (api.js); usa la clase Mapa (mapas.js).
 */
class PaginaCiudadano extends Pagina {

    constructor() {
        super('CIUDADANO');
    }

    async cargar() {
        this.mapaReporte = new Mapa('mapaReporte');
        this.mapaReporte.alHacerClic((lat, lng) => this.marcarUbicacion(lat, lng)); // HU-04, criterio 2
        this.mapaPuntos = new Mapa('mapaPuntos');
        this.mapaRutas = new Mapa('mapaRutas');
        this.$('formReporte').addEventListener('submit', e => { e.preventDefault(); this.enviarReporte(); });
        this.$('formPerfil').addEventListener('submit', e => { e.preventDefault(); this.guardarPerfil(); });
        this.$('formContrasena').addEventListener('submit', e => { e.preventDefault(); this.cambiarContrasena(); });

        await this.cargarSectores(['perfilSector'], 'Sin sector');
        await this.cargarSectores(['filtroSector'], 'Todos los sectores');
        await this.cargarPerfil();
        await Promise.all([this.cargarReportes(), this.cargarNotificaciones(), this.cargarHorarios(),
            this.cargarPuntos(), this.verContenidos()]);
    }

    // ------------------------------------------------------------------ HU-03 y HU-04: nuevo reporte

    /** Pone el marcador del reporte en el mapa y copia las coordenadas en el formulario. */
    marcarUbicacion(latitud, longitud) {
        this.$('latitud').value = latitud.toFixed(6);
        this.$('longitud').value = longitud.toFixed(6);
        this.mapaReporte.marcar(latitud, longitud);
    }

    async usarGps() {
        try {
            const coordenadas = await this.obtenerUbicacion();
            this.marcarUbicacion(coordenadas.latitude, coordenadas.longitude);
        } catch (e) {
            this.mensaje('msgReporte', e.message + '; toque el punto en el mapa', true);
        }
    }

    /** El backend espera una petición multipart con dos partes: "datos" (JSON) y "fotos" (archivos). */
    async enviarReporte() {
        const datos = {
            descripcion: this.$('descripcion').value,
            tipoResiduo: this.$('tipoResiduo').value,
            latitud: parseFloat(this.$('latitud').value),
            longitud: parseFloat(this.$('longitud').value)
        };
        const formulario = new FormData();
        formulario.append('datos', new Blob([JSON.stringify(datos)], { type: 'application/json' }));
        for (const foto of this.$('fotos').files) formulario.append('fotos', foto);

        const creado = await this.conMensaje('msgReporte', () => api.post('/api/reportes', formulario));
        if (creado) {
            this.mensaje('msgReporte', 'Reporte enviado. Número de seguimiento: ' + creado.id + ' · Prioridad sugerida: ' +
                Formato.texto(creado.prioridad) + (creado.sector ? ' · Sector: ' + creado.sector : ''), false);
            this.$('formReporte').reset();
            this.mapaReporte.quitarMarca();
            this.cargarReportes();
        }
    }

    // ------------------------------------------------------------------ HU-10: mis reportes

    async cargarReportes() {
        const estado = this.$('filtroEstado').value;
        const reportes = await api.get('/api/reportes' + (estado ? '?estado=' + estado : ''));
        const e = Formato.escapar;
        this.$('reportes').innerHTML = reportes.length === 0 ? '<p class="ayuda">No hay reportes con ese filtro.</p>'
            : '<div class="tabla"><table><tr><th>N.º</th><th>Fecha</th><th>Descripción</th><th>Tipo</th>' +
            '<th>Estado</th><th>Operador</th><th></th></tr>' +
            reportes.map(r => '<tr><td class="ayuda">' + e(r.id.slice(-6)) + '</td><td>' + Formato.fecha(r.fechaReporte) +
                '</td><td>' + e(r.descripcion) + '</td><td>' + Formato.texto(r.tipoResiduo) + '</td><td>' +
                Formato.etiqueta(r.estado) + '</td><td>' + e(r.operador ? r.operador.nombre : '—') +
                '</td><td><button class="pequeno" onclick="pagina.verReporte(\'' + r.id + '\')">Ver</button></td></tr>').join('') +
            '</table></div>';
    }

    async verReporte(id) {
        const r = await api.get('/api/reportes/' + id);
        const e = Formato.escapar;
        this.$('detalleReporte').innerHTML = '<div class="tarjeta"><h3>Reporte ' + e(r.id) + '</h3>' +
            '<p>' + e(r.descripcion) + '</p>' +
            '<p>Estado: ' + Formato.etiqueta(r.estado) + ' · Prioridad: ' + Formato.etiqueta(r.prioridad) +
            ' · Reportado: ' + Formato.fecha(r.fechaReporte) + '</p>' +
            '<p>Operador: ' + e(r.operador ? r.operador.nombre : 'sin asignar') + '</p>' +
            '<p>Dirección: ' + e(r.direccion || '—') + ' · Sector: ' + e(r.sector || '—') + ' · ' +
            Formato.comoLlegar(r.latitud, r.longitud) + '</p>' +
            (r.reporteOriginalId ? '<p class="ayuda">Vinculado al reporte ' + e(r.reporteOriginalId) +
                ' porque describe el mismo problema; el estado mostrado es el de ese reporte.</p>' : '') +
            Formato.galeria(r.fotos) +
            (r.descripcionTrabajo ? '<h3>Trabajo realizado (' + Formato.fecha(r.fechaResolucion) + ')</h3><p>' +
                e(r.descripcionTrabajo) + '</p>' + Formato.galeria(r.fotosEvidencia) : '') + '</div>';
    }

    // ------------------------------------------------------------------ HU-11: notificaciones

    async cargarNotificaciones() {
        const notificaciones = await api.get('/api/ciudadanos/yo/notificaciones');
        this.$('notificaciones').innerHTML = notificaciones.length === 0 ? '<p class="ayuda">Sin notificaciones.</p>'
            : notificaciones.map(n => '<div class="tarjeta"><b>' + Formato.escapar(n.asunto) + '</b><p class="ayuda">' +
                Formato.fecha(n.fecha) + (n.enviada ? '' : ' · no se pudo enviar por correo') + '</p></div>').join('');
    }

    // ------------------------------------------------------------------ HU-12: horarios y ruta de mi sector

    async cargarHorarios() {
        const horarios = await this.conMensaje('msgHorarios', () => api.get('/api/horarios/mi-sector'));
        this.$('horarios').innerHTML = horarios ? Plantillas.horarios(horarios) : '';
        this.mapaRutas.mostrarRutas(horarios ? horarios.rutas : []);
    }

    // ------------------------------------------------------------------ HU-13: puntos de recolección

    /** Filtros por material y sector (por defecto, el sector del ciudadano); con coordenadas, por cercanía. */
    async cargarPuntos(latitud, longitud) {
        const parametros = new URLSearchParams();
        if (this.$('filtroMaterial').value) parametros.append('material', this.$('filtroMaterial').value);
        if (this.$('filtroSector').value) parametros.append('sector', this.$('filtroSector').value);
        if (latitud != null) { parametros.append('latitud', latitud); parametros.append('longitud', longitud); }
        const puntos = await api.get('/api/puntos?' + parametros);
        this.mapaPuntos.mostrarPuntos(puntos);
        this.$('puntos').innerHTML = Plantillas.puntos(puntos);
    }

    async puntosCercanos() {
        try {
            const coordenadas = await this.obtenerUbicacion();
            await this.cargarPuntos(coordenadas.latitude, coordenadas.longitude);
        } catch (e) {
            this.mensaje('msgPuntos', e.message, true);
        }
    }

    // ------------------------------------------------------------------ HU-14: educación ambiental

    async verContenidos() {
        const contenidos = await api.contenidos(this.$('filtroCategoria').value, this.$('buscarContenido').value.trim());
        this.$('contenidos').innerHTML = Plantillas.contenidos(contenidos);
    }

    // ------------------------------------------------------------------ HU-02: perfil y contraseña

    async cargarPerfil() {
        const perfil = await api.get('/api/ciudadanos/yo');
        this.$('perfilDocumento').value = perfil.documento;
        this.$('perfilNombre').value = perfil.nombre;
        this.$('perfilCorreo').value = perfil.correo;
        this.$('perfilSector').value = perfil.sector || '';
        this.$('filtroSector').value = perfil.sector || '';
    }

    async guardarPerfil() {
        const guardado = await this.conMensaje('msgPerfil', () => api.put('/api/ciudadanos/yo', {
            nombre: this.$('perfilNombre').value,
            correo: this.$('perfilCorreo').value,
            sector: this.$('perfilSector').value || null
        }), 'Datos actualizados');
        if (guardado) {
            this.$('nombreUsuario').textContent = guardado.nombre;
            this.$('filtroSector').value = guardado.sector || '';
            this.cargarHorarios(); // si cambió el sector, cambian los horarios y los puntos
            this.cargarPuntos();
        }
    }

    async cambiarContrasena() {
        await this.conMensaje('msgContrasena', () => api.put('/api/ciudadanos/yo/contrasena', {
            actual: this.$('contrasenaActual').value,
            nueva: this.$('contrasenaNueva').value
        }), 'Contraseña cambiada');
        this.$('formContrasena').reset();
    }
}

const pagina = new PaginaCiudadano();
pagina.iniciar();
