/*
 * api.js — clases comunes a todas las páginas del frontend (programación orientada a objetos).
 *
 *   ClienteApi  habla con el backend (único punto de acceso a la API).
 *   Sesion      controla quién inició sesión y a qué página va cada rol.
 *   Formato     convierte datos en texto y HTML seguro para mostrar.
 *   Plantillas  arma los bloques de HTML que se repiten en varias páginas.
 *   Pagina      clase base de cada página; cada página la extiende y sobrescribe cargar().
 *
 * La sesión se mantiene con la cookie JSESSIONID que crea el backend al iniciar sesión; el navegador la
 * envía sola en cada petición al mismo sitio.
 */

/**
 * Cliente de la API REST del backend.
 * GRASP Indirección: las páginas no usan fetch directamente; si cambia una ruta o el manejo de errores,
 * se cambia solo aquí.
 */
class ClienteApi {
    /**
     * Hace una petición y devuelve el JSON de la respuesta.
     * - Si el cuerpo es un FormData (formularios con fotos) se envía como multipart.
     * - Si es un objeto, se envía como JSON.
     * - Si la API responde con error, lanza un Error con el mensaje "detail" del backend.
     */
    async pedir(metodo, ruta, cuerpo) {
        const opciones = { method: metodo, credentials: 'same-origin', headers: {} };
        if (cuerpo instanceof FormData) {
            opciones.body = cuerpo;
        } else if (cuerpo !== undefined) {
            opciones.headers['Content-Type'] = 'application/json';
            opciones.body = JSON.stringify(cuerpo);
        }
        const respuesta = await fetch(ruta, opciones);
        if (respuesta.status === 204) {
            return null;
        }
        const texto = await respuesta.text();
        const datos = texto ? JSON.parse(texto) : null;
        if (!respuesta.ok) {
            throw new Error((datos && (datos.detail || datos.title)) || 'Error ' + respuesta.status);
        }
        return datos;
    }

    get(ruta) { return this.pedir('GET', ruta); }
    post(ruta, cuerpo) { return this.pedir('POST', ruta, cuerpo); }
    put(ruta, cuerpo) { return this.pedir('PUT', ruta, cuerpo); }
    borrar(ruta) { return this.pedir('DELETE', ruta); }

    /** Lista de sectores con sus barrios (se usa en registro, perfil y filtros). */
    sectores() { return this.get('/api/sectores'); }

    /** Contenidos educativos publicados, con filtros opcionales por categoría y palabra (HU-14). */
    contenidos(categoria, palabra) {
        const parametros = new URLSearchParams();
        if (categoria) parametros.append('categoria', categoria);
        if (palabra) parametros.append('q', palabra);
        return this.get('/api/contenidos?' + parametros);
    }
}

/** Instancia única del cliente que usan todas las páginas. */
const api = new ClienteApi();

/** Sesión del usuario: quién está conectado y a qué página le corresponde ir. */
class Sesion {
    static PAGINAS = { CIUDADANO: '/ciudadano', ADMINISTRADOR: '/administrador', OPERADOR: '/operador' };

    static paginaDeRol(rol) {
        return Sesion.PAGINAS[rol] || '/';
    }

    /** Usuario conectado, o null si no hay sesión. */
    static async usuarioActual() {
        try {
            return await api.get('/api/auth/yo');
        } catch (e) {
            return null;
        }
    }

    static async cerrar() {
        try { await api.post('/api/auth/logout'); } finally { location.href = '/'; }
    }
}

/** Conversión de datos a texto y HTML. GRASP Fabricación pura: agrupa la presentación en un solo lugar. */
class Formato {
    static TEXTO = {
        PENDIENTE: 'Pendiente', ASIGNADO: 'Asignado', EN_PROCESO: 'En proceso', RESUELTO: 'Resuelto',
        ALTA: 'Alta', MEDIA: 'Media', BAJA: 'Baja',
        DOMESTICO: 'Doméstico', ESCOMBROS: 'Escombros', RECICLABLE: 'Reciclable', VOLUMINOSO: 'Voluminoso',
        PELIGROSO: 'Peligroso', OTRO: 'Otro',
        SEPARACION_RESIDUOS: 'Separación de residuos', RECICLAJE: 'Reciclaje',
        REDUCCION_BASURA: 'Reducción de basura', BUENAS_PRACTICAS: 'Buenas prácticas'
    };

