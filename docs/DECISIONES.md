# Decisiones de arquitectura (ADR)

> Registro corto de decisiones importantes. Una decisión nueva **no borra** la anterior: la marca como "Reemplazada por ADR-XXX".
> Plantilla al final.

## ADR-001 · Nombre de trabajo "Cascada" — Reemplazada por ADR-011 (2026-10-23)
**Contexto:** se necesita un nombre para paquete y docs. **Decisión:** "Cascada", paquete `app.cascada.launcher`. Es provisional; el nombre comercial se decide en Fase 5. **Consecuencia:** cambiar el `applicationId` después de publicar es imposible; decidir antes del primer release público.

## ADR-002 · Kotlin + Jetpack Compose nativo — Aceptada
**Contexto:** se busca máxima fluidez y acceso directo a APIs de launcher (`LauncherApps`, widgets, notificaciones, perfiles). **Opciones:** Flutter, React Native, Views, Compose. **Decisión:** Compose. **Por qué:** acceso nativo sin puentes; Compose iguala a Views en jank de scroll desde 1.9; animaciones por resortes de primera clase; Baseline Profiles. Flutter/RN agregan peso y capas para APIs de sistema que un launcher usa todo el tiempo.

## ADR-003 · Una sola Activity — Aceptada
**Decisión:** `HomeActivity` única para inicio, lista, búsqueda y pop-ups; ajustes en `SettingsActivity` aparte. **Por qué:** navegación instantánea sin transiciones de Activity; estado compartido simple.

## ADR-004 · DI manual — Aceptada
**Decisión:** `AppContainer` con `lazy`. **Por qué:** cero costo de arranque y menos complejidad para un proyecto de una persona. **Revisar** si el grafo supera ~25 dependencias.

## ADR-005 · Room para caché de apps + DataStore Proto para preferencias — Aceptada (la parte de DataStore Proto fue reemplazada por ADR-017)
**Por qué:** primer frame inmediato desde caché; favoritos y ocultos son relacionales y reordenables; preferencias tipadas.

## ADR-006 · Sin `QUERY_ALL_PACKAGES` ni `INTERNET` — Aceptada
**Por qué:** `LauncherApps` + `<queries>` cubren todo; Play restringe el permiso; sin red no hay riesgo de fuga y el formulario de datos es simple. Agregar `INTERNET` requiere ADR propio (ej. clima).

## ADR-007 · minSdk 26 / targetSdk 36 — Aceptada
**Por qué:** 26 = íconos adaptativos y APIs modernas de `LauncherApps`; 36 = requisito de Play desde 31 ago 2026.
_`compileSdk` actualizado a 37 por ADR-013; `minSdk` y `targetSdk` siguen vigentes._

## ADR-008 · Alfabeto ola dibujado en un único Canvas — Aceptada
**Por qué:** un composable por letra con estado propio recompondría ~27 nodos por frame de arrastre. Leer el offset del dedo en la fase de dibujo da 0 recomposiciones.

## ADR-009 · Accesibilidad solo para bloquear pantalla — Aceptada
**Por qué:** es el único método limpio sin root (API 28+); Play exige divulgación; menor superficie de riesgo.

## ADR-010 · Código propietario, sin copiar GPL — Aceptada
**Por qué:** mantener libertad de licencia para una posible versión Pro. Proyectos GPL solo como referencia de lectura.

## ADR-011 · Nombre "Velora" — Aceptada (2026-10-23)
**Contexto:** el nombre de trabajo "Cascada" (ADR-001) se cambia antes de escribir código, cuando todavía es gratis cambiarlo.
**Decisión:** el producto se llama **Velora**; paquete y `applicationId` `app.velora.launcher` (debug: `app.velora.launcher.debug`). Logo base: `docs/marca/logo-velora.jpg` (cinta plateada sobre negro), usado para el ícono adaptativo.
**Por qué:** decisión del autor. Cambiarlo ahora no cuesta nada; después del primer release el `applicationId` es permanente.
**Consecuencias:** todos los docs, clases (`VeloraApplication`, `VeloraApp`, `Theme.Velora.*`) y recursos usan "Velora". Antes del release (Fase 5) se verifica que el nombre esté libre en Play y como marca.

