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
| Aplicación (backend y vistas) | **Java 21 + Spring Boot**: Spring MVC, vistas **Thymeleaf**, Spring Security, Spring Data MongoDB. Sin React ni Angular. |
| Clientes | Navegador web del PC o del celular (el GPS se toma con la API de geolocalización del navegador) |
| Base de datos | **MongoDB 7** (colecciones por agregado e índice `2dsphere` para consultas por cercanía) |
| Almacenamiento de imágenes | Amazon S3 (bucket `ecogestion-fotos`) |
| Correo | Servidor SMTP (relay) |
| Mapas y geocodificación | Google Maps Platform (Maps JavaScript API + Geocoding API) |

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
