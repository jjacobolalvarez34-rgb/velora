# CLAUDE.md — Velora Launcher

> Punto de entrada para Claude Code. Léelo completo al inicio de cada sesión.
> Nombre del producto: **Velora** (paquete `app.velora.launcher`). Antes se llamó "Cascada" (ver `docs/DECISIONES.md`, ADR-001 y ADR-011).

## Qué es este proyecto

Un **launcher (pantalla de inicio) para Android**, minimalista, de una sola mano, ligero y con animaciones fluidas a 60/120 fps. Inspirado en el *tipo* de experiencia de Niagara Launcher (lista vertical en vez de cuadrícula, alfabeto lateral con efecto ola, notificaciones integradas), **pero con identidad, código y recursos 100 % propios**.

Un launcher **no cambia el sistema operativo**: es la app que Android abre cuando el usuario toca "Inicio". Reemplaza la pantalla de inicio y el cajón de apps. Todo lo demás (ajustes, notificaciones del sistema, multitarea) sigue siendo de Android.

## Stack (fijo salvo ADR nuevo)

- Kotlin + Jetpack Compose (Material 3, colores dinámicos / Material You)
- Una sola Activity (`HomeActivity`) con `HOME` intent
- Corrutinas + Flow; estado con `StateFlow` + `ViewModel`
- DI manual (`AppContainer`), sin Hilt al inicio
- Room (caché de apps y favoritos) + DataStore (preferencias)
- Baseline Profiles + Macrobenchmark desde la fase 1
- `minSdk 26`, `targetSdk 36`, `compileSdk 37` (ADR-013), en `gradle/libs.versions.toml`
- AGP 9 (Kotlin integrado: no se aplica `org.jetbrains.kotlin.android`), Gradle 9.7, JDK 17+ (en esta máquina: `~/.jdks/jbr-21.0.11`)

## Mapa de documentación

| Archivo | Cuándo leerlo |
|---|---|
| `docs/PRODUCTO.md` | Qué construimos, para quién, alcance del MVP |
| `docs/INVESTIGACION.md` | Qué hace Niagara y qué aprendimos (con fuentes) |
| `docs/ESTILO.md` | Antes de tocar UI, colores, tipografía o animaciones |
| `docs/ARQUITECTURA.md` | Antes de crear archivos, módulos, clases o flujos de datos |
| `docs/RENDIMIENTO.md` | Antes de tocar listas, íconos, arranque o animaciones |
| `docs/REGLAS.md` | Siempre. Reglas de código no negociables |
| `docs/PROCEDIMIENTOS.md` | Cómo trabajar una tarea de principio a fin |
| `docs/VERIFICACIONES.md` | Antes de dar algo por terminado |
| `docs/SEGURIDAD.md` | Antes de agregar un permiso, servicio o dato del usuario |
| `docs/BUGS.md` | Antes de depurar; y para registrar cada bug encontrado |
| `docs/ROADMAP.md` | Para saber en qué fase estamos y qué sigue |
| `docs/PENDIENTES.md` | **Al empezar cada sesión**: lo próximo a hacer, en orden |
| `docs/DECISIONES.md` | Para entender por qué algo se hizo así; registrar decisiones nuevas |

## Comandos

En Windows usa `gradlew.bat`; en macOS/Linux `./gradlew`.

```
gradlew assembleDebug                 # compilar debug
gradlew installDebug                  # instalar en el dispositivo conectado
gradlew testDebugUnitTest             # pruebas unitarias
gradlew connectedDebugAndroidTest     # pruebas instrumentadas
gradlew lint detekt ktlintCheck       # análisis estático (detekt/ktlint como CLI, ADR-012)
gradlew ktlintFormat                  # corrige el estilo automáticamente
gradlew :app:generateBaselineProfile  # requiere dispositivo API 33+ conectado
gradlew :macrobenchmark:connectedBenchmarkReleaseAndroidTest
```

Para probar como launcher: instalar, pulsar Inicio y elegir Velora → "Siempre". Para volver: Ajustes → Apps → App de inicio predeterminada.

## Reglas de oro (resumen; detalle en `docs/REGLAS.md`)

1. **El hilo principal es sagrado.** Nada de disco, red, `PackageManager` ni decodificación de íconos en el main thread.
2. **Nunca pedir `QUERY_ALL_PACKAGES`.** Las apps se listan con `LauncherApps` + `<queries>` (ver `docs/SEGURIDAD.md`).
3. **Cero permisos nuevos sin ADR** en `docs/DECISIONES.md` y entrada en `docs/SEGURIDAD.md`.
4. **Toda animación usa los tokens de movimiento** de `core/designsystem/motion` y respeta "Quitar animaciones" del sistema.
5. **Toda lista lazy usa `key` estable** y modelos `@Immutable`.
6. **No copiar nada de Niagara**: nombre, íconos, textos, recursos, ni código descompilado.
7. **Ningún dato sale del teléfono.** Sin analítica de terceros ni SDKs de anuncios.
8. **Si cambias comportamiento, actualizas el doc correspondiente en el mismo cambio.**
9. **Cada bug corregido se registra en `docs/BUGS.md`** con causa raíz y prueba que lo cubre.
10. **No se marca nada como terminado sin pasar `docs/VERIFICACIONES.md`** (la sección que aplique).

## Forma de trabajar en cada tarea

1. Leer la fase actual en `docs/ROADMAP.md` y los docs que toque la tarea.
2. Revisar `docs/BUGS.md` por problemas conocidos del área.
3. Proponer un plan corto (archivos a crear/tocar, riesgos) antes de escribir código grande.
4. Implementar en pasos pequeños que compilen.
5. Ejecutar las verificaciones aplicables.
6. Actualizar docs (BUGS, DECISIONES, ROADMAP) si aplica.
7. Resumir qué cambió y cómo probarlo en el teléfono.

## Lo que NO hacer

- No agregar librerías sin justificarlo (peso, arranque, licencia). Ver `docs/ARQUITECTURA.md` §Dependencias.
- No usar `GlobalScope`, `runBlocking` en UI, ni `!!`.
- No usar `AccessibilityService` para nada distinto de bloquear pantalla con gesto.
- No guardar contenido de notificaciones en disco.
- No "arreglar" un jank con `delay()`; se mide y se corrige la causa.
