# Arquitectura

## 1. Visión general

```
┌──────────────────────────── :app ─────────────────────────────┐
│ HomeActivity (HOME, singleTask)                               │
│   └─ VeloraApp (Compose root, tema, insets, back predictivo) │
│        ├─ feature/home      HomeScreen  + HomeViewModel       │
│        ├─ feature/drawer    AppListScreen + WaveAlphabet      │
│        ├─ feature/search    SearchSheet + SearchViewModel     │
│        ├─ feature/settings  SettingsScreens                   │
│        └─ feature/notifications (Fase 2)                      │
│ services/  VeloraNotificationListener (F2), LockService (F4) │
│ di/AppContainer                                               │
└───────────────┬───────────────────────────────────────────────┘
                │ depende de
   ┌────────────┴──────────┬───────────────────┬────────────────┐
   │ :core:data            │ :core:designsystem│ :core:model    │
   │ AppsRepository        │ Tema, tokens,     │ data classes   │
   │ IconRepository        │ Motion, componentes│ @Immutable    │
   │ PrefsRepository       │ base               │ sin Android   │
   │ NotificationsRepo(F2) │                    │               │
   │ Room + DataStore      │                    │               │
   └───────────────────────┴────────────────────┴───────────────┘
   :baselineprofile   :macrobenchmark   (módulos de prueba)
```

Regla de dependencias: `feature → core:data → core:model`. `core:designsystem` no depende de `core:data`. Ningún módulo `core` depende de `:app`.

## 2. Estructura de carpetas

```
velora/
├── app/src/main/java/app/velora/launcher/
│   ├── HomeActivity.kt
│   ├── VeloraApplication.kt
│   ├── di/AppContainer.kt
│   ├── ui/VeloraApp.kt
│   ├── feature/
│   │   ├── home/       (HomeScreen.kt, HomeViewModel.kt, HomeUiState.kt, components/)
│   │   ├── drawer/     (AppListScreen.kt, WaveAlphabet.kt, AppRow.kt, DrawerViewModel.kt)
│   │   ├── search/
│   │   ├── appmenu/    (menú de pulsación larga, atajos)
│   │   └── settings/
│   └── services/
├── core/model/        (AppEntry, AppKey, Favorite, UserProfileKind, NotificationSummary…)
├── core/data/         (repositorios, fuentes del sistema, Room, DataStore)
├── core/designsystem/ (theme/, motion/, components/)
├── baselineprofile/
├── macrobenchmark/
├── docs/
├── gradle/libs.versions.toml
└── CLAUDE.md
```

## 3. Patrón de UI: UDF (flujo unidireccional)

- Cada pantalla: `XxxUiState` (data class `@Immutable`), `XxxViewModel` expone `StateFlow<XxxUiState>`, la UI emite **eventos** (`onAppClick(key)`), nunca muta estado.
- Efectos de un solo uso (abrir app, mostrar snackbar) por `Channel`/`SharedFlow` consumido en `LaunchedEffect`, o mejor: llamada directa a un `AppLauncher` desde la UI cuando no requiere lógica.
- Composables **sin lógica de negocio**: reciben estado y lambdas.
- Recolección con `collectAsStateWithLifecycle()`.

## 4. Datos — flujos principales

### 4.1 Lista de apps

```
LauncherApps.getActivityList(null, user) para cada UserHandle de getProfiles()
        │  (Dispatchers.IO)
        ▼
AppsSystemSource ──► AppsRepository ──► Flow<List<AppEntry>> ordenado
        ▲                   │
LauncherApps.Callback       └─► Room (caché: componente, label, sortKey, perfil, firstInstall)
(onPackageAdded/Removed/Changed/
 onPackagesAvailable/Unavailable/Suspended)
```

- **Arranque**: se emite primero lo que hay en Room (instantáneo), luego se reconcilia con el sistema en segundo plano y se emite la diferencia.
- `AppKey` = `ComponentName` + `UserHandle` serial. Es la `key` en listas lazy y la clave primaria.
- `sortKey`: label normalizado con `Collator` en `Locale` actual, sin tildes, mayúsculas; `Ñ` se trata como letra propia en español. Se recalcula al cambiar idioma (`ACTION_LOCALE_CHANGED`).
- Al reanudar (`onResume`) se verifica una "huella" barata (conteo + hash de paquetes) por si se perdieron callbacks con el proceso muerto.

### 4.2 Íconos

