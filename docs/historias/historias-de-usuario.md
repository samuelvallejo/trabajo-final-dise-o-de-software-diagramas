# EcoGestión – Historias de usuario

| N.º | Historia | Rol | Prioridad | Puntos | Issue |
|---|---|---|---|---|---|
| HU-01 | Registro e inicio de sesión del ciudadano | Ciudadano | Alta | 5 | [#1](https://github.com/samuelvallejo/trabajo-final-dise-o-de-software-diagramas/issues/1) |
| HU-02 | Consultar y actualizar datos personales | Ciudadano | Media | 3 | [#2](https://github.com/samuelvallejo/trabajo-final-dise-o-de-software-diagramas/issues/2) |
| HU-03 | Crear un reporte de residuos con fotos | Ciudadano | Alta | 5 | [#3](https://github.com/samuelvallejo/trabajo-final-dise-o-de-software-diagramas/issues/3) |
| HU-04 | Registrar la ubicación por GPS o en el mapa | Ciudadano | Alta | 5 | [#4](https://github.com/samuelvallejo/trabajo-final-dise-o-de-software-diagramas/issues/4) |
| HU-05 | Clasificar el reporte y asignarle prioridad | Administrador | Alta | 3 | [#5](https://github.com/samuelvallejo/trabajo-final-dise-o-de-software-diagramas/issues/5) |
| HU-06 | Detectar y vincular reportes duplicados | Administrador | Alta | 5 | [#6](https://github.com/samuelvallejo/trabajo-final-dise-o-de-software-diagramas/issues/6) |
| HU-07 | Asignar el reporte a un operador sin doble asignación | Administrador | Alta | 5 | [#7](https://github.com/samuelvallejo/trabajo-final-dise-o-de-software-diagramas/issues/7) |
| HU-08 | Consultar mis reportes asignados y su ubicación | Operador de limpieza | Alta | 3 | [#8](https://github.com/samuelvallejo/trabajo-final-dise-o-de-software-diagramas/issues/8) |
| HU-09 | Actualizar el estado y registrar el cierre con evidencias | Operador de limpieza | Alta | 8 | [#9](https://github.com/samuelvallejo/trabajo-final-dise-o-de-software-diagramas/issues/9) |
| HU-10 | Consultar el historial y estado de mis reportes | Ciudadano | Media | 3 | [#10](https://github.com/samuelvallejo/trabajo-final-dise-o-de-software-diagramas/issues/10) |
| HU-11 | Recibir notificaciones por correo ante cambios de estado | Ciudadano | Media | 5 | [#11](https://github.com/samuelvallejo/trabajo-final-dise-o-de-software-diagramas/issues/11) |
| HU-12 | Consultar y actualizar horarios y rutas de recolección por sector | Ciudadano, administrador | Media | 5 | [#12](https://github.com/samuelvallejo/trabajo-final-dise-o-de-software-diagramas/issues/12) |
| HU-13 | Consultar en el mapa los puntos de recolección autorizados | Ciudadano, administrador | Media | 5 | [#13](https://github.com/samuelvallejo/trabajo-final-dise-o-de-software-diagramas/issues/13) |
| HU-14 | Publicar y consultar contenido de educación ambiental | Administrador, ciudadano | Baja | 3 | [#14](https://github.com/samuelvallejo/trabajo-final-dise-o-de-software-diagramas/issues/14) |
| HU-15 | Panel administrativo, estadísticas e informes | Administrador | Media | 8 | [#15](https://github.com/samuelvallejo/trabajo-final-dise-o-de-software-diagramas/issues/15) |

**Total:** 71 puntos de historia.

---

# HU-01 – Registro e inicio de sesión del ciudadano

## Historia
Como **ciudadano**, quiero registrarme con mi nombre, documento de identificación, correo y contraseña e iniciar sesión, para poder reportar problemas de residuos y hacer seguimiento a mis reportes.

## Criterios de aceptación
1. **Dado** que no tengo cuenta, **cuando** envío el formulario con nombre, documento, correo y contraseña válidos, **entonces** se crea mi cuenta con rol Ciudadano y veo un mensaje de confirmación.
2. **Dado** que el documento o el correo ya están registrados, **cuando** intento registrarme, **entonces** el sistema rechaza el registro e indica qué dato está duplicado.
3. **Dado** que escribo una contraseña de menos de 8 caracteres o sin letras y números, **cuando** envío el formulario, **entonces** se muestra el error y no se crea la cuenta.
4. **Dado** que tengo una cuenta, **cuando** ingreso correo y contraseña correctos, **entonces** inicio sesión y accedo a mi panel de ciudadano.
5. **Dado** que ingreso credenciales incorrectas 5 veces seguidas, **cuando** intento de nuevo, **entonces** la cuenta se bloquea temporalmente 15 minutos.

## Prioridad
**Alta** — Sin cuenta el ciudadano no puede crear reportes ni consultarlos; bloquea el resto de historias.

## Estimación
**5 puntos de historia**

## Notas / reglas de negocio
- La contraseña se almacena con hash BCrypt en el atributo contrasenaHash de Ciudadano; nunca en texto plano.
- La sesión se gestiona con Spring Security (sesión HTTP + protección CSRF) en la aplicación Spring Boot; las vistas son Thymeleaf y funcionan en el navegador del PC o del celular.
- Los administradores y operadores no se auto-registran: los crea un administrador.

## Trazabilidad
Componente de la vista funcional: **Gestión de usuarios** (ver `docs/diagramas/02-vista-funcional.png`).


---

# HU-02 – Consultar y actualizar datos personales

## Historia
Como **ciudadano**, quiero consultar y actualizar mis datos personales, para mantener mi información de contacto correcta y recibir las notificaciones.

## Criterios de aceptación
1. **Dado** que inicié sesión, **cuando** abro "Mi perfil", **entonces** veo mi nombre, documento, correo y sector de residencia.
2. **Dado** que estoy en "Mi perfil", **cuando** cambio mi nombre, correo o sector y guardo, **entonces** los datos se actualizan y veo un mensaje de éxito.
3. **Dado** que cambio mi correo por uno que ya usa otra cuenta, **cuando** guardo, **entonces** el sistema rechaza el cambio.
4. **Dado** que quiero cambiar mi contraseña, **cuando** ingreso la actual correctamente y una nueva válida, **entonces** la contraseña se actualiza.

## Prioridad
**Media** — Importante para que las notificaciones lleguen al correo correcto, pero no bloquea el flujo de reportes.

## Estimación
**3 puntos de historia**

## Notas / reglas de negocio
- El documento de identificación no se puede modificar desde el perfil (solo un administrador).
- El sector de residencia se guarda en el atributo sector de Ciudadano y se usa para mostrar por defecto los horarios (HU-12) y los puntos de recolección (HU-13).

## Trazabilidad
Componente de la vista funcional: **Gestión de usuarios** (ver `docs/diagramas/02-vista-funcional.png`).


---

# HU-03 – Crear un reporte de residuos con fotos

## Historia
Como **ciudadano**, quiero reportar una acumulación de basura o residuos abandonados con una descripción, el tipo de residuo y fotografías, para que la entidad ambiental conozca el problema y lo atienda.

## Criterios de aceptación
1. **Dado** que inicié sesión, **cuando** completo descripción, tipo de residuo, al menos 1 foto y la ubicación y envío, **entonces** se crea el reporte en estado Pendiente con fecha y hora.
2. **Dado** que falta la descripción, el tipo de residuo o la foto, **cuando** intento enviar, **entonces** el sistema indica los campos obligatorios y no crea el reporte.
3. **Dado** que adjunto fotos, **cuando** alguna supera 5 MB o no es JPG/PNG, o adjunto más de 5, **entonces** se rechaza ese archivo con un mensaje claro.
4. **Dado** que el reporte se creó, **cuando** veo la confirmación, **entonces** se muestra su número de seguimiento.

## Prioridad
**Alta** — Es la funcionalidad central del sistema: sin reportes no hay nada que gestionar.

## Estimación
**5 puntos de historia**

## Notas / reglas de negocio
- Tipos de residuo iniciales: doméstico, escombros, reciclable, voluminoso, peligroso, otro.
- Las fotos se guardan en el almacenamiento de imágenes (Amazon S3) y en MongoDB solo sus URL, en el atributo fotos del Reporte.
- La ubicación se registra según HU-04.

## Trazabilidad
Componente de la vista funcional: **Gestión de reportes** (ver `docs/diagramas/02-vista-funcional.png`).


---

# HU-04 – Registrar la ubicación por GPS o en el mapa

## Historia
Como **ciudadano**, quiero registrar la ubicación del problema con el GPS de mi dispositivo o seleccionando un punto en el mapa, para indicar el lugar exacto aunque no esté allí en ese momento.

## Criterios de aceptación
1. **Dado** que concedo permiso de ubicación en el navegador, **cuando** elijo "Usar mi ubicación", **entonces** se capturan latitud y longitud del GPS (API de geolocalización del navegador) y se muestran en el mapa.
2. **Dado** que no estoy en el lugar o niego el permiso, **cuando** elijo "Seleccionar en el mapa" y toco un punto, **entonces** se guardan las coordenadas de ese punto.
3. **Dado** que se registró una ubicación, **cuando** el sistema la procesa, **entonces** obtiene la dirección aproximada (geocodificación) y el sector/barrio al que pertenece.
4. **Dado** que el punto seleccionado está fuera del municipio de Pasto, **cuando** intento enviar el reporte, **entonces** el sistema lo impide y lo informa.

## Prioridad
**Alta** — Sin ubicación precisa el operador no puede llegar al sitio ni se pueden identificar zonas afectadas.

## Estimación
**5 puntos de historia**

## Notas / reglas de negocio
- La ubicación se guarda en los atributos latitud, longitud, direccion y sector del Reporte.
- El mapa se muestra en la vista web con Maps JavaScript API; Gestión de reportes obtiene la dirección con la Geocoding API (Servicio de mapas/GPS) y el sector lo identifica Horarios y rutas ("Identificar sector").

## Trazabilidad
Componente de la vista funcional: **Gestión de reportes (usa el Servicio de mapas/GPS)** (ver `docs/diagramas/02-vista-funcional.png`).


---

# HU-05 – Clasificar el reporte y asignarle prioridad

## Historia
Como **administrador**, quiero revisar los reportes nuevos, clasificarlos por tipo de problema y asignarles una prioridad, para atender primero los casos más graves.

## Criterios de aceptación
1. **Dado** que hay reportes en estado Pendiente, **cuando** abro la bandeja de revisión, **entonces** los veo ordenados por prioridad sugerida y fecha.
2. **Dado** un reporte nuevo, **cuando** el sistema lo recibe, **entonces** le asigna una prioridad sugerida según el tipo de residuo (peligroso = Alta).
3. **Dado** que reviso un reporte, **cuando** cambio su tipo de problema o su prioridad (Alta, Media, Baja) y guardo, **entonces** el reporte queda clasificado con el nuevo tipo y la nueva prioridad.
4. **Dado** que filtro la bandeja por tipo, prioridad o sector, **cuando** aplico el filtro, **entonces** solo veo los reportes que cumplen el criterio.

## Prioridad
**Alta** — Permite atender primero los residuos peligrosos y ordenar el trabajo de los equipos.

## Estimación
**3 puntos de historia**

## Notas / reglas de negocio
- Regla: los residuos peligrosos siempre tienen prioridad Alta; el administrador no puede bajarla.
- La prioridad sugerida la calcula Clasificación y priorización a partir del tipo de residuo; se guarda en los atributos tipoResiduo y prioridad del Reporte.

## Trazabilidad
Componente de la vista funcional: **Clasificación y priorización** (ver `docs/diagramas/02-vista-funcional.png`).


---

# HU-06 – Detectar y vincular reportes duplicados

## Historia
Como **administrador**, quiero ver los reportes anteriores del mismo problema y vincular un duplicado a su reporte original, para que no se atienda dos veces el mismo problema.

## Criterios de aceptación
1. **Dado** que reviso un reporte, **cuando** existen reportes abiertos a menos de 50 m con el mismo tipo de residuo, **entonces** el sistema los muestra como posibles duplicados.
2. **Dado** un posible duplicado, **cuando** lo marco como duplicado de un reporte original, **entonces** queda vinculado a él y sale de la bandeja de pendientes.
3. **Dado** un reporte marcado como duplicado, **cuando** el original cambia de estado, **entonces** el duplicado refleja el mismo estado y su ciudadano también recibe la notificación.
4. **Dado** que me equivoqué, **cuando** desvinculo el duplicado, **entonces** vuelve a estado Pendiente.

## Prioridad
**Alta** — Evita que varios equipos se desplacen al mismo problema y desperdicien recursos.

## Estimación
**5 puntos de historia**

## Notas / reglas de negocio
- El vínculo se guarda en el atributo reporteOriginal del Reporte (asociación duplicadoDe). Un reporte solo puede ser duplicado de un original que no sea a su vez duplicado.
- Un reporte duplicado no puede asignarse a un operador.
- La búsqueda de cercanía usa una consulta geoespacial de MongoDB ($near) sobre un índice 2dsphere.

## Trazabilidad
Componente de la vista funcional: **Gestión de reportes** (ver `docs/diagramas/02-vista-funcional.png`).


---

# HU-07 – Asignar el reporte a un operador sin doble asignación

## Historia
Como **administrador**, quiero asignar cada reporte a un operador responsable, para que alguien lo atienda y no haya varios equipos sobre el mismo problema.

## Criterios de aceptación
1. **Dado** un reporte clasificado en estado Pendiente, **cuando** selecciono un operador disponible y confirmo, **entonces** el reporte queda con ese operador y pasa a Asignado.
2. **Dado** un reporte que ya tiene un operador asignado, **cuando** intento asignarlo a otro operador, **entonces** el sistema lo impide y muestra quién lo tiene asignado.
3. **Dado** que necesito cambiar de operador, **cuando** uso "Reasignar" sobre un reporte que no está Resuelto, **entonces** el reporte pasa al nuevo operador y nunca queda con dos.
4. **Dado** que elijo un operador, **cuando** veo la lista, **entonces** solo aparecen los operadores disponibles y cuántos reportes sin resolver tiene cada uno.

## Prioridad
**Alta** — Es el punto de control que garantiza que cada problema tenga un único responsable.

## Estimación
**5 puntos de historia**

## Notas / reglas de negocio
- Regla: como máximo un operador por reporte (el Reporte guarda un único operador y el campo version de MongoDB evita que dos asignaciones simultáneas se guarden a la vez).
- Solo se puede asignar a operadores con disponible = true (ReporteService.validarDisponibilidad).
- La asignación dispara la notificación "Reporte asignado" al ciudadano (HU-11).

## Trazabilidad
Componente de la vista funcional: **Asignación y atención** (ver `docs/diagramas/02-vista-funcional.png`).


---

# HU-08 – Consultar mis reportes asignados y su ubicación

## Historia
Como **operador de limpieza**, quiero ver los reportes que tengo asignados con su ubicación en el mapa, para planificar mi recorrido y llegar al lugar.

## Criterios de aceptación
1. **Dado** que inicié sesión como operador, **cuando** abro "Mis asignaciones", **entonces** veo solo los reportes asignados a mí que no están resueltos.
2. **Dado** la lista de asignaciones, **cuando** cambio a vista de mapa, **entonces** cada reporte aparece como un marcador con color según su prioridad.
3. **Dado** un reporte asignado, **cuando** abro su detalle, **entonces** veo descripción, tipo de residuo, prioridad, fotos, dirección y la opción "Cómo llegar".
4. **Dado** que tengo varias asignaciones, **cuando** ordeno la lista, **entonces** puedo ordenarla por prioridad o por fecha de asignación.

## Prioridad
**Alta** — Sin esta vista el operador no sabe qué atender ni dónde.

## Estimación
**3 puntos de historia**

## Notas / reglas de negocio
- Un operador no puede ver los reportes asignados a otros operadores.

## Trazabilidad
Componente de la vista funcional: **Asignación y atención (el mapa se muestra en la vista web con Maps JavaScript API)** (ver `docs/diagramas/02-vista-funcional.png`).


---

# HU-09 – Actualizar el estado y registrar el cierre con evidencias

## Historia
Como **operador de limpieza**, quiero actualizar el estado del reporte mientras lo atiendo y, al terminar, registrar el trabajo realizado con fotos de evidencia, para que la entidad y el ciudadano sepan que el problema fue resuelto.

## Criterios de aceptación
1. **Dado** un reporte en estado Asignado, **cuando** pulso "Iniciar atención", **entonces** pasa a En proceso y el ciudadano es notificado (HU-11).
2. **Dado** un reporte En proceso, **cuando** escribo la descripción del trabajo, adjunto de 1 a 5 fotos de evidencia y pulso "Marcar como resuelto", **entonces** pasa a Resuelto y se registra la fecha de resolución.
3. **Dado** que intento cerrar sin descripción del trabajo, **cuando** pulso "Marcar como resuelto", **entonces** el sistema no lo permite.
4. **Dado** cualquier reporte, **cuando** intento saltar estados (p. ej. de Asignado a Resuelto) o retroceder, **entonces** el sistema rechaza el cambio.

## Prioridad
**Alta** — Cierra el ciclo de atención y da trazabilidad del trabajo realizado.

## Estimación
**8 puntos de historia**

## Notas / reglas de negocio
- Flujo de estados obligatorio: Pendiente → Asignado → En proceso → Resuelto.
- La descripción y las URL de las fotos de evidencia (Amazon S3) se guardan en los atributos descripcionTrabajo y fotosEvidencia del Reporte (Reporte.registrarCierre).
- Cubre los ítems 9 (actualizar estado) y 10 (registrar cierre) del caso.

## Trazabilidad
Componente de la vista funcional: **Asignación y atención** (ver `docs/diagramas/02-vista-funcional.png`).


---

# HU-10 – Consultar el historial y estado de mis reportes

## Historia
Como **ciudadano**, quiero consultar el historial y el estado de mis reportes, para saber si ya fueron atendidos.

## Criterios de aceptación
1. **Dado** que inicié sesión, **cuando** abro "Mis reportes", **entonces** veo todos mis reportes con fecha, tipo de residuo y estado actual, del más reciente al más antiguo.
2. **Dado** la lista, **cuando** filtro por estado (Pendiente, Asignado, En proceso, Resuelto), **entonces** solo veo los que tienen ese estado.
3. **Dado** un reporte mío, **cuando** abro su detalle, **entonces** veo su estado actual, la fecha de reporte, el operador asignado y, si está resuelto, la fecha de resolución, la descripción del trabajo y las fotos de evidencia.
4. **Dado** un reporte marcado como duplicado, **cuando** lo consulto, **entonces** veo que está vinculado a otro reporte y el estado de ese original.

## Prioridad
**Media** — Da transparencia al ciudadano, pero el sistema funciona sin ella.

## Estimación
**3 puntos de historia**

## Notas / reglas de negocio
- Un ciudadano solo puede ver sus propios reportes (asociación historial Ciudadano 1 — 0..* Reporte).

## Trazabilidad
Componente de la vista funcional: **Gestión de reportes** (ver `docs/diagramas/02-vista-funcional.png`).


---

# HU-11 – Recibir notificaciones por correo ante cambios de estado

## Historia
Como **ciudadano**, quiero recibir una notificación por correo cuando mi reporte se asigna, empieza a atenderse o se resuelve, para enterarme sin tener que entrar al sistema.

## Criterios de aceptación
1. **Dado** un reporte mío, **cuando** pasa a Asignado, **entonces** recibo un correo con el número del reporte y el nuevo estado.
2. **Dado** un reporte mío, **cuando** pasa a En proceso o a Resuelto, **entonces** recibo el correo correspondiente (el de Resuelto incluye la descripción del trabajo).
3. **Dado** que el servidor de correo falla, **cuando** se intenta enviar, **entonces** la notificación queda registrada como no enviada y se reintenta hasta 3 veces.
4. **Dado** cada notificación, **cuando** se genera, **entonces** queda guardada en el historial de notificaciones del usuario.

## Prioridad
**Media** — Mejora la experiencia y la confianza del ciudadano; no bloquea la atención de reportes.

## Estimación
**5 puntos de historia**

## Notas / reglas de negocio
- Diseño extensible: Notificaciones envía a través de la interfaz requerida "Canal de notificación" (vista funcional); hoy se implementa con correo SMTP y se pueden agregar SMS o push sin modificar el componente (patrón Estrategia).
- El correo del destinatario se obtiene de Gestión de usuarios ("Obtener correo del ciudadano"). Las notificaciones de un reporte original también llegan a los ciudadanos de sus duplicados.

## Trazabilidad
Componente de la vista funcional: **Notificaciones** (ver `docs/diagramas/02-vista-funcional.png`).


---

# HU-12 – Consultar y actualizar horarios y rutas de recolección por sector

## Historia
Como **ciudadano**, quiero consultar los horarios y rutas de recolección de mi barrio o sector, para sacar la basura en el momento correcto; y como **administrador**, quiero actualizarlos, para que la información publicada esté siempre vigente.

## Criterios de aceptación
1. **Dado** que tengo un sector registrado, **cuando** abro "Horarios de recolección", **entonces** veo los días, horas y la ruta de mi sector.
2. **Dado** que quiero consultar otro sector o barrio, **cuando** lo busco por nombre, **entonces** veo sus horarios y su ruta en el mapa.
3. **Dado** que soy administrador, **cuando** creo o modifico una ruta, sus sectores o sus horarios y guardo, **entonces** los cambios se publican de inmediato.
4. **Dado** que el administrador ingresa una hora de fin anterior a la de inicio, **cuando** guarda, **entonces** el sistema rechaza el horario.

## Prioridad
**Media** — Ataca la causa del problema (desconocimiento de horarios), pero no es parte del flujo de reportes.

## Estimación
**5 puntos de historia**

## Notas / reglas de negocio
- Un sector agrupa varios barrios; una ruta puede cubrir varios sectores. La ruta se dibuja en la vista web con Maps JavaScript API.

## Trazabilidad
Componente de la vista funcional: **Horarios y rutas** (ver `docs/diagramas/02-vista-funcional.png`).


---

# HU-13 – Consultar en el mapa los puntos de recolección autorizados

## Historia
Como **ciudadano**, quiero ver en un mapa los puntos de recolección autorizados con los materiales que reciben y su horario, para llevar allí residuos especiales como electrónicos, pilas o reciclables.

## Criterios de aceptación
1. **Dado** que abro "Puntos de recolección", **cuando** carga el mapa, **entonces** veo un marcador por cada punto autorizado activo.
2. **Dado** el mapa, **cuando** filtro por material (electrónicos, pilas, reciclables, residuos especiales), **entonces** solo veo los puntos que reciben ese material.
3. **Dado** un punto, **cuando** lo selecciono, **entonces** veo nombre, dirección, materiales aceptados y horario de atención.
4. **Dado** que tengo un sector registrado, **cuando** abro el mapa, **entonces** se centra en mi sector y puedo filtrar los puntos por sector.
5. **Dado** que soy administrador, **cuando** registro, edito o desactivo un punto con su ubicación, materiales y horario, **entonces** el mapa público se actualiza.

## Prioridad
**Media** — Reduce la disposición inadecuada de residuos especiales, pero es informativa.

## Estimación
**5 puntos de historia**

## Notas / reglas de negocio
- Si el ciudadano comparte su ubicación, la lista se ordena por cercanía.

## Trazabilidad
Componente de la vista funcional: **Puntos de recolección (consulta el sector a Horarios y rutas; mapa con Maps JavaScript API)** (ver `docs/diagramas/02-vista-funcional.png`).


---

# HU-14 – Publicar y consultar contenido de educación ambiental

## Historia
Como **administrador**, quiero publicar y actualizar contenidos sobre separación de residuos, reciclaje, reducción de basura y buenas prácticas, para que los ciudadanos aprendan a manejar sus residuos; y como **ciudadano**, quiero consultarlos.

## Criterios de aceptación
1. **Dado** que soy administrador, **cuando** creo un contenido con título, categoría, texto e imagen opcional y lo publico, **entonces** queda visible para los ciudadanos.
2. **Dado** un contenido publicado, **cuando** lo edito o lo despublico, **entonces** los cambios se reflejan de inmediato.
3. **Dado** que soy ciudadano, **cuando** abro "Educación ambiental", **entonces** veo los contenidos publicados agrupados por categoría y puedo buscarlos por palabra clave.

## Prioridad
**Baja** — Aporta a la prevención a largo plazo, pero no afecta la operación diaria.

## Estimación
**3 puntos de historia**

## Notas / reglas de negocio
- Categorías: separación de residuos, reciclaje, reducción de basura, buenas prácticas.
- Los borradores no son visibles para los ciudadanos. Publicar exige rol de administrador ("Validar rol de administrador" en Gestión de usuarios).

## Trazabilidad
Componente de la vista funcional: **Contenido educativo** (ver `docs/diagramas/02-vista-funcional.png`).


---

# HU-15 – Panel administrativo, estadísticas e informes

## Historia
Como **administrador**, quiero un panel con indicadores de los reportes y generar informes por periodo y sector, para evaluar la atención, identificar las zonas que necesitan más intervención y planificar las limpiezas.

## Criterios de aceptación
1. **Dado** que inicio sesión como administrador, **cuando** abro el panel, **entonces** veo el total de reportes, los pendientes, los resueltos y los problemas (tipos de residuo) más frecuentes.
2. **Dado** el panel, **cuando** consulto la sección de sectores, **entonces** veo un ranking y un mapa de calor de los sectores con mayor acumulación de reportes.
3. **Dado** que selecciono un rango de fechas y opcionalmente un sector, **cuando** genero el informe, **entonces** obtengo los indicadores de ese periodo, incluido el tiempo promedio de atención.
4. **Dado** un informe generado, **cuando** pulso "Exportar", **entonces** se descarga en PDF o CSV.

## Prioridad
**Media** — Es la base para la toma de decisiones de la entidad, pero requiere que ya existan reportes.

## Estimación
**8 puntos de historia**

## Notas / reglas de negocio
- Tiempo de atención = fecha de paso a Resuelto − fecha de creación del reporte.
- Los reportes duplicados no se cuentan dos veces en los indicadores. Los datos salen de Gestión de reportes ("Consultar reportes") y de Clasificación y priorización ("Consultar tipos y prioridades").

## Trazabilidad
Componente de la vista funcional: **Estadísticas e informes** (ver `docs/diagramas/02-vista-funcional.png`).

