# EcoGestión – Diseño de arquitectura

Sistema para reportar y gestionar la disposición inadecuada de residuos sólidos en Pasto (Nariño):
reportes ciudadanos con fotos y ubicación, clasificación y priorización, asignación a operadores de
limpieza, seguimiento, notificaciones, horarios y rutas de recolección, puntos de recolección,
educación ambiental y estadísticas.

## Contenido

Los 5 diagramas están modelados en **Visual Paradigm 18.1**, en un solo proyecto:
[`docs/diagramas/visual-paradigm/EcoGestion.vpp`](docs/diagramas/visual-paradigm/EcoGestion.vpp)
(ábrelo con *File > Open Project*; cada vista está en *Diagram Navigator* y sus elementos UML en *Model Explorer*).

Las imágenes de la presentación se generan con PlantUML y draw.io a partir de las fuentes editables de
esta carpeta, porque la copia de Visual Paradigm usada es de evaluación y marca todas sus exportaciones con
una marca de agua. Con una licencia activada, cada diagrama del `.vpp` se puede exportar directamente
(*Export > Active Diagram as Image*).

| Vista | Diagrama en el proyecto VP | Fuente editable de la imagen | Imagen |
|---|---|---|---|
| Contexto (DFD nivel 0) | 1. Vista de contexto | [01-vista-contexto.drawio](docs/diagramas/01-vista-contexto.drawio) | [PNG](docs/diagramas/01-vista-contexto.png) |
| Funcional (componentes UML) | 2. Vista funcional | [02-vista-funcional.drawio](docs/diagramas/02-vista-funcional.drawio) | [PNG](docs/diagramas/02-vista-funcional.png) |
| Lógica (modelo de clases inicial) | 3. Vista lógica – Modelo de clases inicial | [03-vista-logica.puml](docs/diagramas/03-vista-logica.puml) | [PNG](docs/diagramas/03-vista-logica.png) |
| Desarrollo (clases por capas controller → service → repository → model) | 4. Vista de desarrollo – Clases por capas | [04-vista-desarrollo.puml](docs/diagramas/04-vista-desarrollo.puml) | [PNG](docs/diagramas/04-vista-desarrollo.png) |
| Despliegue (despliegue UML) | 5. Vista de despliegue | [05-vista-despliegue.drawio](docs/diagramas/05-vista-despliegue.drawio) | [PNG](docs/diagramas/05-vista-despliegue.png) |

- Presentación: [EcoGestion-Diseno-de-arquitectura.pptx](docs/EcoGestion-Diseno-de-arquitectura.pptx)
- Historias de usuario: [historias-de-usuario.md](docs/historias/historias-de-usuario.md) (también publicadas como issues)
- Matriz de trazabilidad caso de estudio → historias → diagramas: [trazabilidad.md](docs/historias/trazabilidad.md)

## Stack tecnológico

| Capa | Tecnología |
|---|---|
| Aplicación (backend y vistas) | **Java 21 + Spring Boot 4**: Spring MVC (API REST), frontend HTML con plantillas **Thymeleaf** y JavaScript sin librerías, Spring Security, Spring Data MongoDB, Bean Validation. Sin React, Angular ni Node. |
| Clientes | Navegador web del PC o del celular (el GPS se toma con la API de geolocalización del navegador) |
| Base de datos | **MongoDB** (colecciones por agregado e índices `2dsphere` para consultas por cercanía) |
| Almacenamiento de imágenes | MongoDB **GridFS** (en la misma base de datos) |
| Despliegue | **Railway**: servicio web con el backend Spring Boot + servicio MongoDB |
| Correo | Servidor SMTP (relay) |
| Mapas y geocodificación | Google Maps Platform (Maps JavaScript API + Geocoding API) |

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
├── integracion  interfaces y estrategias: CanalNotificacion (correo/registro), ServicioMapas (Google/coordenadas),
│                AlmacenamientoImagenes (GridFS)
├── security     autenticación con Spring Security (sesión HTTP, BCrypt, bloqueo tras 5 intentos)
├── config       seguridad, propiedades y datos iniciales (sectores, rutas, puntos y contenido de Pasto)
└── dto          registros de entrada y salida de la API

