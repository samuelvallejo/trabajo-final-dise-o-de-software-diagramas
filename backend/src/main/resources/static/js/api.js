/*
 * api.js — funciones comunes a todas las páginas del frontend.
 *
 * Todas las páginas hablan con el backend solo a través del objeto Api (un único punto de acceso,
 * equivalente al patrón GRASP de Indirección: si cambia una ruta o el manejo de errores, se cambia aquí).
 *
 * La sesión se mantiene con la cookie JSESSIONID que crea el backend al iniciar sesión; el navegador
 * la envía sola en cada petición al mismo sitio.
 */

const Api = {
    /**
     * Hace una petición a la API y devuelve el JSON de la respuesta.
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
            const detalle = datos && (datos.detail || datos.title);
            throw new Error(detalle || 'Error ' + respuesta.status);
        }
        return datos;
    },
    get(ruta) { return this.pedir('GET', ruta); },
    post(ruta, cuerpo) { return this.pedir('POST', ruta, cuerpo); },
    put(ruta, cuerpo) { return this.pedir('PUT', ruta, cuerpo); },
    borrar(ruta) { return this.pedir('DELETE', ruta); }
};

/** Página a la que va cada rol después de iniciar sesión. */
function paginaDeRol(rol) {
    return { CIUDADANO: '/ciudadano', ADMINISTRADOR: '/administrador', OPERADOR: '/operador' }[rol] || '/';
}

/**
 * Protege una página: si no hay sesión vuelve al inicio y si el rol no corresponde lleva a la página
 * de su rol. Devuelve los datos del usuario y escribe su nombre en la barra superior.
 */
async function exigirRol(rolEsperado) {
    try {
        const yo = await Api.get('/api/auth/yo');
        if (yo.rol !== rolEsperado) {
            location.href = paginaDeRol(yo.rol);
            return null;
        }
        const nombre = document.getElementById('nombreUsuario');
        if (nombre) nombre.textContent = yo.nombre;
        return yo;
    } catch (e) {
        location.href = '/';
        return null;
    }
}

async function cerrarSesion() {
    try { await Api.post('/api/auth/logout'); } finally { location.href = '/'; }
}

/** Muestra un mensaje de éxito o de error dentro del elemento indicado. */
function mensaje(idElemento, texto, esError) {
    const el = document.getElementById(idElemento);
    el.textContent = texto;
    el.className = 'mensaje ' + (esError ? 'error' : 'exito');
}

/**
 * Ejecuta una acción mostrando su resultado en un elemento de mensajes.
 * Evita repetir try/catch en cada botón.
 */
async function conMensaje(idElemento, accion, textoExito) {
    try {
        const resultado = await accion();
        if (textoExito) mensaje(idElemento, textoExito, false);
        return resultado;
    } catch (e) {
        mensaje(idElemento, e.message, true);
        return undefined;
    }
}

/** Evita que un texto escrito por un usuario se interprete como HTML (protección contra XSS). */
function escapar(valor) {
    return String(valor ?? '').replace(/[&<>"']/g, c => (
        { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

/** Convierte "2026-11-20T10:30:00" en "20/11/2026 10:30". */
function fecha(iso) {
    if (!iso) return '—';
    const d = new Date(iso);
    return d.toLocaleDateString('es-CO') + ' ' + d.toLocaleTimeString('es-CO', { hour: '2-digit', minute: '2-digit' });
}

/** Textos legibles de los valores de los enum del backend. */
const TEXTO = {
    PENDIENTE: 'Pendiente', ASIGNADO: 'Asignado', EN_PROCESO: 'En proceso', RESUELTO: 'Resuelto',
    ALTA: 'Alta', MEDIA: 'Media', BAJA: 'Baja',
    DOMESTICO: 'Doméstico', ESCOMBROS: 'Escombros', RECICLABLE: 'Reciclable', VOLUMINOSO: 'Voluminoso',
    PELIGROSO: 'Peligroso', OTRO: 'Otro'
};

function texto(valor) {
    return TEXTO[valor] || valor || '—';
}

/** Etiqueta de color para un estado o una prioridad. */
function etiqueta(valor) {
    return '<span class="etiqueta ' + escapar(String(valor).toLowerCase()) + '">' + escapar(texto(valor)) + '</span>';
}

/** Miniaturas de las fotos de un reporte (las imágenes requieren sesión iniciada). */
function galeria(urls) {
    if (!urls || urls.length === 0) return '';
    return '<div class="galeria">' + urls.map(u =>
        '<a href="' + escapar(u) + '" target="_blank"><img src="' + escapar(u) + '" alt="foto"></a>').join('') + '</div>';
}

/** Llena una lista desplegable con los sectores (se usa en registro, perfil y filtros). */
async function cargarSectores(idSelect, textoVacio) {
    const select = document.getElementById(idSelect);
    const sectores = await Api.get('/api/sectores');
    select.innerHTML = '<option value="">' + escapar(textoVacio) + '</option>' +
        sectores.map(s => '<option value="' + escapar(s.nombre) + '">' + escapar(s.nombre) +
            ' (' + escapar(s.barrios.join(', ')) + ')</option>').join('');
}

// ------------------------------------------------------------------ vistas compartidas

/** Dibuja las rutas de un sector con sus días y horas. (inicio y ciudadano). */
function htmlHorarios(resultado) {
    const DIAS = { MONDAY: 'Lunes', TUESDAY: 'Martes', WEDNESDAY: 'Miércoles', THURSDAY: 'Jueves',
        FRIDAY: 'Viernes', SATURDAY: 'Sábado', SUNDAY: 'Domingo' };
    if (resultado.rutas.length === 0) {
        return '<p class="ayuda">El sector ' + escapar(resultado.sector.nombre) + ' no tiene rutas registradas.</p>';
    }
    return '<p>Sector <b>' + escapar(resultado.sector.nombre) + '</b> · barrios: ' +
        escapar(resultado.sector.barrios.join(', ')) + '</p>' +
        resultado.rutas.map(ruta => '<div class="tarjeta"><b>' + escapar(ruta.nombre) + '</b><p>' +
            ruta.horarios.map(h => DIAS[h.diaSemana] + ' ' + h.horaInicio.substring(0, 5) + '–' + h.horaFin.substring(0, 5))
                .join(' · ') + '</p></div>').join('');
}

/** Tabla de puntos de recolección. (inicio y ciudadano). */
function htmlPuntos(puntos) {
    if (puntos.length === 0) return '<p class="ayuda">No hay puntos para ese filtro.</p>';
    return '<div class="tabla"><table><tr><th>Punto</th><th>Dirección</th><th>Sector</th><th>Materiales</th><th>Horario</th><th></th></tr>' +
        puntos.map(p => '<tr><td>' + escapar(p.nombre) + '</td><td>' + escapar(p.direccion) + '</td><td>' +
            escapar(p.sector) + '</td><td>' + escapar(p.materiales.join(', ')) + '</td><td>' +
            escapar(p.horarioAtencion) + '</td><td>' + enlaceComoLlegar(p.latitud, p.longitud) + '</td></tr>').join('') +
        '</table></div>';
}

/**
 * Contenidos educativos agrupados por categoría (HU-14). Se usa en el inicio y en la página del ciudadano.
 * El filtro por categoría y la búsqueda por palabra los hace el backend (/api/contenidos?categoria=&q=).
 */
async function cargarContenidos(idContenedor, categoria, palabra) {
    const parametros = new URLSearchParams();
    if (categoria) parametros.append('categoria', categoria);
    if (palabra) parametros.append('q', palabra);
    const contenidos = await Api.get('/api/contenidos?' + parametros);
    const contenedor = document.getElementById(idContenedor);
    if (contenidos.length === 0) {
        contenedor.innerHTML = '<p class="ayuda">No hay contenidos con ese filtro.</p>';
        return;
    }
    const porCategoria = {};
    contenidos.forEach(c => (porCategoria[c.categoria] = porCategoria[c.categoria] || []).push(c));
    contenedor.innerHTML = Object.entries(porCategoria).map(([cat, lista]) =>
        '<h3>' + escapar(TEXTO_CATEGORIA[cat] || cat) + '</h3>' + lista.map(c => '<div class="tarjeta"><b>' +
            escapar(c.titulo) + '</b><p>' + escapar(c.cuerpo) + '</p></div>').join('')).join('');
}

const TEXTO_CATEGORIA = {
    SEPARACION_RESIDUOS: 'Separación de residuos', RECICLAJE: 'Reciclaje',
    REDUCCION_BASURA: 'Reducción de basura', BUENAS_PRACTICAS: 'Buenas prácticas'
};
