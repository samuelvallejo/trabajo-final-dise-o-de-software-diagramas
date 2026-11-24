# EcoGestión – Diseño de arquitectura

Sistema para reportar y gestionar la disposición inadecuada de residuos sólidos en Pasto (Nariño):
reportes ciudadanos con fotos y ubicación, clasificación y priorización, asignación a operadores de
limpieza, seguimiento, notificaciones, horarios y rutas de recolección, puntos de recolección,
educación ambiental y estadísticas.

## Contenido

Los 5 diagramas están modelados en **Visual Paradigm 18.1**, un proyecto `.vpp` por diagrama en
[`docs/diagramas/visual-paradigm`](docs/diagramas/visual-paradigm) (ábrelos con *File > Open Project*).

Las imágenes de la presentación son exportaciones limpias, porque la copia de Visual Paradigm usada es de
evaluación y marca todas sus exportaciones con una marca de agua. Con una licencia activada, cada `.vpp` se
puede exportar directamente (*Export > Active Diagram as Image*).

| Vista | Proyecto Visual Paradigm | Imagen |
|---|---|---|
| Contexto (DFD nivel 0) | [01-vista-contexto.vpp](docs/diagramas/visual-paradigm/01-vista-contexto.vpp) | [PNG](docs/diagramas/01-vista-contexto.png) |
| Funcional (componentes UML) | [02-vista-funcional.vpp](docs/diagramas/visual-paradigm/02-vista-funcional.vpp) | [PNG](docs/diagramas/02-vista-funcional.png) |
| Lógica (modelo de clases inicial) | [03-vista-logica.vpp](docs/diagramas/visual-paradigm/03-vista-logica.vpp) | [PNG](docs/diagramas/03-vista-logica.png) |
| Desarrollo (clases por capas controller → service → repository → model) | [04-vista-desarrollo.vpp](docs/diagramas/visual-paradigm/04-vista-desarrollo.vpp) | [PNG](docs/diagramas/04-vista-desarrollo.png) |
| Despliegue (despliegue UML) | [05-vista-despliegue.vpp](docs/diagramas/visual-paradigm/05-vista-despliegue.vpp) | [PNG](docs/diagramas/05-vista-despliegue.png) |

- Presentación: [EcoGestion-Diseno-de-arquitectura.pptx](docs/EcoGestion-Diseno-de-arquitectura.pptx)
- Historias de usuario: [historias-de-usuario.md](docs/historias/historias-de-usuario.md) (también publicadas como issues)
- Matriz de trazabilidad caso de estudio → historias → diagramas: [trazabilidad.md](docs/historias/trazabilidad.md)

## Stack tecnológico

| Capa | Tecnología |
|---|---|
| Aplicación (backend y vistas) | **Java 21 + Spring Boot 4**: Spring MVC (API REST), frontend HTML con plantillas **Thymeleaf**, JavaScript y mapas con **Leaflet**, Spring Security, Spring Data MongoDB, Bean Validation. Sin React, Angular ni Node. |
| Clientes | Navegador web del PC o del celular (el GPS se toma con la API de geolocalización del navegador) |
| Base de datos | **MongoDB Atlas** (clúster gratuito en AWS N. Virginia; colecciones por agregado e índices `2dsphere` para consultas por cercanía) |
| Almacenamiento de imágenes | MongoDB **GridFS** (en la misma base de datos) |
| Despliegue | **Railway** (región US East) para la aplicación Spring Boot + MongoDB Atlas para los datos |
| Correo | Servidor SMTP (relay) |
| Mapas y geocodificación | **OpenStreetMap**: teselas del mapa en el navegador (Leaflet) y dirección de cada reporte con Nominatim; no necesita clave |

## Backend (carpeta `backend`)

Aplicación Spring Boot orientada a objetos, organizada por capas como la vista de desarrollo:

