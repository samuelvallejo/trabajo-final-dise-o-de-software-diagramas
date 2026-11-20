# EcoGestión – Diseño de arquitectura

Sistema para reportar y gestionar la disposición inadecuada de residuos sólidos en Pasto (Nariño):
reportes ciudadanos con fotos y ubicación, clasificación y priorización, asignación a operadores de
limpieza, seguimiento, notificaciones, horarios y rutas de recolección, puntos de recolección,
educación ambiental y estadísticas.

## Contenido

| Entregable | Archivo fuente (editable) | Imagen |
|---|---|---|
| Vista de contexto (DFD nivel 0) | [01-vista-contexto.drawio](docs/diagramas/01-vista-contexto.drawio) | [PNG](docs/diagramas/01-vista-contexto.png) |
| Vista funcional (componentes UML) | [02-vista-funcional.puml](docs/diagramas/02-vista-funcional.puml) | [PNG](docs/diagramas/02-vista-funcional.png) |
| Vista lógica (clases UML) | [03-vista-logica.puml](docs/diagramas/03-vista-logica.puml) | [PNG](docs/diagramas/03-vista-logica.png) |
| Vista de desarrollo (paquetes y dependencias) | [04-vista-desarrollo.puml](docs/diagramas/04-vista-desarrollo.puml) | [PNG](docs/diagramas/04-vista-desarrollo.png) |
| Vista de despliegue (despliegue UML) | [05-vista-despliegue.puml](docs/diagramas/05-vista-despliegue.puml) | [PNG](docs/diagramas/05-vista-despliegue.png) |
| Presentación | [EcoGestion-Diseno-de-arquitectura.pptx](docs/EcoGestion-Diseno-de-arquitectura.pptx) | |
| Historias de usuario | [historias-de-usuario.md](docs/historias/historias-de-usuario.md) (también como issues) | |

## Stack tecnológico supuesto

| Capa | Tecnología |
|---|---|
| Cliente web (ciudadano y administrador) | React (SPA) servida por Nginx |
| App móvil (ciudadano y operador, con GPS) | React Native (Android / iOS) |
| API REST | Java 21 + Spring Boot (Spring Web, Spring Security con JWT, Spring Data JPA) |
| Base de datos | PostgreSQL 16 + PostGIS (consultas por distancia para duplicados y mapa de calor) |
| Almacenamiento de imágenes | Amazon S3 (bucket `ecogestion-fotos`) |
| Correo | Servidor SMTP (relay, p. ej. SendGrid) |
| Mapas y geocodificación | Google Maps Platform (Maps SDK + Geocoding API) |

## Regenerar los diagramas

```bash
# PlantUML (requiere Java y Graphviz)
java -jar plantuml.jar -charset UTF-8 -tpng docs/diagramas/*.puml
# draw.io desktop
draw.io --export --format png --scale 2 --border 20 --output docs/diagramas/01-vista-contexto.png docs/diagramas/01-vista-contexto.drawio
```

## Historias de usuario como issues

`docs/historias/historias.py` es la fuente de las 15 historias; `crear_issues.py` crea las etiquetas y los
issues con `gh` sin duplicarlos:

```bash
cd docs/historias && python crear_issues.py samuelvallejo/trabajo-final-dise-o-de-software-diagramas
```