## ADR-012 · detekt y ktlint como CLI desde Gradle, sin sus plugins — Aceptada (2026-10-23)
**Contexto:** el plugin de Gradle de detekt 1.23 usa la API antigua de variantes de AGP, que AGP 9 eliminó, y exige que la versión de Kotlin del build coincida con la suya (2.0.x). Hay detekt 2.0, pero solo en alpha.
**Opciones:** A) detekt 2.0 alpha (requiere ADR por ser alpha, API inestable). B) plugins de detekt/ktlint con parches de versión. C) Ejecutar `detekt-cli` y `ktlint-cli` estables como tareas `JavaExec` con su propia configuración de dependencias.
**Decisión:** C. Las tareas `detekt`, `ktlintCheck` y `ktlintFormat` están en el `build.gradle.kts` raíz.
**Por qué:** cero acoplamiento con AGP y con la versión de Kotlin del proyecto; versiones estables; los comandos de `CLAUDE.md` siguen igual.
**Consecuencias:** detekt corre sin resolución de tipos (las reglas "type resolution" no aplican). Revisar al salir detekt 2.0 estable.

## ADR-013 · `compileSdk 37` con `targetSdk 36` — Aceptada (2026-10-23)
**Contexto:** las últimas estables de AndroidX (Core 1.19, Compose 1.12 / BOM 2026.09.00, Lifecycle 2.11) exigen compilar contra API 37.
**Opciones:** A) bajar todas esas librerías a versiones anteriores. B) subir solo `compileSdk`.
**Decisión:** B. `compileSdk 37`, `targetSdk 36`, `minSdk 26`, definidos una sola vez en `gradle/libs.versions.toml`.
**Por qué:** `compileSdk` solo cambia qué APIs se pueden *referenciar*; el comportamiento en ejecución lo fija `targetSdk`. Usar una API de 37 sigue requiriendo `Build.VERSION.SDK_INT` (REGLAS §9).
**Consecuencias:** subir `targetSdk` a 37 es una decisión aparte (cambios de comportamiento de Android 17), con su propio ADR.

## ADR-014 · El ícono abre el inicio; los ajustes son internos — Aceptada (2026-10-23)
**Contexto:** con P-02 original, el único ícono en el cajón de otros launchers era "Ajustes de Velora"; el autor lo tocó esperando ver el inicio.
**Opciones:** A) dos íconos (Velora + Ajustes). B) Un ícono que abre `HomeActivity`; ajustes por pulsación larga.
**Decisión:** B. `HomeActivity` tiene también `MAIN/LAUNCHER`; `SettingsActivity` queda `exported="false"`, sin `LAUNCHER`.
**Por qué:** un solo punto de entrada, claro. Abrir el inicio sin ser predeterminado muestra la landing (PENDIENTES §1), que ofrece hacerlo predeterminado. Un componente exportado menos (SEGURIDAD §3.5).
**Consecuencias:** Velora se excluye de su propia lista de apps. Si los ajustes crecen, considerar un acceso visible en el inicio.

## ADR-015 · Fuente Manrope empaquetada — Reemplazada por ADR-016 (2026-10-24)
**Contexto:** el autor pidió una fuente "elegante, bonita". ESTILO §3 decía "fuente del sistema; propia opcional y descargable en Fase 3".
**Opciones:** A) fuente del sistema. B) Manrope descargable (Google Fonts provider). C) Manrope empaquetada.
**Decisión:** C. Manrope variable (eje `wght` 200–800), recortada a latino con `fontTools` (~87 KB), en `core/designsystem/src/main/res/font/manrope.ttf`. Licencia SIL OFL 1.1 (`docs/licencias/OFL-Manrope.txt`, `THIRD_PARTY_NOTICES.md`).
**Por qué:** una fuente descargable llega después del primer frame y el texto "salta": inaceptable en un launcher. Una sola fuente variable cubre todos los pesos. Pesa poco frente al presupuesto de 8 MB.
**Consecuencias:** idiomas no latinos (cirílico, griego, CJK) caen a la fuente del sistema. Pendiente: opción "Fuente del sistema" en Ajustes.

## ADR-016 · Varias fuentes empaquetadas, elegibles en Ajustes → Tema — Aceptada (2026-10-24)
**Contexto:** probada en el teléfono, Manrope se sintió "forzada, robótica" para el autor. El gusto tipográfico es personal, y los launchers de referencia ofrecen temas con fuentes.
**Decisión:** cinco opciones: **Plus Jakarta Sans** (por defecto: moderna y suave), **Figtree** (amable), **Fraunces** (serif con carácter; ejes SOFT/WONK/opsz fijados con `fontTools.varLib.instancer`, solo queda `wght`), **Quicksand** (redondeada) y **Fuente del sistema**. Todas variables, empaquetadas en `core/designsystem/src/main/res/font/` y, salvo Quicksand, recortadas a latino.
**Por qué:** empaquetadas, por la misma razón que ADR-015 (sin "salto" en el primer frame). Quicksand declara un *Reserved Font Name* en su OFL: una versión modificada (y recortar ya es modificar) no puede llamarse igual, por eso va **sin modificar**.
**Consecuencias:** +~400 KB de APK, dentro del presupuesto de 8 MB. Licencias en `docs/licencias/`. Manrope se elimina.

