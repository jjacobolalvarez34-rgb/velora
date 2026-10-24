# Reglas de código

> Obligatorias. Si una regla estorba de verdad, se cambia con un ADR en `DECISIONES.md`, no se ignora.

## 1. Kotlin

- Kotlin idiomático, `val` por defecto, `data class` para modelos, `sealed interface` para estados/eventos.
- **Prohibido `!!`**. Usar `?:`, `requireNotNull` con mensaje, o rediseñar.
- Prohibido `GlobalScope`, `runBlocking` (excepto tests), `Thread.sleep`.
- Errores esperados como `Result`/sealed class; excepciones solo para lo excepcional. Nunca `catch (e: Exception) {}` vacío: como mínimo, log en debug y estado de error.
- Llamadas al sistema que pueden fallar por OEM (`LauncherApps`, `startActivity`, `ShortcutManager`) siempre envueltas: capturar `SecurityException`, `ActivityNotFoundException`, `IllegalStateException` y degradar con mensaje amable.
- Funciones ≤ 40 líneas como guía; archivos ≤ 400 líneas. Si crece, dividir.
- Nombres en **inglés** en código; comentarios y docs en **español**.

## 2. Compose

- Un composable de pantalla (`XxxScreen`) recibe `uiState` + lambdas; un `XxxRoute` lo conecta al ViewModel.
- Todo composable reutilizable recibe `modifier: Modifier = Modifier` como primer parámetro opcional y lo aplica al nodo raíz.
- Sin estado de negocio con `remember`; solo estado de UI efímero (expandido, foco).
- Todo valor de diseño desde `core/designsystem` (colores, tipografía, espaciado, motion). Sin `16.dp` sueltos repetidos: usar `Spacing.m`.
- Cada composable visible tiene `@Preview` claro y oscuro (y zurdo si aplica).
- Ver `RENDIMIENTO.md` §3: estas reglas también son obligatorias.

## 3. Arquitectura

- Respeta las capas de `ARQUITECTURA.md`. La UI nunca llama a `PackageManager`/`LauncherApps` directamente.
- Un repositorio por fuente de verdad. Los ViewModels no se comunican entre sí.
- Nada de `Context` en ViewModels (salvo `applicationContext` vía repositorio).
- Estado expuesto como `StateFlow` inmutable; `MutableStateFlow` privado.

## 4. Permisos y sistema

- Ningún permiso nuevo en el manifiesto sin ADR + entrada en `SEGURIDAD.md`.
- Toda función que dependa de permiso especial funciona en modo degradado sin él.
- Nunca leer/escribir fuera del almacenamiento privado de la app.

## 5. Textos e idioma

- Todo texto visible en `strings.xml` (`values/` español por defecto, `values-en/` inglés). Plurales con `<plurals>`.
- Fechas y horas con `DateTimeFormatter`/`DateFormat` del locale del usuario; respetar 12/24 h del sistema.
- Ordenamiento con `Collator` del locale; nunca `sortedBy { it.label }` a secas.

## 6. Pruebas

- Toda lógica en `core:data` y ViewModels con prueba unitaria (JUnit + Turbine para Flows, fakes en vez de mocks).
- Cada bug corregido trae una prueba que lo reproduce (ver `BUGS.md`).
- Pruebas de UI (Compose Test) para: abrir app desde favoritos, alfabeto salta a letra, búsqueda, ocultar app.
- Nombres de prueba descriptivos: `` `buscar sin tildes encuentra Cámara`() ``.

## 7. Git

- Rama `main` siempre compila y pasa pruebas.
- Ramas: `feat/…`, `fix/…`, `perf/…`, `docs/…`, `chore/…`.
- Commits en español, imperativo, Conventional Commits: `feat(drawer): alfabeto ola con desplazamiento gaussiano`.
- Un commit = un cambio lógico. Nada de "cambios varios".
- Nunca commitear: keystore, `local.properties`, APKs, claves.

## 8. Logs

- `Log` solo en debug (wrapper `Logger` que no hace nada en release).
- Nunca loguear contenido de notificaciones, nombres de contactos ni datos de uso.

## 9. Para Claude Code específicamente

- Antes de crear un archivo, verificar que no exista algo equivalente (buscar por nombre y por función).
- No inventar APIs: si no estás seguro de que una API de Android existe en el `minSdk`, verificar con documentación y proteger con `Build.VERSION.SDK_INT`.
- No agregar dependencias por iniciativa propia; proponerlas.
- No borrar pruebas que fallan para que pase el build.
- Si una tarea toca más de ~5 archivos o un área de `BUGS.md`, presentar plan antes de editar.
- Al terminar, decir exactamente cómo probarlo en el teléfono (pasos).
