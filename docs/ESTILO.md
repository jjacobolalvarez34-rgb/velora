# Estilo — diseño visual, movimiento y UX

> Fuente de verdad visual. Si un valor no está aquí, se agrega aquí primero y luego se usa en código.
> Todo vive en el módulo `core/designsystem`: `Color.kt`, `Type.kt`, `Spacing.kt`, `Shape.kt`, `motion/Motion.kt`.

## 1. Personalidad

**Agua tranquila.** Superficies limpias, texto como protagonista, movimiento que fluye y se asienta con suavidad. Nunca rebota de forma caricaturesca; nunca es brusco.

Palabras guía: *calmado, preciso, líquido, ligero*.
Anti-palabras: *ruidoso, recargado, gamer, neón*.

## 2. Color

- Base: **Material You dinámico** (`dynamicDarkColorScheme` / `dynamicLightColorScheme`) en Android 12+.
- Respaldo (Android 8–11): paleta propia "Velora" — azul-pizarra neutro, definida en `Color.kt`.
- El fondo de inicio es **el fondo de pantalla del usuario** (`windowShowWallpaper`). Sobre él se pone un velo (scrim) degradado sutil para garantizar contraste del texto.
- Texto sobre wallpaper: contraste mínimo **4.5:1**. Si el wallpaper es muy claro/variado, el scrim se oscurece automáticamente (calcular luminancia del wallpaper con `WallpaperColors`).
- Acento: solo `primary` del esquema dinámico. Un acento por pantalla.
- Prohibido: colores hardcodeados en composables (`Color(0xFF...)`) fuera de `Color.kt`.

## 3. Tipografía

- Fuente: elegible en **Ajustes → Tema** (ADR-016). Por defecto **Plus Jakarta Sans**; también Figtree, Fraunces, Quicksand y la del sistema. Todas variables y empaquetadas para que el texto nunca "salte" en el primer frame. Los idiomas no latinos usan la fuente del sistema.
- Escala (sp), siempre respetando el tamaño de fuente del sistema:

| Token | Uso | Tamaño | Peso |
|---|---|---|---|
| `clock` | Hora en inicio | 56 | Light |
| `clockDay` | Día grande del reloj "Día" ("JUE") | 120 | Bold, espaciado 4 sp |
| `date` | Fecha bajo la hora | 16 | Regular |
| `favorite` | Nombre de favorito | 22 | Regular |
| `appRow` | Fila de lista completa | 18 | Regular |
| `letter` | Alfabeto ola | 13 | Medium |
| `letterActive` | Letra bajo el dedo | 28 | SemiBold |
| `caption` | Notificación resumida, pistas | 14 | Regular |
| `sectionHeader` | Letra de sección en la lista | 13 | SemiBold |
| `brandTitle` | "Velora" en la landing | 44 | Light, espaciado 2 sp |

## 4. Espaciado, forma y layout

- Grilla base de **4 dp**. Tokens: `xs 4`, `s 8`, `m 16`, `l 24`, `xl 32`, `xxl 48`.
- Altura de fila de favorito: 56 dp. Fila de lista completa: 52 dp. Objetivo táctil mínimo: 48 dp.
- Íconos: 40 dp en favoritos, 36 dp en lista. Forma por defecto: círculo (íconos adaptativos con máscara).
- Esquinas: pop-ups y hojas `28 dp`; menús `16 dp`.
- **Edge-to-edge siempre** (obligatorio con targetSdk 36): contenido detrás de barras de sistema, con `WindowInsets` correctos.
- **Zona del pulgar**: favoritos anclados abajo (alineados al fondo de la pantalla), reloj arriba. En modo zurdo todo se espeja: el inicio se dibuja con `LayoutDirection.Rtl`, así alfabeto, márgenes, íconos y zona de gestos cambian de lado sin código aparte; la ola se abre hacia la derecha.
- **Tamaño de texto** (Ajustes → Diseño): 0,9× / 1× / 1,15× encima del tamaño de fuente del sistema.