```
backend/src/main/java/com/ecogestion
├── controller   API REST (un controlador por componente de la vista funcional) + manejo de errores
├── service      lógica de aplicación (UsuarioService, ReporteService, ClasificacionService, AtencionService,
│                NotificacionService, HorarioRutaService, PuntoRecoleccionService, ContenidoEducativoService,
│                EstadisticaService)
├── repository   interfaces Spring Data MongoDB
├── model        modelo de dominio rico: Usuario (abstracta) → Ciudadano, Operador, Administrador; Reporte
│                (máquina de estados Pendiente → Asignado → En proceso → Resuelto, duplicados, cierre), Sector,
│                RutaRecoleccion, PuntoRecoleccion, ContenidoEducativo, Notificacion y enumeraciones
├── integracion  interfaces y estrategias: CanalNotificacion (correo/registro), ServicioMapas (OpenStreetMap/coordenadas),
│                AlmacenamientoImagenes (GridFS)
├── security     autenticación con Spring Security (sesión HTTP, BCrypt, bloqueo tras 5 intentos)
├── config       seguridad, propiedades y datos iniciales (sectores, rutas, puntos y contenido de Pasto)
└── dto          registros de entrada y salida de la API

backend/src/main/resources
├── templates    páginas HTML (Thymeleaf): index, ciudadano, administrador, operador y fragmentos comunes
└── static       css/estilos.css y js/: clases JavaScript (api.js: ClienteApi, Sesion, Formato, Plantillas y la clase
                 base Pagina; mapas.js: Mapa; y una subclase de Pagina por página)
```

Cada clase tiene un comentario que explica qué hace, cómo funciona y qué patrón GRASP aplica y por qué.
Resumen:

| Patrón GRASP | Dónde se aplica | Por qué |
|---|---|---|
| Controlador | `controller/*Controller` | Reciben las peticiones HTTP y delegan en los servicios; no tienen reglas de negocio |
| Experto en información | `Reporte`, `Usuario`, `Operador`, `EstadoReporte`, `TipoResiduo`, `Coordenada`, `EstadisticasReportes`, `Notificacion`... | Cada regla está en la clase que tiene los datos: `Reporte.asignar` impide la doble asignación, `Coordenada.distanciaMetros` calcula distancias, `EstadisticasReportes` calcula los indicadores y la `Notificacion` se redacta sola |
| Creador | `ReporteService` crea `Reporte`, `UsuarioService` crea usuarios, `Notificacion.porCambioDeEstado` crea cada aviso | Quien tiene los datos de inicialización crea el objeto |
| Alta cohesión | Un servicio por componente de la vista funcional | Cada clase tiene un solo tema (reportes, usuarios, notificaciones...) |
| Bajo acoplamiento | DTOs en la API, servicios que usan servicios de otros componentes y no sus repositorios | Un cambio interno no se propaga al resto |
| Polimorfismo | `Rol` (cada rol decide qué reportes ve), `Usuario.getRol()`/`guardarEn()`/`getSector()`, `CanalNotificacion`, `ServicioMapas`, `AlmacenamientoImagenes`; en el frontend, las subclases de `Pagina` | Se cambia el comportamiento sin `switch`, `if/else` ni `instanceof` por tipo |
| Fabricación pura | Servicios, repositorios, `ManejadorErrores`, `ValidadorFotos` | Clases técnicas que no existen en el caso de estudio pero mantienen limpio el dominio |
| Indirección | Repositorios, `CatalogoReportes`, `RegistroUsuarios`, `CanalNotificacion`, `CuentaUsuario`, `EventosAutenticacion`; en el frontend, `ClienteApi` y `Mapa` | Un intermediario desacopla dos partes (servicio ↔ MongoDB, servicio ↔ SMTP, dominio ↔ Spring Security) |
| Variaciones protegidas | Interfaces de `integracion` y `EcoGestionProperties` | Cambiar de proveedor de correo, mapas o almacenamiento, o un parámetro, no obliga a tocar la lógica |

