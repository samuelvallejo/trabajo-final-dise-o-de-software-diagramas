/*
 * inicio.js — página de inicio (index.html): iniciar sesión, registrarse y consultas públicas.
 * Clase PaginaInicio, subclase de Pagina (api.js).
 */
class PaginaInicio extends Pagina {

    constructor() {
        super(null); // página pública
    }

    /** Si ya hay una sesión abierta, se va directo a la página del rol; si no, carga las consultas públicas. */
    async cargar() {
        const usuario = await Sesion.usuarioActual();
        if (usuario) { location.href = Sesion.paginaDeRol(usuario.rol); return; }
        this.mapaPuntos = new Mapa('mapaPuntos');
        this.mapaRutas = new Mapa('mapaRutas');
        this.$('formLogin').addEventListener('submit', e => { e.preventDefault(); this.iniciarSesion(); });
        this.$('formRegistro').addEventListener('submit', e => { e.preventDefault(); this.registrarse(); });
        await this.cargarSectores(['regSector'], 'Sin sector');
        await Promise.all([this.verPuntos(), this.verContenidos()]);
    }

    // ------------------------------------------------------------------ HU-01: iniciar sesión y registrarse
    async iniciarSesion() {
        const usuario = await this.conMensaje('msgLogin', () => api.post('/api/auth/login', {
            correo: this.$('loginCorreo').value,
            contrasena: this.$('loginContrasena').value
        }));
        if (usuario) location.href = Sesion.paginaDeRol(usuario.rol); // cada rol tiene su propia página
    }

    async registrarse() {
        const datos = {
            nombre: this.$('regNombre').value,
            documento: this.$('regDocumento').value,
            correo: this.$('regCorreo').value,
            contrasena: this.$('regContrasena').value,
            sector: this.$('regSector').value || null
        };
        const creado = await this.conMensaje('msgRegistro', () => api.post('/api/auth/registro', datos),
            'Cuenta creada. Ya puede iniciar sesión.');
        if (creado) {
            this.$('formRegistro').reset();
            this.$('loginCorreo').value = datos.correo;
        }
    }

    // ------------------------------------------------------------------ HU-12: horarios y rutas por sector o barrio
    async verHorarios() {
        const busqueda = this.$('buscarSector').value.trim();
        if (!busqueda) return;
        this.$('horarios').innerHTML = '';
        const resultado = await this.conMensaje('msgHorarios',
            () => api.get('/api/horarios?sector=' + encodeURIComponent(busqueda)), ' ');
        if (resultado) {
            this.$('horarios').innerHTML = Plantillas.horarios(resultado);
            this.mapaRutas.mostrarRutas(resultado.rutas);
        }
    }

    // ------------------------------------------------------------------ HU-13: puntos de recolección
    async verPuntos() {
        const material = this.$('filtroMaterial').value;
        const puntos = await api.get('/api/puntos' + (material ? '?material=' + material : ''));
        this.mapaPuntos.mostrarPuntos(puntos);
        this.$('puntos').innerHTML = Plantillas.puntos(puntos);
    }

    // ------------------------------------------------------------------ HU-14: educación ambiental
    async verContenidos() {
        const contenidos = await api.contenidos(this.$('filtroCategoria').value, this.$('buscarContenido').value.trim());
        this.$('contenidos').innerHTML = Plantillas.contenidos(contenidos);
    }
}

const pagina = new PaginaInicio();
pagina.iniciar();
