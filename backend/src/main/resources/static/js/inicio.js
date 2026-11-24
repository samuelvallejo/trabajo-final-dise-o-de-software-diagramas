/*
 * inicio.js — lógica de la página de inicio (index.html).
 * Usa las funciones comunes de api.js y mapas.js.
 */

let mapaPuntos, capaPuntos, mapaRutas, capaRutas;

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

// ------------------------------------------------------------------ Horarios y rutas (HU-12)
async function verHorarios() {
    const busqueda = document.getElementById('buscarSector').value.trim();
    if (!busqueda) return;
    const contenedor = document.getElementById('horarios');
    contenedor.innerHTML = '';
    const resultado = await conMensaje('msgHorarios',
        () => Api.get('/api/horarios?sector=' + encodeURIComponent(busqueda)), ' ');
    if (resultado) {
        contenedor.innerHTML = htmlHorarios(resultado);
        dibujarRutas(mapaRutas, capaRutas, resultado.rutas);
    }
}

// ------------------------------------------------------------------ Puntos de recolección (HU-13)
async function verPuntos() {
    const material = document.getElementById('filtroMaterial').value;
    const puntos = await Api.get('/api/puntos' + (material ? '?material=' + material : ''));
    dibujarPuntos(mapaPuntos, capaPuntos, puntos);
    document.getElementById('puntos').innerHTML = htmlPuntos(puntos);
}

// ------------------------------------------------------------------ Educación ambiental (HU-14)
function verContenidos() {
    return cargarContenidos('contenidos', document.getElementById('filtroCategoria').value,
        document.getElementById('buscarContenido').value.trim());
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
    mapaPuntos = crearMapa('mapaPuntos');
    capaPuntos = L.featureGroup().addTo(mapaPuntos);
    mapaRutas = crearMapa('mapaRutas');
    capaRutas = L.featureGroup().addTo(mapaRutas);
    await cargarSectores('regSector', 'Sin sector');
    await Promise.all([verPuntos(), verContenidos()]);
})();