## 5. Movimiento (motion)

### 5.1 Reglas

1. **Resortes para lo espacial** (posición, escala, tamaño); **tweens cortos para efectos** (opacidad, color).
2. Toda animación es **interrumpible**: si el usuario toca a mitad de camino, se redirige desde el estado actual, no se reinicia.
3. Animaciones **dirigidas por el gesto** cuando hay gesto: el elemento sigue al dedo 1:1 y al soltar lo toma un resorte con la velocidad del dedo.
4. Respetar "Quitar animaciones" del sistema (`Settings.Global.ANIMATOR_DURATION_SCALE == 0` o `TRANSITION_ANIMATION_SCALE == 0`): se reemplaza por fundidos instantáneos (≤ 50 ms) o ningún movimiento.
5. Ninguna animación bloquea la interacción. Nunca `delay()` antes de navegar.
6. Solo se animan propiedades de **dibujo** (`graphicsLayer`: translation, scale, alpha, rotation) siempre que se pueda. Animar tamaño/padding provoca relayout: evitar en elementos repetidos.

### 5.2 Tokens (`core/designsystem/motion/Motion.kt`)

Basados en `MotionScheme` de Material 3. Velora usa **standard** por defecto y **expressive** solo en momentos "héroe" (abrir pop-up).

| Token | Tipo | Valor inicial (ajustable tras pruebas) | Uso |
|---|---|---|---|
| `SpatialFast` | spring | dampingRatio 0.9, stiffness 1400 | Letras del alfabeto, press de fila |
| `SpatialDefault` | spring | dampingRatio 0.85, stiffness 700 | Entrar/salir lista completa, reordenar |
| `SpatialHero` | spring | dampingRatio 0.75, stiffness 380 | Pop-ups, carpetas |
| `EffectFast` | tween | 90 ms, `FastOutSlowIn` | Alpha de elementos pequeños |
| `EffectDefault` | tween | 180 ms, `FastOutSlowIn` | Fundidos de secciones |
| `PressScale` | valor | 0.96 | Escala al presionar una fila |

Prohibido escribir `spring(...)` o `tween(...)` con números sueltos fuera de `Motion.kt`.

En código: `VeloraTheme.motion.spatialFast<Float>()`, `.effectDefault<Float>()`, `.pressScale`, etc. `VeloraTheme` observa "Quitar animaciones" en vivo (`rememberReducedMotion()`) y con eso los resortes pasan a `snap()` y los efectos a 50 ms, sin que cada pantalla tenga que comprobarlo. Otros tokens: `VeloraTheme.typography` (§3), `VeloraTheme.onWallpaper` (texto y velo sobre el wallpaper), `Spacing`, `Sizes`, `Shapes` (§4).

### 5.3 Coreografía de momentos clave

| Momento | Qué pasa | Duración percibida |
|---|---|---|
| Arranque en frío | Reloj y favoritos aparecen con fade+slide 8 dp escalonado 20 ms por fila | < 250 ms total |
| Volver a Inicio desde una app | Sin animación propia: deja que la transición del sistema mande. Lista ya lista, sin recomponer | Instantáneo |
| Abrir app | `ActivityOptions.makeClipRevealAnimation` / `makeScaleUpAnimation` desde los bounds de la fila | Sistema |
| Abrir lista completa | Deslizar desde el alfabeto o swipe arriba; la lista sigue al dedo | Gesto + `SpatialDefault` |
| Presionar fila | Escala a `PressScale` con `SpatialFast`; háptico ligero | 60–100 ms |
| Aparece contextual (música) | Se expande en altura con `SpatialDefault` mientras el resto se desplaza | ~300 ms |

## 6. Alfabeto ola (diseño propio)

El elemento firma. Especificación:

