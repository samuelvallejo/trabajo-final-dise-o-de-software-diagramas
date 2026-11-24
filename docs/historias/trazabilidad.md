# EcoGestión – Matriz de trazabilidad

Relaciona cada necesidad del caso de estudio con su historia de usuario y con los elementos de los
cinco diagramas (vista de contexto, funcional, lógica, de desarrollo y de despliegue).

## 1. Necesidades del caso → historias → diagramas

| Necesidad del caso de estudio | Historia | Componente (vista funcional) | Clases y atributos (vistas lógica y de desarrollo) | Despliegue / externos |
|---|---|---|---|---|
| Registro e inicio de sesión con nombre, documento, correo y contraseña | HU-01 | Gestión de usuarios | `Ciudadano` (nombre, documento, correo, contrasenaHash) | Navegador → Servidor web/API (HTTPS); MongoDB |
| Consultar y actualizar datos personales | HU-02 | Gestión de usuarios | `Ciudadano` (correo, sector, getters/setters) | MongoDB |
| Reportar acumulación o residuos abandonados con descripción, tipo y fotos | HU-03 | Gestión de reportes (requiere *Validar ciudadano*) | `Reporte` (descripcion, tipoResiduo, fotos); `GestorReportes.registrarReporte`; `ReporteController/Service.registrarReporte` | Fotos en MongoDB GridFS; URL en el `Reporte` |
| Ubicación por GPS o seleccionando un punto en el mapa | HU-04 | Gestión de reportes (requiere *Servicio de mapas/GPS* e *Identificar sector*) | `Reporte` (latitud, longitud, direccion, sector) | Sensor GPS del smartphone; mapa de OpenStreetMap (Leaflet) + geocodificación con Nominatim |
| Clasificación por tipo de problema y prioridad de atención | HU-05 | Clasificación y priorización (*Clasificar y priorizar*) | `Reporte` (tipoResiduo, prioridad); `ReporteRepository.findByEstado` (bandeja de pendientes) | — |
| Revisar reportes y detectar duplicados | HU-06 | Gestión de reportes | `Reporte.reporteOriginal`, asociación `duplicadoDe`, `marcarComoDuplicado` | Índice 2dsphere de MongoDB |
| Asignar a un operador sin que varios equipos atiendan lo mismo | HU-07 | Asignación y atención (requiere *Consultar operadores* y *Consultar y actualizar estado del reporte*) | `Reporte.operador [0..1]`, `Reporte.version`, `Operador.disponible`; `ReporteService.asignarReporte / validarDisponibilidad` | — |
| El operador consulta sus reportes asignados y su ubicación | HU-08 | Asignación y atención | asociación `atiende` (Operador 0..1 — 0..* Reporte) | Smartphone del operador; mapa de OpenStreetMap (Leaflet) con color por prioridad |
| Actualizar estado (pendiente → asignado → en proceso → resuelto) | HU-09 | Asignación y atención | `Reporte.estado`, `cambiarEstado` | — |
| Registrar el trabajo realizado y fotos de evidencia al terminar | HU-09 | Asignación y atención | `Reporte` (descripcionTrabajo, fotosEvidencia, fechaResolucion), `registrarCierre` | Fotos en MongoDB GridFS |
| El ciudadano consulta el historial y el estado de sus reportes | HU-10 | Gestión de reportes | asociación `historial` (Ciudadano 1 — 0..* Reporte); `listarReportes / obtenerReporte` | — |
| Notificaciones al asignar, empezar a atender o resolver (correo, extensible) | HU-11 | Notificaciones (provee *Notificar cambio de estado*; requiere *Obtener correo del ciudadano* y la interfaz externa *Canal de notificación*) | — | Servicio de correo electrónico (SMTP/TLS:587) |
| Horarios y rutas de recolección por barrio/sector, actualizados por el administrador | HU-12 | Horarios y rutas | `Ciudadano.sector` | — |
| Puntos de recolección autorizados en un mapa (materiales y horario) | HU-13 | Puntos de recolección (requiere *Consultar sector*) | — | Mapa de OpenStreetMap (Leaflet) |
| Contenido de educación ambiental publicado por el administrador | HU-14 | Contenido educativo (requiere *Validar rol de administrador*) | — | — |
| Panel administrativo (totales, problemas frecuentes, pendientes/resueltos, sectores) | HU-15 | Estadísticas e informes (requiere *Consultar reportes* y *Consultar tipos y prioridades*) | `Reporte` (estado, tipoResiduo, sector) | — |
| Estadísticas e informes para evaluar la atención y planificar limpiezas | HU-15 | Estadísticas e informes | `Reporte` (fechaReporte, fechaResolucion) | — |