## ADR-017 · DataStore Preferences en vez de DataStore Proto — Aceptada (2026-10-24)
**Contexto:** ADR-005 pedía DataStore Proto. Proto exige el plugin de protobuf y `protoc` en el build, para unas pocas preferencias.
**Opciones:** A) Proto + plugin de protobuf. B) Preferences detrás de un repositorio tipado (`PrefsRepository`) que guarda nombres de enums.
**Decisión:** B, con `androidx.datastore:datastore-preferences` 1.2.1 (la 1.3.0 tenía menos de dos semanas).
**Por qué:** mismo modelo asíncrono y seguro, sin generación de código. El tipado lo da `PrefsRepository`; un valor desconocido vuelve al de fábrica (`decodeTheme`, con prueba).
**Consecuencias:** si las preferencias crecen mucho o necesitan estructuras anidadas, reconsiderar Proto con un ADR.

## ADR-018 · Temas completos con fondo dibujado por Velora — Aceptada (2026-10-25)
**Contexto:** el autor quiere temas como los de las capturas de referencia: fondo, colores, fuente e íconos que combinan.
**Opciones:** A) cambiar el wallpaper del sistema (`SET_WALLPAPER`, permiso nuevo y cambio que el usuario no pidió fuera del launcher). B) imágenes de fondo empaquetadas (pesan MB). C) fondos dibujados en Compose dentro del inicio.
**Decisión:** C. `ThemePreset` (Tu fondo, Sakura, Bosque, Noche pixel, Aurora). Cada tema define su fondo (`ThemeBackground`, formas calculadas una vez con `drawWithCache`, sin animación), sus colores sobre el fondo (`PresetColors`, texto, acento, velos, sombra, claro/oscuro) y una fuente, reloj e íconos recomendados que se aplican al elegirlo (`applyPreset`) y luego se pueden cambiar.
**Por qué:** cero permisos y cero MB: todo el arte pesa unas decenas de KB de código. El wallpaper del usuario no se toca y "Tu fondo" sigue disponible. Diseños propios, no copias de las capturas.
**Consecuencias:** las barras del sistema usan íconos oscuros en temas claros. Fuente pixel nueva: Silkscreen (OFL, sin nombre reservado, sin modificar). Agregar un tema = colores en `Color.kt` + una función de dibujo + su recomendación.

## ADR-019 · Íconos de Material (`material-icons-core`) — Aceptada (2026-10-26)
**Contexto:** el autor pidió ajustes y menús como los de su referencia: cada opción con su ícono.
**Opciones:** A) dibujar íconos a mano. B) Vectores XML copiados de Material Symbols. C) `androidx.compose.material:material-icons-core` (Apache-2.0, ya en la BOM 2026.09.00, versión 1.7.8).
**Decisión:** C, solo el paquete *core* (nunca *extended*, que pesa varios MB).
**Por qué:** íconos consistentes y accesibles sin mantener vectores. R8 quita todos los que no se usan.
**Consecuencias:** si falta un ícono que solo existe en *extended*, se copia ese vector suelto con su licencia, no se agrega el paquete entero.

## ADR-020 · Widgets en el inicio — Aceptada (2026-10-26)
**Contexto:** el autor pidió widgets (estaban en la Fase 3).
**Decisión:** un `AppWidgetHost` por proceso (`WidgetHost`, id 1024). Mantener presionado el inicio → "Agregar widget" → selector propio con vistas previas (`installedProviders`) → `bindAppWidgetIdIfAllowed` o el diálogo del sistema `ACTION_APPWIDGET_BIND`, la primera vez por proveedor → configuración del widget, si la tiene. Lista y altos en Room (`widgets`, versión 3, migración automática). Se dibujan con `AndroidView` entre el reloj y los favoritos. En Ajustes → Widgets se cambia el alto o se quitan.
**Por qué:** cero permisos en el manifiesto (el sistema pide el consentimiento por proveedor). Escuchar solo con el inicio visible (`onStart`/`onStop`) cuida la batería. Los ids huérfanos se liberan al empezar (B-019).
**Consecuencias:** los resultados del permiso y de la configuración llegan por `onActivityResult` (API clásica, marcada como obsoleta). Desde 0.6.0 se reordenan con flechas en Ajustes (`WidgetsDao.setPosition`). Pendiente: redimensionar arrastrando.