- Columna de letras presentes (solo letras que tienen apps + `#` para números/símbolos + ícono de ajustes al final). **Incluye la Ñ** cuando hay apps que empiezan por Ñ.
- Reposo: letras pequeñas (`letter`), color `onSurfaceVariant` 70 %.
- Al tocar/arrastrar:
  - Cada letra se desplaza horizontalmente hacia el centro de la pantalla según una **campana gaussiana** sobre la distancia vertical al dedo: `dx(d) = A · exp(−d² / (2σ²))`, con `A = 56 dp`, `σ = 3 filas` (ajustable).
  - Escala: `1 + 0.9 · exp(−d²/(2σ²))`; la letra bajo el dedo pasa a `letterActive`.
  - Los valores siguen al dedo cada frame (sin animación entre frames), y al soltar todas vuelven a reposo con `SpatialFast`.
  - Haptic tick (`HapticFeedbackConstants.CLOCK_TICK` / `SEGMENT_TICK`) **solo al cambiar de letra**.
  - La lista de apps salta (`scrollToItem`, sin animación) a la primera app de esa letra; las apps de esa letra se resaltan.
- Implementación: **un solo `Canvas`/`Layout` personalizado** que dibuja todas las letras leyendo el offset del dedo en la fase de dibujo (`drawWithCache` / lambda de `graphicsLayer`), **no** un composable por letra con su propio estado. Así un movimiento del dedo = 0 recomposiciones.

## 7. Íconos

- Estilos (Ajustes → Tema), renderizados en `IconRenderer` fuera del hilo principal:
  - **Círculo** (por defecto): capas del ícono adaptativo a sangre completa y recorte circular suavizado (`BitmapShader`, nunca `clipPath`, que deja el borde dentado). Los íconos antiguos van sobre un círculo neutro.
  - **Forma del sistema**: `getBadgedIcon` tal cual.
  - **Minimalista** (ADR-021): círculo lleno (`iconCircle`) con un glifo de línea encima (`iconGlyph`, trazo 2, 55 % del círculo). El glifo sale de `GlyphMatcher` (paquete → palabra clave → categoría); si no hay, la capa monocroma de Android 13+; si tampoco, la inicial de la app.
  - **Sin íconos**: solo texto; es el más minimalista y el más rápido.
- La insignia de perfil de trabajo se agrega encima del ícono ya recortado (`getUserBadgedIcon`).
- **No** usar ni imitar los paquetes de íconos de Niagara.

## 6.1 La ola, más grande (2026-10-26)

- El alfabeto se estira hasta **la mitad del ancho de la pantalla** en un arco amplio (σ = 6,5 filas). Las letras crecen poco (1 + 0,35·g): el estiramiento hace el trabajo.
- La burbuja es **blanca con la letra oscura** sobre el wallpaper (en cada tema, su acento) y queda pasando la cresta, junto al pulgar.
- **La ola sigue al dedo** (0.6.0): el estiramiento es la distancia horizontal del dedo al borde, 1 a 1, entre 20 dp y la mitad de la pantalla. Poco movimiento = poca ola; se lee en la fase de dibujo, sin animar entre frames.
- Si hay favoritos, el alfabeto empieza con **★**: en modo por letra muestra los favoritos; en la lista completa salta arriba.

## 6.2 Ajustes y menús (ADR-019)

- Ajustes en **tarjetas** redondeadas (20 dp) con un poco de aire entre ellas; las opciones principales llevan su ícono en un círculo suave.
- El menú de una app es un **panel desde abajo**: el ícono y el nombre arriba, luego sus atajos, y después Favorito, Información, Ocultar y Desinstalar, cada uno con su ícono. Al final, "Ajustes de Velora".
- Mantener presionado el inicio abre otro panel: "Agregar widget" y "Ajustes de Velora".

## 7.0 Temas (ADR-018)

