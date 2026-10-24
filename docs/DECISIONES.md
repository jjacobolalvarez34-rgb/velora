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

## ADR-005 · Room para caché de apps + DataStore Proto para preferencias — Aceptada
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