**Orientación a objetos sin lógica procedimental.** No hay `switch` ni `instanceof` sobre tipos: lo que cambia
según el rol, el tipo de usuario o el estado lo resuelve el propio objeto (`Rol.puedeVer`, `Usuario.guardarEn`,
`EstadoReporte.etiqueta`). Los cálculos que antes eran funciones estáticas viven en objetos: `Coordenada` (distancias),
`EstadisticasReportes` (indicadores del panel e informes) y el informe, que se exporta a CSV con `aCsv()`. En el
frontend, cada página es una clase que hereda de `Pagina` (patrón Método plantilla).

**Encapsulamiento (sin get/set casuales).** Las clases del modelo no tienen setters: cada cambio pasa por un
método que expresa la acción y valida su regla (`Reporte.asignar`, `Reporte.registrarCierre`,
`Ciudadano.actualizarPerfil`, `Usuario.cambiarContrasena`, `Operador.marcarOcupado`), y `Reporte.cambiarEstado`
es privado para que nadie se salte el flujo de estados. Spring Data guarda y lee los atributos por reflexión,
así que no necesita setters. Solo existen los getters que alguien usa (para armar las respuestas de la API, las
notificaciones o las estadísticas); son de solo lectura y las listas se devuelven inmodificables. El hash de la
contraseña solo lo lee Spring Security y nunca sale en la API. Las vistas lógica y de desarrollo muestran esta
regla con una nota de encapsulamiento.

### Frontend

| Página | Rol | Qué permite |
|---|---|---|
| `/` | Público | Iniciar sesión, registrarse, consultar horarios y rutas por sector o barrio, puntos de recolección en el mapa y educación ambiental |
| `/ciudadano` | Ciudadano | Crear reportes con fotos y ubicación por GPS o tocando el mapa, ver su historial y detalle, notificaciones, horarios y ruta de su sector, puntos en el mapa (por material, sector o cercanía), educación ambiental, perfil y contraseña |
| `/administrador` | Administrador | Panel con mapa de calor, bandeja con filtros, clasificar, revisar duplicados, asignar operadores, crear operadores, informe y CSV, rutas y horarios, puntos y contenido educativo (crear y editar) |
| `/operador` | Operador | Ver sus reportes asignados en lista y en mapa (color por prioridad), cómo llegar, iniciar la atención y cerrarlos con fotos de evidencia |

Las páginas solo traen la estructura; los datos se piden a la API con `fetch` y la sesión viaja en la cookie
`JSESSIONID`. Si alguien abre una página que no corresponde a su rol, se le lleva a la suya.

### Ejecutar en local

Requiere Java 21 y un MongoDB local (`mongodb://localhost:27017/ecogestion` por defecto).

```bash
cd backend && ./mvnw spring-boot:run
```

Luego abrir `http://localhost:8080`. Para entrar como administrador, definir antes `ADMIN_CORREO` y `ADMIN_CONTRASENA`.

Pruebas unitarias (modelo de dominio y servicios):

```bash
cd backend && ./mvnw test
```

### Variables de entorno

| Variable | Uso |
|---|---|
| `MONGO_URL` | Cadena de conexión de MongoDB (`mongodb+srv://...` de MongoDB Atlas) |
| `MONGO_DATABASE` | Nombre de la base de datos (por defecto `ecogestion`) |
| `ADMIN_CORREO`, `ADMIN_CONTRASENA` | Cuenta del primer administrador, que se crea al iniciar |
| `DATOS_DEMO` | `true` (por defecto) carga sectores, rutas, puntos y contenido de ejemplo de Pasto |
| `MAPAS_GEOCODIFICACION` | `osm` (por defecto) obtiene la dirección con Nominatim de OpenStreetMap; `coordenadas` no usa servicios externos |
| `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`, `NOTIFICACIONES_CANAL=correo` | Opcional: envío real de correos; sin ellas las notificaciones quedan en el historial y en el registro |
| `PORT` | Lo asigna Railway |

### API principal (por rol)

