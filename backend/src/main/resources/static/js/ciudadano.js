/*
 * ciudadano.js — lógica de la página del ciudadano (ciudadano.html).
 * Usa las funciones comunes de api.js.
 */

// ------------------------------------------------------------------ Nuevo reporte (HU-03, HU-04)

/** Pide la ubicación al GPS del celular o del navegador (el usuario debe dar permiso). */
function usarGps() {
    if (!navigator.geolocation) {
        mensaje('msgReporte', 'Este navegador no permite obtener la ubicación', true);
        return;
    }
    navigator.geolocation.getCurrentPosition(
        posicion => {
            document.getElementById('latitud').value = posicion.coords.latitude.toFixed(6);
            document.getElementById('longitud').value = posicion.coords.longitude.toFixed(6);
        },
        () => mensaje('msgReporte', 'No se pudo obtener la ubicación; escríbala o use el centro de Pasto', true));
}

/** Coordenadas de la plaza de Nariño, útil para probar desde un computador sin GPS. */
function usarCentro() {
    document.getElementById('latitud').value = '1.213600';
    document.getElementById('longitud').value = '-77.281100';
}

document.getElementById('formReporte').addEventListener('submit', async (evento) => {
    evento.preventDefault();
    // El backend espera una petición multipart con dos partes: "datos" (JSON) y "fotos" (archivos)
    const datos = {
        descripcion: document.getElementById('descripcion').value,
        tipoResiduo: document.getElementById('tipoResiduo').value,
        latitud: parseFloat(document.getElementById('latitud').value),
        longitud: parseFloat(document.getElementById('longitud').value)
    };
    const formulario = new FormData();
    formulario.append('datos', new Blob([JSON.stringify(datos)], { type: 'application/json' }));
    for (const foto of document.getElementById('fotos').files) {
        formulario.append('fotos', foto);
    }
    const creado = await conMensaje('msgReporte', () => Api.post('/api/reportes', formulario));
    if (creado) {
        mensaje('msgReporte', 'Reporte enviado. Prioridad sugerida: ' + texto(creado.prioridad) +
            (creado.sector ? ' · Sector: ' + creado.sector : ''), false);
        document.getElementById('formReporte').reset();
        cargarReportes();
    }
});

// ------------------------------------------------------------------ Mis reportes (HU-10)

async function cargarReportes() {
    const estado = document.getElementById('filtroEstado').value;
    const reportes = await Api.get('/api/reportes' + (estado ? '?estado=' + estado : ''));
    const contenedor = document.getElementById('reportes');
    if (reportes.length === 0) {
        contenedor.innerHTML = '<p class="ayuda">Aún no tiene reportes.</p>';
        return;
    }
    contenedor.innerHTML = '<div class="tabla"><table><tr><th>Fecha</th><th>Descripción</th><th>Tipo</th>' +
        '<th>Estado</th><th>Operador</th><th></th></tr>' +
        reportes.map(r => '<tr><td>' + fecha(r.fechaReporte) + '</td><td>' + escapar(r.descripcion) + '</td><td>' +
            texto(r.tipoResiduo) + '</td><td>' + etiqueta(r.estado) + '</td><td>' +
            escapar(r.operador ? r.operador.nombre : '—') + '</td><td><button class="pequeno" onclick="verReporte(\'' +
            r.id + '\')">Ver</button></td></tr>').join('') + '</table></div>';
}

async function verReporte(id) {
    const r = await Api.get('/api/reportes/' + id);
    document.getElementById('detalleReporte').innerHTML = '<div class="tarjeta"><h3>Reporte ' + escapar(r.id) + '</h3>' +
        '<p>' + escapar(r.descripcion) + '</p>' +
        '<p>Estado: ' + etiqueta(r.estado) + ' · Prioridad: ' + etiqueta(r.prioridad) + '</p>' +
        '<p>Dirección: ' + escapar(r.direccion || '—') + ' · Sector: ' + escapar(r.sector || '—') + ' · ' +
        enlaceMapa(r.latitud, r.longitud) + '</p>' +
        (r.reporteOriginalId ? '<p class="ayuda">Vinculado al reporte ' + escapar(r.reporteOriginalId) +
            ' porque describe el mismo problema.</p>' : '') +
        galeria(r.fotos) +
        (r.descripcionTrabajo ? '<h3>Trabajo realizado (' + fecha(r.fechaResolucion) + ')</h3><p>' +
            escapar(r.descripcionTrabajo) + '</p>' + galeria(r.fotosEvidencia) : '') + '</div>';
}

// ------------------------------------------------------------------ Notificaciones (HU-11)

async function cargarNotificaciones() {
    const notificaciones = await Api.get('/api/ciudadanos/yo/notificaciones');
    document.getElementById('notificaciones').innerHTML = notificaciones.length === 0
        ? '<p class="ayuda">Sin notificaciones.</p>'
        : notificaciones.map(n => '<div class="tarjeta"><b>' + escapar(n.asunto) + '</b><p class="ayuda">' +
            fecha(n.fecha) + (n.enviada ? '' : ' · no se pudo enviar por correo') + '</p></div>').join('');
}

// ------------------------------------------------------------------ Horarios y puntos de mi sector (HU-12, HU-13)

async function cargarMiSector() {
    const horarios = await conMensaje('msgHorarios', () => Api.get('/api/horarios/mi-sector'));
    document.getElementById('horarios').innerHTML = horarios ? htmlHorarios(horarios) : '';
    const puntos = await Api.get('/api/puntos/mi-sector');
    document.getElementById('puntos').innerHTML = htmlPuntos(puntos);
}

// ------------------------------------------------------------------ Perfil y contraseña (HU-02)

async function cargarPerfil() {
    const perfil = await Api.get('/api/ciudadanos/yo');
    document.getElementById('perfilNombre').value = perfil.nombre;
    document.getElementById('perfilCorreo').value = perfil.correo;
    document.getElementById('perfilSector').value = perfil.sector || '';
}

document.getElementById('formPerfil').addEventListener('submit', async (evento) => {
    evento.preventDefault();
    const guardado = await conMensaje('msgPerfil', () => Api.put('/api/ciudadanos/yo', {
        nombre: document.getElementById('perfilNombre').value,
        correo: document.getElementById('perfilCorreo').value,
        sector: document.getElementById('perfilSector').value || null
    }), 'Datos actualizados');
    if (guardado) {
        document.getElementById('nombreUsuario').textContent = guardado.nombre;
        cargarMiSector(); // si cambió el sector, cambian los horarios y puntos
    }
});

document.getElementById('formContrasena').addEventListener('submit', async (evento) => {
    evento.preventDefault();
    await conMensaje('msgContrasena', () => Api.put('/api/ciudadanos/yo/contrasena', {
        actual: document.getElementById('contrasenaActual').value,
        nueva: document.getElementById('contrasenaNueva').value
    }), 'Contraseña cambiada');
    document.getElementById('formContrasena').reset();
});

// ------------------------------------------------------------------ Al cargar la página
(async function iniciar() {
    if (!await exigirRol('CIUDADANO')) return;
    await cargarSectores('perfilSector', 'Sin sector');
    await Promise.all([cargarReportes(), cargarNotificaciones(), cargarPerfil(), cargarMiSector()]);
})();