| Tema | Fondo | Texto | Fuente / reloj / íconos recomendados |
|---|---|---|---|
| Tu fondo | wallpaper del usuario + velo suave y sombra en el texto | blanco neutro (nunca el azul del sistema) | Nunito / Clásico / Círculo |
| Noche | casi negro con un brillo frío tenue, bruma y grano | claro | Nunito / Clásico / Minimalista |
| Papel | papel de algodón crema con fibra fina y grano | tinta oscura | Fraunces / Día / Minimalista |
| Sakura | cielo rosa en malla (durazno, rosa, lila), una luna con halo, siete pétalos | oscuro (barras del sistema oscuras) | Nunito / Clásico / Círculo |
| Bosque | cuatro filas de colinas con pinos y niebla en los valles | claro | Quicksand / Clásico / Minimalista |
| Aurora | azul noche con dos cortinas de luz onduladas, rayos finos y pocas estrellas | claro | Nunito / Centrado / Minimalista |
| Noche pixel | cielo en tres tonos, nebulosa tramada, estrellas en cruz, luna con cráteres y montañas, todo en la misma rejilla | casi blanco con sombra dura de píxel; acento amarillo luna | Silkscreen / Clásico / Círculo |
| Atardecer | lofi: durazno a rosa, sol difuso, nubes en hilos y ciudad con ventanas | tinta ciruela | Fraunces / Centrado / Minimalista |
| Océano | vista aérea: turquesa profundo a aguamarina con frentes de espuma | espuma | Figtree / Clásico / Minimalista |
| Nieve | blanco casi puro con tres lomas de nieve muy suaves | carbón | Plus Jakarta Sans / Clásico / Minimalista |
| Café | marrón tostado, luz cálida de ventana y crema que gira tenue | crema | Fraunces / Día / Minimalista |
| Galaxia | franja de nebulosa de humo (magenta a turquesa) con polvo oscuro, estrellas, planeta con anillo | blanco con sombra profunda | Figtree / Centrado / Minimalista |
| Neón | cielo violeta con bruma, sol rayado y cuadrícula en perspectiva | blanco con brillo magenta | Plus Jakarta Sans / Centrado / Minimalista |
| Matcha | degradado de malla: crema verdosa, té y pistacho | verde bosque | Quicksand / Clásico / Minimalista |
| Lavanda | degradado de malla: lila, violeta, rosa y pervinca | violeta profundo | Nunito / Día / Minimalista |
| Ceniza | cenizas cálidas, una hoguera abajo, brasas que flotan, viñeta | hueso/pergamino, acento brasa | Cinzel / Día / Minimalista |
| Mármol | mármol blanco con vetas grises que serpentean | grafito cálido | Fraunces / Apilado / Minimalista |
| Mármol negro | casi negro con humo gris y vetas doradas finas | hueso, acento dorado | Cinzel / Apilado / Minimalista |
| Grafito | gris carbón, una luz suave arriba y grano fino; el más sobrio | blanco | Figtree / Banner / Minimalista |

- **Ceniza** es un tema de fantasía oscura propio (sin nombres, logos ni arte de ningún juego): fuente romana grabada (Cinzel, OFL), círculos de íconos de hierro y glifos color hueso.
- **Foto propia** (ADR-022): cualquier tema puede llevar una foto del usuario de fondo (recortada a pantalla, con el velo del tema); los colores del texto siguen siendo los del tema.

- **Arte procedural** (`components/ProceduralArt.kt`, `ArtCache.kt`): los fondos se calculan píxel a píxel con ruido fbm y *domain warping* (semilla fija), a la mitad de resolución y fuera del hilo principal; se escalan con filtrado y se guardan en memoria y en `cacheDir/art/` (PNG; subir `ART_VERSION` al cambiar un dibujo). Mientras se generan se ve el degradado del tema y luego entran con `effectDefault`. Encima: detalles vectoriales nítidos y un grano de película fino (mosaico de 128 px repetido). Noche pixel no lleva filtrado ni grano. Zonas tranquilas detrás del reloj (10–30 % de la altura) y de los favoritos (abajo a la izquierda).

Regla: **sencillo y estético**. Pocas formas, mucho aire: el protagonista es el texto.