## ADR-021 · Íconos minimalistas con glifos propios (Tabler Icons) — Aceptada (2026-10-26)
**Contexto:** con el estilo minimalista, las apps sin ícono monocromo (la mayoría) quedaban con su logo a color o en gris. El autor quería todas iguales: círculo claro y un dibujo de línea encima, también para apps que no traen ese dibujo.
**Opciones:** A) solo el ícono monocromo de Android 13 (la mitad de las apps no lo tiene). B) Dibujar a mano un set de íconos (semanas). C) Un set de íconos de línea con licencia libre + un emparejador por paquete, palabra clave y categoría.
**Decisión:** C con Tabler Icons 3.49.0 (MIT). `tools/icons/build_glyphs.py` descarga solo los glifos que usamos (~130) y los guarda como datos de trazo en `core/data/src/main/assets/glyphs/tabler.tsv` (≈60 KB). `GlyphMatcher` elige el glifo: paquete conocido → palabra clave del nombre o paquete (español e inglés) → categoría de la app. `IconRenderer` dibuja círculo + glifo (trazo 2, 55 %); si no hay glifo usa el monocromo de la app, y si tampoco, la inicial.
**Por qué:** cero dependencias en tiempo de ejecución (solo `PathParser` de androidx.core), resultado uniforme y propio, sin copiar los íconos de ningún launcher. Lo que no se reconoce queda con su inicial, nunca con un dibujo equivocado.
**Consecuencias:** agregar un glifo = añadirlo al script, regenerar y mapearlo en `GlyphMatcher` (con prueba). Aviso MIT en `docs/licencias/MIT-TablerIcons.txt` y `THIRD_PARTY_NOTICES.md`. Las marcas (`brand-*`) se usan solo como pictogramas genéricos de Tabler.

## ADR-022 · Foto propia como fondo de un tema — Aceptada (2026-10-26)
**Contexto:** el autor pidió poder cambiar el fondo de los temas.
**Opciones:** A) `READ_MEDIA_IMAGES` y una galería propia (permiso nuevo). B) El selector de fotos del sistema (`PickVisualMedia`), sin permisos. C) Cambiar el wallpaper del sistema (`SET_WALLPAPER`).
**Decisión:** B para la foto del tema: se copia reducida al tamaño de la pantalla a `files/backgrounds/` (excluida de las copias de seguridad) y su ruta va en DataStore (`theme_photo_background`). Se dibuja con `ContentScale.Crop` y un velo según el tema. Para el fondo del teléfono, un botón abre el selector del sistema (`ACTION_SET_WALLPAPER`), sin permiso.
**Por qué:** cero permisos; la foto nunca sale del teléfono; leerla una sola vez reducida cuida memoria y arranque.
**Consecuencias:** la foto se lee fuera del hilo principal al iniciar; hasta tenerla se ve el fondo dibujado del tema. Al quitarla se borra el archivo.

## ADR-023 · Notificaciones y música en el inicio — Aceptada (2026-10-26)
**Contexto:** Fase 2 del ROADMAP: puntos de notificación, lo último bajo los favoritos y un reproductor contextual.
**Decisión:** `VeloraNotificationListener` (exported=false, `BIND_NOTIFICATION_LISTENER_SERVICE`), opt-in desde Ajustes con aviso destacado previo. Si la función está apagada, el servicio se desconecta solo tras conectar (B-003). Publica en `NotificationsRepository` (solo memoria, `StateFlow`) agrupado por app y perfil, con 100 ms de debounce. Las notificaciones `VISIBILITY_SECRET` solo cuentan, sin título ni texto. La música se lee con `MediaSessionManager.getActiveSessions` usando el mismo listener (no hace falta otro permiso) y se controla con `TransportControls`. Doble toque para bloquear: `LockAccessibilityService` (ADR-009), que solo ejecuta `GLOBAL_ACTION_LOCK_SCREEN`.
**Por qué:** es la única vía de Android para estas funciones; todo queda en memoria y en el teléfono.
**Consecuencias:** nada de notificaciones en disco ni en logs (REGLAS). Si el usuario quita el acceso, los puntos desaparecen y Ajustes muestra "Falta el acceso". Algunos fabricantes apagan la accesibilidad (B-002): el doble toque avisa y abre Accesibilidad.

