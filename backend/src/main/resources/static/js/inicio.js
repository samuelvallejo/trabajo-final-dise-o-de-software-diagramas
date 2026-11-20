/*
 * inicio.js — lógica de la página de inicio (index.html).
 * Usa las funciones comunes de api.js.
 */

// ------------------------------------------------------------------ Iniciar sesión (HU-01)
document.getElementById('formLogin').addEventListener('submit', async (evento) => {
    evento.preventDefault(); // evita que el formulario recargue la página
    const usuario = await conMensaje('msgLogin', () => Api.post('/api/auth/login', {
        correo: document.getElementById('loginCorreo').value,
        contrasena: document.getElementById('loginContrasena').value
    }));
    if (usuario) {
        location.href = paginaDeRol(usuario.rol); // cada rol tiene su propia página
    }
});

// ------------------------------------------------------------------ Registro de ciudadano (HU-01)
document.getElementById('formRegistro').addEventListener('submit', async (evento) => {
    evento.preventDefault();
    const datos = {
        nombre: document.getElementById('regNombre').value,
        documento: document.getElementById('regDocumento').value,
        correo: document.getElementById('regCorreo').value,
        contrasena: document.getElementById('regContrasena').value,
        sector: document.getElementById('regSector').value || null
    };
    const creado = await conMensaje('msgRegistro', () => Api.post('/api/auth/registro', datos),
        'Cuenta creada. Ya puede iniciar sesión.');
    if (creado) {
        document.getElementById('formRegistro').reset();
        document.getElementById('loginCorreo').value = datos.correo;
    }
});

// ------------------------------------------------------------------ Horarios por sector o barrio (HU-12)
async function verHorarios() {
    const busqueda = document.getElementById('buscarSector').value.trim();
    if (!busqueda) return;
    const contenedor = document.getElementById('horarios');
    contenedor.innerHTML = '';
    const resultado = await conMensaje('msgHorarios',
        () => Api.get('/api/horarios?sector=' + encodeURIComponent(busqueda)), ' ');
    if (resultado) contenedor.innerHTML = htmlHorarios(resultado);
}

// ------------------------------------------------------------------ Puntos de recolección (HU-13)
async function verPuntos() {
    const material = document.getElementById('filtroMaterial').value;
    const puntos = await Api.get('/api/puntos' + (material ? '?material=' + material : ''));
    document.getElementById('puntos').innerHTML = htmlPuntos(puntos);
}

// ------------------------------------------------------------------ Educación ambiental (HU-14)
async function verContenidos() {
    const contenidos = await Api.get('/api/contenidos');
    document.getElementById('contenidos').innerHTML = contenidos.length === 0
        ? '<p class="ayuda">Aún no hay contenido publicado.</p>'
        : contenidos.map(c => '<div class="tarjeta"><b>' + escapar(c.titulo) + '</b> <span class="ayuda">' +
            escapar(c.categoria) + '</span><p>' + escapar(c.cuerpo) + '</p></div>').join('');
}

// ------------------------------------------------------------------ Al cargar la página
(async function iniciar() {
    // Si ya hay una sesión abierta, se va directo a la página del rol
    try {
        const yo = await Api.get('/api/auth/yo');
        location.href = paginaDeRol(yo.rol);
        return;
    } catch (e) {
        // sin sesión: se queda en el inicio
    }
    await cargarSectores('regSector', 'Sin sector');
    await verPuntos();
    await verContenidos();
})();