- **Íconos minimalistas** = círculo claro lleno con el glifo oscuro (o al revés en temas claros), iguales para todas las apps aunque no traigan ícono temático (ADR-021).
- **Carpetas** (ADR-026): fila igual a un favorito, con un mosaico de hasta 4 íconos en un círculo suave y "N apps" debajo.
- **Editar un widget**: mantenerlo presionado lo marca con un borde del acento; debajo, "Quitar widget" en rojo (lo más visible), flechas y "Listo", y una barra para arrastrar el alto.
- **Notificaciones** (ADR-023): un punto de 7 dp del color de acento junto al nombre; en favoritos, el título del último aviso en una línea (`caption`). El reproductor aparece bajo el reloj creciendo en altura (`spatialDefault`) y se va igual.

- Estética más fina: íconos de 34 dp (favoritos) y 30 dp (lista), textos de 19 sp y 17 sp con 0,2 sp de espaciado, filas de 52 dp y 48 dp. Sobre fondos oscuros el texto lleva una sombra suave (desenfoque de 8, caída de 1,5): permite un velo más liviano.
- **Burbuja del alfabeto**: al arrastrar, un círculo del color de acento (56 dp) con la letra actual sigue al dedo junto a la cresta de la ola, crece desde el 60 % y se desvanece al soltar.

## 7.1 Reloj (Ajustes → Tema)

Sencillo a propósito: hora **delgada y grande** (64 sp, ExtraLight) y fecha tranquila debajo (18 sp). El encabezado baja un décimo de la pantalla: no va pegado arriba.

- **Clásico** (por defecto): "07:58" y "Lunes, 26 de octubre", a la izquierda.
- **Centrado**: lo mismo al centro, con la batería discreta junto a la fecha.
- **Día**: "LUNES" en mayúsculas espaciadas y discretas, la hora, y "26 de octubre · 82 %". (El corte con `BlendMode.Clear` de la 0.4.0 se quitó: se veía forzado.)
- **Apilado**, **Banda** y **Dúo** (ADR-028): horas sobre minutos enormes; el día gigante de lado a lado; minutos del acento.
- **Vertical**: horas sobre minutos (60 sp) en una columna angosta, una línea fina de 1 dp y, al lado, el día y la fecha con la batería.
- **Contorno**: la hora a 104 sp, SemiBold, dibujada solo con su trazo (1,5 dp, `DrawStyle` Stroke); la fecha normal debajo.
- **Minimal**: solo la hora a 30 sp, Light, con 10 sp de espaciado; debajo la fecha en mayúsculas de 11 sp muy espaciadas.
- **Analógico**: esfera de 120 dp dibujada en `Canvas`: aro fino, 12 marcas (las de 12/3/6/9 más grandes), agujas redondeadas y un punto del acento al centro; sin números. A un lado, el día y la fecha.
- **Palabras**: "Las siete y media" / "Half past seven", redondeado a 5 minutos (de "y media" en adelante cuenta hacia la hora siguiente: "veinte para las ocho"). Las frases están en `clock_words_hours` / `clock_words_patterns` de cada idioma; la hora va en el color del texto y el resto más suave. 44 sp, Light.

## 7.1b Personalizar (Ajustes → Personalizar)

- **Orden**: barra superior (atrás, título y, solo con cambios, "Descartar" y "Guardar"); la **vista previa** del inicio en un marco de teléfono (bisel negro de 7 dp, esquinas de 30 dp, sombra de 18 dp) ocupa el 45 % del alto; debajo, un **panel** con esquinas superiores de 28 dp (`surfaceContainer`).
- La vista previa se maqueta a 360×780 dp y se achica con `Modifier.fitInside` (capa con escala desde la esquina): nunca se re-maqueta al cambiar de sección y no deja bordes vacíos. Muestra barra de estado, reloj, favoritos, el alfabeto en reposo y la rayita de gestos.
- **Secciones** en una sola fila de pastillas con ícono (la elegida llena del primario y siempre a la vista): Tema · Fondo · Reloj · Íconos · Color · Letra · Elementos. Cada sección: una línea de ayuda corta y títulos de grupo iguales (`titleSmall`).
- **Tarjetas de opción** (`OptionCard`): rejillas con columnas fijas que reparten el ancho (márgenes de 16 dp, 12 dp entre tarjetas); misma proporción en toda la rejilla, nombre en una línea; elegida = aro de 2 dp del primario + marca con check.
- **Guardar / descartar**: todo lo que se toca va a un borrador (`ThemeDraft`) que solo se ve en la vista previa; "Guardar" lo escribe de una vez. Salir con cambios pregunta: Guardar / Descartar / Seguir editando.
- **Fondos anteriores**: se guardan las últimas 8 fotos en `files/backgrounds/`; tocar una la vuelve a poner (en el borrador), la ✕ la borra (la que usa el inicio no se puede borrar). "Del tema" deja de usar la foto sin borrar el archivo.