| Rol | Rutas |
|---|---|
| Público | `POST /api/auth/registro`, `POST /api/auth/login`, `GET /api/sectores`, `/api/horarios?sector=`, `/api/puntos?material=`, `/api/contenidos`, `/api/clasificacion/tipos`, `/actuator/health` |
| Ciudadano | `GET/PUT /api/ciudadanos/yo`, `PUT /api/ciudadanos/yo/contrasena`, `GET /api/ciudadanos/yo/notificaciones`, `POST /api/reportes` (multipart `datos` + `fotos`), `GET /api/reportes`, `GET /api/reportes/{id}`, `GET /api/horarios/mi-sector`, `GET /api/puntos/mi-sector` |
| Administrador | `GET /api/reportes?estado=&tipoResiduo=&prioridad=&sector=`, `PUT /api/reportes/{id}/clasificacion`, `GET /api/reportes/{id}/posibles-duplicados`, `PUT/DELETE /api/reportes/{id}/duplicado`, `PUT /api/reportes/{id}/asignacion`, `/api/admin/operadores`, `/api/admin/rutas`, `/api/admin/puntos`, `/api/admin/contenidos`, `/api/admin/estadisticas/panel`, `/informe`, `/informe.csv` |
| Operador | `GET /api/operador/asignaciones`, `GET /api/operador/asignaciones/{id}`, `POST /api/operador/asignaciones/{id}/inicio`, `POST /api/operador/asignaciones/{id}/cierre` (multipart `descripcionTrabajo` + `evidencias`) |

Los errores se devuelven en formato *Problem Details* (`status`, `title`, `detail`).

### Despliegue en Railway

Aplicación publicada: **https://trabajo-final-dise-o-de-software-diagramas-production.up.railway.app**
(base de datos en MongoDB Atlas, clúster gratuito en AWS N. Virginia; servicio de Railway en US East).

1. En MongoDB Atlas: clúster gratuito, usuario de base de datos con permiso `readWrite` sobre `ecogestion` y
   acceso de red `0.0.0.0/0` (Railway no tiene IP fija).
2. Nuevo proyecto en Railway → *Deploy from GitHub repo* → este repositorio, con *Root Directory* `/backend`
   (Railway detecta Maven y Java 21 y construye el `.jar`).
3. En el servicio, definir `MONGO_URL` (cadena de Atlas), `MONGO_DATABASE=ecogestion`, `ADMIN_CORREO` y
   `ADMIN_CONTRASENA`.
4. *Settings → Networking → Generate Domain* (puerto 8080) y comprobar `https://<dominio>/actuator/health`.

## Regenerar los diagramas

Los diagramas se editan en Visual Paradigm (carpeta `visual-paradigm`). Las imágenes de contexto, funcional y
despliegue también tienen una fuente editable en draw.io:

```bash
draw.io --export --format png --scale 2 --border 20 --output docs/diagramas/01-vista-contexto.png docs/diagramas/01-vista-contexto.drawio
draw.io --export --format png --scale 2 --border 20 --output docs/diagramas/05-vista-despliegue.png docs/diagramas/05-vista-despliegue.drawio
draw.io --export --format png --scale 2 --border 20 --output docs/diagramas/02-vista-funcional.png docs/diagramas/02-vista-funcional.drawio
```

Los `.drawio` se generan con los scripts de [`docs/diagramas/generadores-drawio`](docs/diagramas/generadores-drawio).

La presentación se arma con [`docs/generar_presentacion.py`](docs/generar_presentacion.py) a partir de la plantilla del curso:

```bash
cd docs && python generar_presentacion.py plantilla.pptx diagramas EcoGestion-Diseno-de-arquitectura.pptx
```


## Historias de usuario como issues

`docs/historias/historias.py` es la fuente de las 15 historias; `crear_issues.py` crea las etiquetas y los
issues con `gh` sin duplicarlos:

```bash
cd docs/historias && python crear_issues.py samuelvallejo/trabajo-final-dise-o-de-software-diagramas
```