    static DIAS = { MONDAY: 'Lunes', TUESDAY: 'Martes', WEDNESDAY: 'Miércoles', THURSDAY: 'Jueves',
        FRIDAY: 'Viernes', SATURDAY: 'Sábado', SUNDAY: 'Domingo' };

    /** Evita que un texto escrito por un usuario se interprete como HTML (protección contra XSS). */
    static escapar(valor) {
        return String(valor ?? '').replace(/[&<>"']/g, c => (
            { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
    }

    /** Convierte "2026-11-20T10:30:00" en "20/11/2026 10:30". */
    static fecha(iso) {
        if (!iso) return '—';
        const d = new Date(iso);
        return d.toLocaleDateString('es-CO') + ' ' + d.toLocaleTimeString('es-CO', { hour: '2-digit', minute: '2-digit' });
    }

    /** Texto legible de un valor de enumeración del backend. */
    static texto(valor) {
        return Formato.TEXTO[valor] || valor || '—';
    }

    /** Etiqueta de color para un estado o una prioridad. */
    static etiqueta(valor) {
        return '<span class="etiqueta ' + Formato.escapar(String(valor).toLowerCase()) + '">' +
            Formato.escapar(Formato.texto(valor)) + '</span>';
    }

    /** Miniaturas de las fotos de un reporte (las imágenes requieren sesión iniciada). */
    static galeria(urls) {
        if (!urls || urls.length === 0) return '';
        return '<div class="galeria">' + urls.map(u => '<a href="' + Formato.escapar(u) + '" target="_blank"><img src="' +
            Formato.escapar(u) + '" alt="foto"></a>').join('') + '</div>';
    }

    /** Enlace "Cómo llegar" con la ruta de OpenStreetMap hasta el punto. */
    static comoLlegar(latitud, longitud) {
        return '<a href="https://www.openstreetmap.org/directions?route=%3B' + latitud + '%2C' + longitud +
            '#map=17/' + latitud + '/' + longitud + '" target="_blank">Cómo llegar</a>';
    }

    static horario(h) {
        return Formato.DIAS[h.diaSemana] + ' ' + h.horaInicio.substring(0, 5) + '–' + h.horaFin.substring(0, 5);
    }
}

/** Bloques de HTML que se repiten en varias páginas. */
class Plantillas {
    /** Rutas de un sector con sus días y horas (HU-12). */
    static horarios(resultado) {
        if (resultado.rutas.length === 0) {
            return '<p class="ayuda">El sector ' + Formato.escapar(resultado.sector.nombre) + ' no tiene rutas registradas.</p>';
        }
        return '<p>Sector <b>' + Formato.escapar(resultado.sector.nombre) + '</b> · barrios: ' +
            Formato.escapar(resultado.sector.barrios.join(', ')) + '</p>' +
            resultado.rutas.map(ruta => '<div class="tarjeta"><b>' + Formato.escapar(ruta.nombre) + '</b><p>' +
                ruta.horarios.map(Formato.horario).join(' · ') + '</p></div>').join('');
    }

    /** Tabla de puntos de recolección (HU-13). */
    static puntos(puntos) {
        if (puntos.length === 0) return '<p class="ayuda">No hay puntos para ese filtro.</p>';
        const e = Formato.escapar;
        return '<div class="tabla"><table><tr><th>Punto</th><th>Dirección</th><th>Sector</th><th>Materiales</th><th>Horario</th><th></th></tr>' +
            puntos.map(p => '<tr><td>' + e(p.nombre) + '</td><td>' + e(p.direccion) + '</td><td>' + e(p.sector) +
                '</td><td>' + e(p.materiales.join(', ')) + '</td><td>' + e(p.horarioAtencion) + '</td><td>' +
                Formato.comoLlegar(p.latitud, p.longitud) + '</td></tr>').join('') + '</table></div>';
    }

    /** Contenidos educativos agrupados por categoría (HU-14). */
    static contenidos(contenidos) {
        if (contenidos.length === 0) return '<p class="ayuda">No hay contenidos con ese filtro.</p>';
        const porCategoria = {};
        contenidos.forEach(c => (porCategoria[c.categoria] = porCategoria[c.categoria] || []).push(c));
        return Object.entries(porCategoria).map(([categoria, lista]) =>
            '<h3>' + Formato.escapar(Formato.texto(categoria)) + '</h3>' + lista.map(c => '<div class="tarjeta"><b>' +
                Formato.escapar(c.titulo) + '</b><p>' + Formato.escapar(c.cuerpo) + '</p></div>').join('')).join('');
    }

    /** Opciones de una lista desplegable de sectores. */
    static opcionesSectores(sectores, textoVacio) {
        return (textoVacio !== null ? '<option value="">' + Formato.escapar(textoVacio) + '</option>' : '') +
            sectores.map(s => '<option value="' + Formato.escapar(s.nombre) + '">' + Formato.escapar(s.nombre) +
                ' (' + Formato.escapar(s.barrios.join(', ')) + ')</option>').join('');
    }
}

/**
 * Clase base de las páginas. Patrón Método plantilla: iniciar() define los pasos comunes (verificar el
 * rol y mostrar el nombre del usuario) y cada subclase sobrescribe cargar() con lo propio de su página
 * (GRASP Polimorfismo).
 */
class Pagina {
    /** @param rolRequerido rol que debe tener el usuario, o null si la página es pública */
    constructor(rolRequerido) {
        this.rolRequerido = rolRequerido;
        this.usuario = null;
    }

    async iniciar() {
        if (this.rolRequerido) {
            this.usuario = await Sesion.usuarioActual();
            if (!this.usuario) { location.href = '/'; return; }
            if (this.usuario.rol !== this.rolRequerido) { location.href = Sesion.paginaDeRol(this.usuario.rol); return; }
            const nombre = document.getElementById('nombreUsuario');
            if (nombre) nombre.textContent = this.usuario.nombre;
        }
        await this.cargar();
    }

    /** Cada página la sobrescribe para cargar sus datos. */
    async cargar() { }

    /** Acceso corto a un elemento por id. */
    $(id) { return document.getElementById(id); }

    /** Muestra un mensaje de éxito o de error dentro del elemento indicado. */
    mensaje(id, texto, esError) {
        const el = this.$(id);
        el.textContent = texto;
        el.className = 'mensaje ' + (esError ? 'error' : 'exito');
    }

    /** Ejecuta una acción mostrando su resultado; evita repetir try/catch en cada botón. */
    async conMensaje(id, accion, textoExito) {
        try {
            const resultado = await accion();
            if (textoExito) this.mensaje(id, textoExito, false);
            return resultado;
        } catch (e) {
            this.mensaje(id, e.message, true);
            return undefined;
        }
    }

    /** Llena listas desplegables con los sectores; textoVacio null = sin opción vacía. */
    async cargarSectores(ids, textoVacio) {
        const sectores = await api.sectores();
        ids.forEach(id => this.$(id).innerHTML = Plantillas.opcionesSectores(sectores, textoVacio));
    }

    /** Pide la ubicación al GPS del celular o del navegador (el usuario debe dar permiso). */
    obtenerUbicacion() {
        return new Promise((resolver, rechazar) => {
            if (!navigator.geolocation) { rechazar(new Error('Este navegador no permite obtener la ubicación')); return; }
            navigator.geolocation.getCurrentPosition(p => resolver(p.coords),
                () => rechazar(new Error('No se pudo obtener la ubicación')));
        });
    }
}