## 7.2b Buscador

- **Calculadora**: si lo escrito es una cuenta ("12*3", "2,5 x 4"), arriba sale "= 36"; tocarlo lo copia.
- **Buscar en la web**: última fila de los resultados. La búsqueda la hace la app de búsqueda o el navegador del teléfono; Velora sigue sin `INTERNET`.

## 7.2 Lista y búsqueda

- **Por letra (por defecto, pedido del autor):** al tocar el alfabeto solo se ven las apps de esa letra, con la letra grande arriba. Al deslizar el pulgar cambia la letra y las apps entran deslizándose en la dirección del dedo (`SpatialFast` + fundido). Al soltar se queda la última letra.
- **Abierta deslizando, sin letra:** buscador, la pista "Toca una letra…" y Recién instaladas.
- **Lista completa** con scroll: opción en Ajustes → Diseño.
- Arriba de la lista: **Recién instaladas** (últimos 14 días, máximo 4; `LauncherActivityInfo.firstInstallTime`, sin permisos).
- El buscador se **esconde al bajar** por la lista y vuelve al subir o al llegar arriba.
- **Modo búsqueda** (buscador con foco o con texto): la lista completa y el alfabeto se van con un fundido; sin texto se ven las recientes, con texto los resultados. Los resultados entran con fundido (`EffectDefault`) y se reacomodan con resorte (`SpatialDefault`, `animateItem`).

## 7.3 Bienvenida e introducción

- **Logo vivo** (`AnimatedLogo`): entra con `SpatialHero`, flota (±6 dp, ±2°, `ambientFloat`) y un brillo plateado recorre solo la cinta (capa `Offscreen` + `BlendMode.SrcAtop`, `ambientSweep`). Con "Quitar animaciones", quieto.
- **Introducción de la primera vez** (5 páginas deslizables): logo, deslizar para buscar, alfabeto ola, mantener presionada una app y Tema, cada una con una demo animada (`demoLoop`). En la última: "Usar Velora como inicio" / "Ahora no". Se ve una sola vez (`onboarding_done`).

## 8. Háptica

- Tick al cambiar de letra; "confirm" ligero al abrir pop-up; nada al tocar una app (el sistema ya responde).
- Respetar ajuste de vibración táctil del sistema (`View.performHapticFeedback` lo hace).

## 9. Accesibilidad (no negociable)

- Todos los elementos con `contentDescription` / semántica correcta. TalkBack debe poder abrir cualquier app.
- El alfabeto ola tiene alternativa accesible: con TalkBack activo se comporta como lista de botones de letra.
- Tamaño de fuente del sistema hasta 200 % sin texto cortado (filas crecen).
- Contraste AA.

## 10. Microcopy

- Español neutro latinoamericano, tuteo. Frases cortas.
- Explicaciones de permisos: qué hace, por qué, que es opcional, cómo quitarlo. Ejemplo:
  > **Ver tus notificaciones aquí**
  > Para mostrarlas en tu inicio necesitamos acceso a notificaciones. Se quedan en tu teléfono; no guardamos ni enviamos nada. Puedes quitarlo cuando quieras.
- Todo texto en `strings.xml` (con `values-en` desde el MVP). Nada de texto hardcodeado.