```
IconRepository
 ├─ Memoria: LruCache<AppKey, ImageBitmap> (tamaño = 1/8 de la memoria de la app)
 ├─ Disco: PNG/WebP pre-renderizado por (AppKey, versión app, tamaño, forma, iconPack)
 └─ Sistema: LauncherActivityInfo.getBadgedIcon(density) → render a bitmap en Dispatchers.Default
```
- La UI pide `iconFor(key): State<ImageBitmap?>`; muestra placeholder del color dominante y nunca bloquea.
- Precarga de íconos de favoritos en el arranque; los de la lista se cargan según visibilidad.
- Invalida por `onPackageChanged` y cambio de forma/paquete de íconos.

### 4.3 Preferencias

- `PrefsRepository` sobre **DataStore Proto** (tipado). Una sola instancia. Lectura como `Flow`.
- Favoritos y ocultos en **Room** (relacionales, reordenables).

### 4.4 Notificaciones (Fase 2)

- `VeloraNotificationListener` (servicio del sistema) → publica en `NotificationsRepository` (en memoria, `StateFlow`). **Nunca a disco.**
- Solo procesa si el usuario activó la función; si no, se solicita `requestUnbind()` **después** de `onListenerConnected()`.
- Se guarda un resumen mínimo: paquete, usuario, título, texto corto, hora, `key`, acciones de respuesta.

## 5. Ciclo de vida de `HomeActivity`

- `launchMode="singleTask"`, `stateNotNeeded="true"`, `excludeFromRecents="true"`, `resumeWhilePausing="true"`, `clearTaskOnLaunch="true"`, `windowSoftInputMode="adjustNothing"`, tema con `windowShowWallpaper=true` y fondo transparente.
- `configChanges`: manejar `orientation|screenSize|screenLayout|smallestScreenSize|density|keyboard|keyboardHidden|navigation|uiMode` para no recrear la Activity (recrear un launcher = parpadeo).
- `onNewIntent(HOME)`: si ya estamos en inicio → cerrar lista/búsqueda/pop-ups y volver al estado raíz con animación. Si veníamos de una app → no animar, solo resetear estado.
- **Back predictivo**: usar `BackHandler`/`PredictiveBackHandler` de Compose; en el estado raíz el back no hace nada (no se puede "salir" del inicio).
- Ver `BUGS.md` B-001 (UI congelada tras desbloqueo) para la estrategia de refresco en `onResume` / `onWindowFocusChanged` / `onTopResumedActivityChanged`.

## 6. Inyección de dependencias

`AppContainer` creado en `VeloraApplication.onCreate()` con `by lazy` para todo: **nada se inicializa hasta que se usa**. Los ViewModels reciben dependencias por `viewModelFactory { initializer { ... } }`.

Si el grafo crece mucho (> ~25 dependencias), evaluar Hilt con ADR.

## 7. Concurrencia

- `Dispatchers.IO`: PackageManager/LauncherApps, Room, DataStore, disco.
- `Dispatchers.Default`: ordenar, normalizar, renderizar íconos, filtrar búsqueda.
- `Main`: solo UI.
- Scope de la app: `applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)` en `AppContainer`, para trabajo que debe sobrevivir a la pantalla (reconciliación de apps).

## 8. Dependencias permitidas

| Librería | Por qué | Nota |
|---|---|---|
| Compose BOM, Material3, Activity Compose, Lifecycle | UI | Versiones vía BOM |
| Kotlin Coroutines | Concurrencia | |
| Room (KSP) | Caché de apps, favoritos | |
| DataStore Proto | Preferencias | |
| Kotlinx Serialization | Respaldo/exportación | Fase 4 |
| ProfileInstaller, Benchmark/Macrobenchmark | Rendimiento | |
| JUnit, Turbine, Robolectric, Compose UI Test | Pruebas | Solo test |
| detekt, ktlint | Calidad | Solo build |

**Prohibido sin ADR:** Coil/Glide (los íconos no son imágenes de red), Hilt/Koin, RxJava, Retrofit/OkHttp (no hay red), Firebase/Crashlytics/analytics, librerías de animación de terceros (Lottie, etc.).

Versiones: todas fijadas en `gradle/libs.versions.toml`, última estable al iniciar; nada `-alpha` salvo ADR.

## 9. Build

- `minSdk 26` (íconos adaptativos, `LauncherApps` moderno), `targetSdk 36`, `compileSdk 37` (ADR-013). Los tres viven en `gradle/libs.versions.toml`.
- Sin `keystore.properties` en la raíz, el release se firma con la clave debug (sirve para benchmarks locales; Play lo rechaza). Formato: `storeFile`, `storePassword`, `keyAlias`, `keyPassword`.
- Release: R8 full mode, `isShrinkResources = true`, Baseline Profile incluido, sin logs (`Log.d` eliminado por R8 o detrás de `BuildConfig.DEBUG`).
- `applicationId` debug con sufijo `.debug` para tener ambas instaladas.