## 2. Flujos de la vista de contexto → historias

| Flujo | Historias |
|---|---|
| Ciudadano → EcoGestión: registro e inicio de sesión, datos personales, reporte + fotos + ubicación | HU-01, HU-02, HU-03, HU-04 |
| EcoGestión → Ciudadano: estado e historial de reportes, horarios, rutas y puntos, contenido educativo | HU-10, HU-12, HU-13, HU-14 |
| Administrador → EcoGestión: clasificación, prioridad y asignación; horarios, rutas, puntos y contenido | HU-05, HU-07, HU-12, HU-13, HU-14 |
| EcoGestión → Administrador: reportes nuevos y posibles duplicados; panel, estadísticas e informes | HU-05, HU-06, HU-15 |
| EcoGestión → Operador: reportes asignados + ubicación | HU-08 |
| Operador → EcoGestión: estado de atención; trabajo realizado + fotos de evidencia | HU-09 |
| EcoGestión ↔ Servicio de correo electrónico | HU-11 |
| EcoGestión ↔ Servicio de mapas/GPS | HU-04, HU-08, HU-13 |

## 3. Conexiones de la vista funcional → historias

| Consumidor → Proveedor | Servicio | Historias |
|---|---|---|
| Gestión de reportes → Gestión de usuarios | Validar ciudadano | HU-03, HU-10 |
| Gestión de reportes → Clasificación y priorización | Clasificar y priorizar | HU-05 |
| Gestión de reportes → Horarios y rutas | Identificar sector | HU-04 |
| Asignación y atención → Gestión de reportes | Consultar y actualizar estado del reporte | HU-07, HU-08, HU-09 |
| Asignación y atención → Gestión de usuarios | Consultar operadores | HU-07 |
| Asignación y atención → Notificaciones | Notificar cambio de estado | HU-11 |
| Notificaciones → Gestión de usuarios | Obtener correo del ciudadano | HU-11 |
| Estadísticas e informes → Gestión de reportes | Consultar reportes | HU-15 |
| Estadísticas e informes → Clasificación y priorización | Consultar tipos y prioridades | HU-15 |
| Contenido educativo → Gestión de usuarios | Validar rol de administrador | HU-14 |
| Puntos de recolección → Horarios y rutas | Consultar sector | HU-13 |
| Gestión de reportes → (externo) | Servicio de mapas/GPS | HU-04 |
| Notificaciones → (externo) | Canal de notificación (correo · SMS · push) | HU-11 |

## 4. Decisiones de coherencia

- El modelo de clases inicial tiene cuatro clases (`GestorReportes`, `Ciudadano`, `Operador`, `Reporte`). Por eso
  la asignación se modela con el atributo `Reporte.operador [0..1]` y no con una clase aparte: un reporte nunca
  tiene dos operadores, y el campo `version` del documento MongoDB impide que dos administradores lo asignen a la vez.
- Los estados del reporte son Pendiente → Asignado → En proceso → Resuelto (`Reporte.estado`).
- Los mapas de las pantallas (HU-08, HU-12, HU-13, HU-15) se dibujan en el navegador con Leaflet y las teselas
  de OpenStreetMap, que no necesitan clave (vista de despliegue). Solo Gestión de reportes llama al servicio de mapas desde el servidor, para la
  geocodificación con Nominatim (HU-04).
