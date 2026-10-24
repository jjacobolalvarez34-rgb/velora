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

- Fuente por defecto: la del sistema (rápida, sin peso extra en el APK). Fuente propia opcional en Fase 3 (descargable vía Google Fonts provider, no empaquetada).
- Escala (sp), siempre respetando el tamaño de fuente del sistema:

| Token | Uso | Tamaño | Peso |
|---|---|---|---|
| `clock` | Hora en inicio | 56 | Light |
| `date` | Fecha bajo la hora | 16 | Regular |
| `favorite` | Nombre de favorito | 22 | Regular |
| `appRow` | Fila de lista completa | 18 | Regular |
| `letter` | Alfabeto ola | 13 | Medium |
| `letterActive` | Letra bajo el dedo | 28 | SemiBold |
| `caption` | Notificación resumida | 14 | Regular |

## 4. Espaciado, forma y layout

- Grilla base de **4 dp**. Tokens: `xs 4`, `s 8`, `m 16`, `l 24`, `xl 32`, `xxl 48`.
- Altura de fila de favorito: 56 dp. Fila de lista completa: 52 dp. Objetivo táctil mínimo: 48 dp.
- Íconos: 40 dp en favoritos, 36 dp en lista. Forma por defecto: círculo (íconos adaptativos con máscara).
- Esquinas: pop-ups y hojas `28 dp`; menús `16 dp`.
- **Edge-to-edge siempre** (obligatorio con targetSdk 36): contenido detrás de barras de sistema, con `WindowInsets` correctos.
- **Zona del pulgar**: favoritos anclados abajo (alineados al fondo de la pantalla), reloj arriba. En modo zurdo todo se espeja.

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

- Íconos del sistema vía `LauncherApps.getActivityList(...).getBadgedIcon(density)` (incluye insignia de perfil de trabajo).
- Normalizados a íconos adaptativos con máscara uniforme; los no adaptativos se escalan dentro de un fondo neutro.
- Modo "sin íconos" (solo texto) disponible: es el más minimalista y el más rápido.
- **No** usar ni imitar los paquetes de íconos de Niagara.

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
