# Rendimiento — "liviano y fluido" convertido en números

> El launcher es la app que más veces se abre al día. Cada milisegundo se multiplica.
> Los presupuestos son **objetivos del proyecto**: se calibran en el dispositivo de referencia de gama media y se vuelven umbrales de CI.

## 1. Dispositivo de referencia

- **Gama media** (ej. Samsung Galaxy A-series o Redmi Note recientes) con ~150 apps instaladas.
- Secundario: un Pixel o emulador API 36 para pruebas automáticas.
- Nunca dar por bueno algo probado solo en un teléfono de gama alta.

## 2. Presupuestos

| Métrica | Objetivo | Cómo se mide |
|---|---|---|
| Arranque en frío (TTID) | ≤ 400 ms | Macrobenchmark `StartupTimingMetric`, `CompilationMode.Partial(BaselineProfile)` |
| Arranque en caliente (volver a Inicio) | ≤ 100 ms, sin frame perdido | Macrobenchmark `StartupMode.WARM/HOT` |
| Scroll lista completa | Jank < 1 %, P90 frame < 1 periodo de refresco | `FrameTimingMetric` |
| Arrastre alfabeto ola | 0 recomposiciones por movimiento; 0 frames > 16 ms | Layout Inspector (conteo) + `FrameTimingMetric` |
| Búsqueda (por tecla) | Resultado en ≤ 16 ms para 300 apps | Microbenchmark del filtro |
| Memoria en reposo (PSS) | ≤ 120 MB con 150 apps e íconos | `dumpsys meminfo` |
| APK release | ≤ 8 MB | Tamaño de `app-release.apk` |
| Trabajo en main thread al arrancar | Sin I/O; StrictMode limpio | StrictMode en debug |

Si un cambio empeora una métrica > 10 %, no se mergea sin justificación en el PR.

## 3. Reglas de Compose

1. **Leer estado lo más tarde posible.** Lo que cambia cada frame (offset del dedo, scroll) se lee en la fase de **dibujo/layout** usando lambdas: `Modifier.offset { }`, `graphicsLayer { }`, `drawBehind { }`. Nunca `Modifier.offset(x.dp)` con un valor animado.
2. **Estabilidad.** Modelos de UI `@Immutable`; listas como `ImmutableList` (kotlinx.collections.immutable) o envueltas en clase `@Immutable`. Activar *strong skipping* (por defecto en compiladores recientes) y revisar reportes del compilador (`composeCompiler { reportsDestination }`).
3. **`key` estable** en `LazyColumn` (`AppKey.stableId`) y `contentType` por tipo de fila.
4. **`derivedStateOf`** para valores derivados de estado que cambia mucho (ej. "¿mostrar sombra?" a partir del scroll).
5. **`remember`** cálculos costosos con sus claves correctas; nada de ordenar/filtrar dentro de un composable.
6. **Lambdas estables**: referencias a métodos del ViewModel o `remember { { … } }`; evitar crear lambdas que capturan estado inestable en filas.
7. **Nada de `SubcomposeLayout`/`BoxWithConstraints`** en filas repetidas.
8. **Sin sombras/blur caros** en elementos de lista. Blur (`RenderEffect`) solo en un elemento grande, y solo en API 31+.
9. Íconos como `ImageBitmap` ya escalado al tamaño final (no `Drawable` → `painterResource` en cada fila).

## 4. Arranque

- `VeloraApplication.onCreate` hace lo mínimo: crear `AppContainer` (perezoso). Nada de lecturas sincrónicas.
- Primer frame con datos de **Room** (caché). Reconciliación con el sistema después del primer frame.
- **Baseline Profile** generado para: arranque, abrir lista, arrastrar alfabeto, buscar, abrir menú de app. Regenerar en cada release.
- Splash: ninguno. Un launcher no muestra splash; el fondo es el wallpaper.
- `ProfileInstaller` incluido para que el perfil aplique también en instalaciones fuera de Play.

## 5. Memoria

- `LruCache` de íconos dimensionado por `ActivityManager.memoryClass`.
- Responder a `onTrimMemory(TRIM_MEMORY_UI_HIDDEN)` liberando caché de íconos **de la lista completa** (no de favoritos).
- Nada de fugas de `Context`/`Activity` en repositorios (solo `applicationContext`). LeakCanary en debug.

## 6. Batería

- Cero trabajo periódico en segundo plano en el MVP. Nada de `WorkManager` salvo para respaldo (Fase 4).
- El reloj se actualiza con `ACTION_TIME_TICK` (por minuto) solo mientras la Activity está visible.
- El listener de notificaciones procesa en lote (debounce ~100 ms) y no despierta la UI si no está visible.

## 7. Cómo investigar un jank (procedimiento)

1. Reproducir en el dispositivo de referencia con build **release** (debug es mucho más lento; no sirve para medir).
2. Grabar traza con *System Trace* (Android Studio Profiler) o Perfetto.
3. Buscar frames largos: ¿composición, layout, dibujo, o hilo principal ocupado (I/O, GC)?
4. Composición → conteo de recomposiciones en Layout Inspector → aplicar §3.
5. GC → asignaciones en el bucle (crear objetos por frame) → mover fuera.
6. Escribir la causa en `BUGS.md` y agregar/ajustar un benchmark que lo cubra.

## 8. Lo que NO es optimización

- Bajar la tasa de refresco, poner `delay()`, o quitar animaciones para "ocultar" el problema.
- Optimizar sin medir antes y después.