- Las historias no piden notificaciones dentro de la aplicación. Por eso la vista de contexto solo muestra
  notificaciones por correo electrónico.
- Los tipos del reporte son enumeraciones: `TipoResiduo`, `Prioridad` y `EstadoReporte`; las fechas son `LocalDateTime`.
- En el código, `Ciudadano`, `Operador` y `Administrador` heredan de la clase abstracta `Usuario`
  (nombre, documento, correo, contraseña cifrada con BCrypt y bloqueo tras 5 intentos fallidos).
  El papel de `GestorReportes` del modelo inicial lo cumple `ReporteService`, usado por el administrador.
- Las fotos de los reportes y de las evidencias se guardan en MongoDB GridFS dentro de la misma base de datos
  (MongoDB Atlas), para no depender de servicios distintos a Spring Boot y MongoDB.
- Si no se configura un servidor SMTP, las notificaciones se guardan igual en el historial y se escriben en el
  registro de la aplicación (`CanalRegistro`); con SMTP se envían por correo (`CanalCorreo`).

## 5. Componentes de la vista funcional → código (backend Spring Boot)

| Componente | Servicio (`service`) | Controlador y rutas (`controller`) | Historias |
|---|---|---|---|
| Gestión de usuarios | `UsuarioService` | `AuthController` (`/api/auth/registro`, `/login`, `/logout`, `/yo`), `CiudadanoController` (`/api/ciudadanos/yo`, `/contrasena`, `/notificaciones`), `OperadorController` (`/api/admin/operadores`) | HU-01, HU-02, HU-07 |
| Gestión de reportes | `ReporteService` | `ReporteController` (`POST/GET /api/reportes`, `GET /api/reportes/{id}`, `/posibles-duplicados`, `/duplicado`) | HU-03, HU-04, HU-06, HU-10 |
| Clasificación y priorización | `ClasificacionService` | `PUT /api/reportes/{id}/clasificacion`, `GET /api/clasificacion/tipos` | HU-05 |
| Asignación y atención | `ReporteService.asignarReporte`, `AtencionService` | `PUT /api/reportes/{id}/asignacion`, `AtencionController` (`/api/operador/asignaciones`, `/{id}/inicio`, `/{id}/cierre`) | HU-07, HU-08, HU-09 |
| Notificaciones | `NotificacionService` + `CanalNotificacion` (`CanalCorreo`, `CanalRegistro`) | `GET /api/ciudadanos/yo/notificaciones` | HU-11 |
| Horarios y rutas | `HorarioRutaService` | `HorarioRutaController` (`/api/sectores`, `/api/horarios`, `/api/horarios/mi-sector`, `/api/admin/rutas`) | HU-12, HU-04 |
| Puntos de recolección | `PuntoRecoleccionService` | `PuntoRecoleccionController` (`/api/puntos`, `/api/puntos/mi-sector`, `/api/admin/puntos`) | HU-13 |
| Contenido educativo | `ContenidoEducativoService` | `ContenidoEducativoController` (`/api/contenidos`, `/api/admin/contenidos`) | HU-14 |
| Estadísticas e informes | `EstadisticaService` | `EstadisticaController` (`/api/admin/estadisticas/panel`, `/informe`, `/informe.csv`) | HU-15 |
| Servicio de mapas/GPS (externo) | `ServicioMapas` (`OpenStreetMapServicioMapas`, `CoordenadasServicioMapas`) | — | HU-04 |
| Almacenamiento de imágenes | `AlmacenamientoImagenes` (`GridFsAlmacenamientoImagenes`) | `GET /api/imagenes/{id}` | HU-03, HU-09 |
| Interfaz web (vistas Thymeleaf + JS) | — | `PaginasController` (`/`, `/ciudadano`, `/administrador`, `/operador`) con `static/js/*.js` | HU-01 a HU-15 |