backend/src/main/resources
├── templates    páginas HTML (Thymeleaf): index, ciudadano, administrador, operador y fragmentos comunes
└── static       css/estilos.css y js/ (api.js con las funciones comunes + un archivo por página)
```

Cada clase tiene un comentario que explica qué hace, cómo funciona y qué patrón GRASP aplica y por qué.
Resumen:

| Patrón GRASP | Dónde se aplica | Por qué |
|---|---|---|
| Controlador | `controller/*Controller` | Reciben las peticiones HTTP y delegan en los servicios; no tienen reglas de negocio |
| Experto en información | `Reporte`, `Usuario`, `Operador`, `EstadoReporte`, `TipoResiduo`, `Sector`, `PuntoRecoleccion`... | Cada regla está en la clase que tiene los datos (por ejemplo, `Reporte.asignar` impide la doble asignación) |
| Creador | `ReporteService` crea `Reporte`, `UsuarioService` crea usuarios, `NotificacionService` crea `Notificacion` | Quien tiene los datos de inicialización crea el objeto |
| Alta cohesión | Un servicio por componente de la vista funcional | Cada clase tiene un solo tema (reportes, usuarios, notificaciones...) |
| Bajo acoplamiento | DTOs en la API, servicios que usan servicios de otros componentes y no sus repositorios | Un cambio interno no se propaga al resto |
| Polimorfismo | `Usuario.getRol()`, `CanalNotificacion`, `ServicioMapas`, `AlmacenamientoImagenes` | Se cambia el comportamiento sin if/else por tipo |
| Fabricación pura | Servicios, repositorios, `ManejadorErrores`, `ValidadorFotos`, `Geo` | Clases técnicas que no existen en el caso de estudio pero mantienen limpio el dominio |
| Indirección | Repositorios, `CanalNotificacion`, `CuentaUsuario`, `EventosAutenticacion` | Un intermediario desacopla dos partes (servicio ↔ MongoDB, servicio ↔ SMTP, dominio ↔ Spring Security) |
| Variaciones protegidas | Interfaces de `integracion` y `EcoGestionProperties` | Cambiar de proveedor de correo, mapas o almacenamiento, o un parámetro, no obliga a tocar la lógica |

**Encapsulamiento (sin get/set casuales).** Las clases del modelo no tienen setters: cada cambio pasa por un
método que expresa la acción y valida su regla (`Reporte.asignar`, `Reporte.registrarCierre`,
`Ciudadano.actualizarPerfil`, `Usuario.cambiarContrasena`, `Operador.marcarOcupado`), y `Reporte.cambiarEstado`
es privado para que nadie se salte el flujo de estados. Spring Data guarda y lee los atributos por reflexión,
así que no necesita setters. Solo existen los getters que alguien usa (para armar las respuestas de la API, las
notificaciones o las estadísticas); son de solo lectura y las listas se devuelven inmodificables. El hash de la
contraseña solo lo lee Spring Security y nunca sale en la API. Las vistas lógica y de desarrollo muestran esta
regla con una nota y separan las "consultas de solo lectura".

### Frontend

| Página | Rol | Qué permite |
|---|---|---|
| `/` | Público | Iniciar sesión, registrarse, consultar horarios por sector o barrio, puntos de recolección y educación ambiental |
| `/ciudadano` | Ciudadano | Crear reportes con GPS o coordenadas y fotos, ver su historial y detalle, notificaciones, horarios y puntos de su sector, perfil y contraseña |
| `/administrador` | Administrador | Panel, bandeja con filtros, clasificar, revisar duplicados, asignar operadores, crear operadores, informe y CSV, contenido educativo y puntos |
| `/operador` | Operador | Ver sus reportes asignados, iniciar la atención y cerrarlos con fotos de evidencia |

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
| `MONGO_URL` | Cadena de conexión de MongoDB (en Railway: `${{MongoDB.MONGO_URL}}`) |
| `MONGO_DATABASE` | Nombre de la base de datos (por defecto `ecogestion`) |
| `ADMIN_CORREO`, `ADMIN_CONTRASENA` | Cuenta del primer administrador, que se crea al iniciar |
| `DATOS_DEMO` | `true` (por defecto) carga sectores, rutas, puntos y contenido de ejemplo de Pasto |
| `GOOGLE_MAPS_API_KEY` | Opcional: geocodificación con Google; sin ella la dirección se arma con las coordenadas |
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

1. Nuevo proyecto en Railway → *Deploy from GitHub repo* → este repositorio, con *Root Directory* `backend`
   (Railway detecta Maven y Java 21 y construye el `.jar`).
2. Agregar un servicio **MongoDB** al proyecto.
3. En el servicio web, definir `MONGO_URL=${{MongoDB.MONGO_URL}}`, `MONGO_DATABASE=ecogestion`,
   `ADMIN_CORREO` y `ADMIN_CONTRASENA`.
4. *Settings → Networking → Generate Domain* y comprobar `https://<dominio>/actuator/health`.

## Regenerar los diagramas

Imágenes (PlantUML necesita Java y Graphviz; draw.io desktop para los `.drawio`):

```bash
java -jar plantuml.jar -charset UTF-8 -tpng docs/diagramas/*.puml
draw.io --export --format png --scale 2 --border 20 --output docs/diagramas/01-vista-contexto.png docs/diagramas/01-vista-contexto.drawio
draw.io --export --format png --scale 2 --border 20 --output docs/diagramas/05-vista-despliegue.png docs/diagramas/05-vista-despliegue.drawio
draw.io --export --format png --scale 2 --border 20 --output docs/diagramas/02-vista-funcional.png docs/diagramas/02-vista-funcional.drawio
```

Los `.drawio` se generan con los scripts de [`docs/diagramas/generadores-drawio`](docs/diagramas/generadores-drawio).

La presentación se arma con [`docs/generar_presentacion.py`](docs/generar_presentacion.py) a partir de la plantilla del curso:

```bash
cd docs && python generar_presentacion.py plantilla.pptx diagramas EcoGestion-Diseno-de-arquitectura.pptx
```

El proyecto de Visual Paradigm se construye con un plugin de la Open API de Visual Paradigm
([`docs/diagramas/visual-paradigm/generador`](docs/diagramas/visual-paradigm/generador)): crea los
elementos UML, los ubica, guarda el `.vpp` y exporta los PNG.

```bash
javac --release 11 -cp "C:/Program Files/Visual Paradigm 18.1/lib/openapi.jar" -d classes src/ecogestion/vp/EcoGestionVP.java
# copiar plugin.xml y classes/ a %APPDATA%\VisualParadigm\plugins\ecogestion.vp\ y luego, desde "Visual Paradigm 18.1\scripts":
Plugin.bat -project semilla.vpp -pluginid ecogestion.vp -pluginargs "salida.vpp carpetaImagenes carpetaLayouts"
```

## Historias de usuario como issues

`docs/historias/historias.py` es la fuente de las 15 historias; `crear_issues.py` crea las etiquetas y los
issues con `gh` sin duplicarlos:

```bash
cd docs/historias && python crear_issues.py samuelvallejo/trabajo-final-dise-o-de-software-diagramas
```