## ADR-024 · Tiempo de uso por app — Aceptada (2026-10-27)
**Contexto:** el autor pidió todo lo pendiente; "tiempo de uso" estaba en la Fase 4.
**Opciones:** A) contar nosotros (no sabemos cuándo se usa otra app sin un servicio). B) `UsageStatsManager` con el acceso especial `PACKAGE_USAGE_STATS`, que concede el usuario en Ajustes.
**Decisión:** B, opt-in con aviso destacado. Solo se consulta `queryAndAggregateUsageStats` de hoy al abrir el panel de una app, en `Dispatchers.IO`; se muestra "Hoy: 1 h 20 min". Nada se guarda ni sale del teléfono.
**Por qué:** es la vía del sistema; sin el acceso la función no hace nada.
**Consecuencias:** `tools:ignore="ProtectedPermissions"` en el manifiesto (el permiso se concede como acceso especial). El "freno de uso" (avisar al pasar un límite) queda pendiente.

## ADR-025 · Contactos en el buscador — Aceptada (2026-10-27)
**Contexto:** búsqueda de contactos, pendiente desde la comparación con Niagara.
**Decisión:** `READ_CONTACTS` en tiempo de ejecución, pedido solo después del aviso destacado en Ajustes → Funciones. `ContactsRepository` consulta `Contacts.CONTENT_FILTER_URI` (solo `_ID` y nombre, máximo 4) en `Dispatchers.IO`; tocar abre la ficha en la app de Contactos (`ACTION_VIEW`). Si el usuario lo niega dos veces, se abre la ficha de Velora en Ajustes.
**Por qué:** el filtro del proveedor es rápido y no requiere copiar nada; sin permiso la consulta no ocurre.
**Consecuencias:** nada de números ni correos en memoria; no se indexa ni se guarda.

## ADR-026 · Carpetas y edición de widgets en el inicio — Aceptada (2026-10-27)
**Decisión:** carpetas en Room (versión 4, `folders` y `folder_apps`, migración automática): se crean desde el panel de una app ("Añadir a carpeta"), aparecen arriba de los favoritos con un mosaico de 4 íconos y se abren en un panel (sacar apps, renombrar, eliminar). Widgets: `VeloraAppWidgetHost` crea `LongPressWidgetHostView`, que detecta el toque largo antes que el widget; el widget queda en modo edición con "Quitar widget" en rojo, flechas para moverlo y una barra para cambiar el alto arrastrando.
**Por qué:** las mismas tablas que favoritos (solo identidad, nunca `Intent`); los widgets son `View`s y Compose no ve su toque largo sin interceptarlo.
**Consecuencias:** Ajustes → Widgets sigue sirviendo para lo mismo.

## ADR-027 · Temas compartibles de Velora (y no los de Niagara) — Aceptada (2026-10-27)
**Contexto:** el autor pidió poder usar en Velora los temas de la comunidad de Niagara, porque "son públicos".
**Opciones:** A) leer los archivos o respaldos de Niagara. B) Un formato propio para compartir temas entre usuarios de Velora. C) Soportar packs de íconos de terceros (formato común de launchers, Play Store).
**Decisión:** B ahora y C como siguiente paso. Se rechaza A: que algo esté publicado no da licencia para usarlo; los fondos, packs y fuentes de esos temas son de sus autores, y el formato de Niagara es propio y no está documentado (habría que hacerle ingeniería inversa, contra REGLAS §6 "No copiar nada de Niagara"). El formato `.velora-theme` es texto plano, una clave por línea (`velora-theme 1`, `preset=`, `font=`, `clock=`, `icons=`, `photo=` en Base64). Se exporta e importa con el selector de archivos del sistema (sin permisos). Al importar: límite de 8 MB, solo nombres conocidos de Velora (lo demás se ignora), la foto se decodifica y reduce como cualquier foto propia.
**Por qué:** la comunidad puede compartir temas sin riesgo legal ni de seguridad: el archivo no puede ejecutar nada.
**Consecuencias:** pruebas en `ThemeFileTest`. Un tema importado incluye la foto de quien lo compartió: es responsabilidad de esa persona tener derecho a compartirla.

---

## Plantilla

```
## ADR-0XX · Título — Propuesta | Aceptada | Rechazada | Reemplazada por ADR-0YY (fecha)
**Contexto:** qué problema o fuerza obliga a decidir.
**Opciones:** A, B, C (con pros/contras breves).
**Decisión:** 
**Por qué:** 
**Consecuencias:** qué se vuelve más fácil / más difícil; qué docs se actualizan.
```
