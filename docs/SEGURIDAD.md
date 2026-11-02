# Seguridad, privacidad y políticas

> Un launcher ve todas las apps instaladas, puede ver notificaciones y abre todo lo que el usuario toca. Eso exige más cuidado que una app normal.

## 1. Principios

1. **Mínimo privilegio**: la API más estrecha que resuelva la necesidad.
2. **Opt-in explícito** para todo permiso especial, con explicación previa y forma clara de revocarlo.
3. **Local primero**: ningún dato sale del dispositivo. No hay servidores, ni analítica, ni SDKs de terceros con red. El manifiesto **no declara `INTERNET`** en el MVP.
4. **Degradación elegante**: sin permisos especiales la app funciona completa en lo esencial.
5. **Nada sensible persistido**: notificaciones solo en memoria.
6. **Velora solo cambia Velora** (ADR-037): nunca el fondo, los íconos, los temas ni los ajustes del teléfono. Lo vigila `SystemUntouchedTest`.

## 2. Inventario de permisos

| Permiso / capacidad | Fase | Tipo | Para qué | Alternativa descartada |
|---|---|---|---|---|
| `<queries>` MAIN/LAUNCHER | 1 | Visibilidad de paquetes | Listar apps abribles | `QUERY_ALL_PACKAGES` (no permitido para este caso en Play; innecesario) |
| `LauncherApps` (sin permiso) | 1 | API de sistema | Apps por perfil, íconos con insignia, eventos de instalación, atajos | — |
| `REQUEST_DELETE_PACKAGES` | 1 | Normal | Abrir el diálogo de desinstalar del sistema | — |
| `VIBRATE` | 1 | Normal | Háptica del alfabeto (si `performHapticFeedback` no basta) | — |
| `EXPAND_STATUS_BAR` | 1 | Normal | Gesto bajar = abrir panel de notificaciones | — |
| Selector de fotos del sistema (`PickVisualMedia`) y `ACTION_SET_WALLPAPER` | 0.6.0 (ADR-022) | **Sin permiso** | Foto propia como fondo del tema; abrir el selector de fondos del sistema | `READ_MEDIA_IMAGES` y `SET_WALLPAPER` (innecesarios) |
| `ACCESS_HIDDEN_PROFILES` | 2 (implementado en 0.9.0, ADR-033) | Normal (Android 15+) | Mostrar apps del espacio privado solo desbloqueado; bloquearlo/desbloquearlo desde el panel de ocultas (`requestQuietModeEnabled`, la credencial la pide el sistema) | No declararlo (espacio privado inaccesible desde el inicio) |
| `BIND_NOTIFICATION_LISTENER_SERVICE` (servicio) | 2 (implementado en 0.6.0, ADR-023) | Acceso especial, el usuario lo concede en Ajustes | Puntos de notificación, último aviso bajo favoritos y reproductor (sesiones de música con el mismo acceso) | — |
| `AppWidgetHost` + diálogo `ACTION_APPWIDGET_BIND` | 1 (adelantado, ADR-020) | Consentimiento del sistema por proveedor; **sin permiso en el manifiesto** | Widgets | `BIND_APPWIDGET` (solo para apps del sistema) |
| `BIND_ACCESSIBILITY_SERVICE` (servicio) | 4 (adelantado a 0.6.0) | Acceso especial | **Únicamente** bloquear pantalla con gesto (`GLOBAL_ACTION_LOCK_SCREEN`, API 28+) | Administrador de dispositivo (rompe huella), bajar timeout (altera ajustes) |
| `READ_CONTACTS` | 0.7.0 (ADR-025) | Peligroso, en tiempo de ejecución, opt-in tras aviso | Contactos en el buscador (solo el nombre) | Índice propio de contactos (copiaría datos) |
| `PACKAGE_USAGE_STATS` | 0.7.0 (ADR-024) | Acceso especial, el usuario lo concede en Ajustes | Tiempo de uso de hoy en el panel de cada app | — |
| `INTERNET` | — | — | **No se declara.** Si una función futura lo requiere: ADR + revisión completa | — |

Cualquier otro permiso requiere ADR.

## 3. Reglas por componente

### 3.1 Abrir apps
- Usar `LauncherApps.startMainActivity(component, user, sourceBounds, opts)` — respeta perfiles y valida que el componente sea abrible.
- Atajos: `LauncherApps.startShortcut(...)`. Nunca construir `Intent`s arbitrarios desde datos externos.
- Capturar `ActivityNotFoundException`/`SecurityException` (app suspendida, deshabilitada, perfil en pausa) y mostrar mensaje; si el perfil de trabajo está en pausa, ofrecer reactivarlo con la API del sistema.

### 3.2 Servicio de notificaciones
- Declarado con `android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"` y `exported="false"`: lo enlaza el sistema, ninguna otra app puede.
- Opt-in con aviso destacado en Ajustes antes de mandar a la pantalla del sistema. Con la función apagada se desconecta solo (`requestUnbind`, B-003).
- Nada a disco ni a logs: `NotificationsRepository` es un `StateFlow` en memoria. Para la música solo se leen título, artista y estado.
- Datos solo en memoria; se borran al desactivar la función o al morir el proceso.
- Respuestas rápidas: usar las `RemoteInput` de la propia notificación; nunca guardar el texto escrito.
- Respetar notificaciones de **pantalla de bloqueo/sensibles**: si `Notification.visibility == VISIBILITY_SECRET` o el perfil está bloqueado, mostrar solo "Nueva notificación".
- No mostrar notificaciones del espacio privado cuando está bloqueado.

### 3.3 Servicio de accesibilidad (Fase 4)
- `isAccessibilityTool="false"` (no somos herramienta de accesibilidad).
- Config mínima: sin `canRetrieveWindowContent`, `accessibilityEventTypes` vacío/mínimo; solo `performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)`.
- **Divulgación destacada dentro de la app** (no solo en la política de privacidad) antes de mandar al usuario a Ajustes; declaración en Play Console.
- Desactivado por defecto. Si el sistema lo apaga, Ajustes muestra "Falta el acceso" y el doble toque avisa una vez y abre Accesibilidad, sin insistir.
- Implementado en 0.6.0 (`LockAccessibilityService`, `exported="false"`, `res/xml/lock_service.xml`).

### 3.4 Widgets (Fase 3)
- `AppWidgetHost` con ID propio; liberar IDs de widgets eliminados (`deleteAppWidgetId`) para no acumular.
- Solicitar `ACTION_APPWIDGET_BIND` por widget; manejar `configure` del proveedor.

### 3.5 Componentes exportados
- Solo: `HomeActivity` (HOME y LAUNCHER, ADR-014) y servicios protegidos por permiso de sistema. `SettingsActivity` es `exported="false"`. Ningún `ContentProvider` ni `BroadcastReceiver` exportado sin protección.

### 3.6 `PendingIntent`
- `FLAG_IMMUTABLE` por defecto. Las respuestas inline de notificación usan los `PendingIntent` de la propia notificación (no creamos `MUTABLE` propios).

## 4. Datos que guarda la app

| Dato | Dónde | Sensible | Respaldo |
|---|---|---|---|
| Caché de apps (componente, nombre, orden) | Room | Bajo | No (se reconstruye) |
| Favoritos, ocultos, carpetas | Room | Medio (revela apps instaladas) | Sí, con Android Backup cifrado por el sistema |
| Preferencias | DataStore | Bajo | Sí |
| Íconos renderizados | Caché interna | Bajo | No |
| Foto de fondo elegida (reducida) y fondos anteriores (máx. 8; el usuario los borra en Personalizar → Fondo, y se borran con la app) | `files/backgrounds/` | Medio (foto personal) | **No** (excluida en `backup_rules`/`data_extraction_rules`) |
| Notificaciones | Solo memoria | **Alto** | Nunca |
| Carpetas (nombre y apps) | Room | Medio | Sí |
| Contactos y tiempo de uso | No se guardan: se consultan al momento | Alto | Nunca |
| Contadores de uso por app (para sugerencias / freno de uso) | Room | Medio | Opcional, desactivado por defecto |
| PIN de las apps ocultas (ADR-034) | DataStore propio `velora_lock`: solo hash PBKDF2 + sal + contador de intentos | Alto | **No** (excluido en `backup_rules`/`data_extraction_rules` y fuera del `.velora-backup`) |
| Apps del espacio privado (ADR-033) | Solo memoria, y solo desbloqueado | Alto | Nunca (no entran en la caché de apps) |

- `android:allowBackup="true"` con `dataExtractionRules`/`fullBackupContent` que **excluyen** cachés y bases de íconos.
- Respaldo manual completo (ADR-035, adelantado a 0.9.0): archivo `.velora-backup` de texto plano elegido por el usuario con el selector del sistema (SAF), sin permisos de almacenamiento. Lleva listas, widgets por re-agregar y preferencias; nunca notificaciones ni el PIN. Al leerlo se valida todo (tamaño, claves conocidas, nombres de paquete) y restaurar pide confirmación.

## 5. Propiedad intelectual

- Nombre, logo, íconos, textos y fondos de Velora son **propios**. No usar "Niagara", sus íconos ("Niagara Dots", "Anycons"), capturas, fondos ni frases de su ficha.
- No descompilar ni reutilizar código de Niagara.
- Código abierto de terceros: solo licencias permisivas (Apache-2.0, MIT, BSD) se pueden copiar con atribución. **GPL (Kvaesitso, Focus Launcher): solo leer para entender, no copiar**, salvo que se decida publicar Velora bajo GPL (ADR).
- Archivo `THIRD_PARTY_NOTICES.md` con las licencias de dependencias.

## 6. Google Play — checklist de políticas

- [ ] Target API vigente (36 desde 31 ago 2026).
- [ ] Sin `QUERY_ALL_PACKAGES`.
- [ ] Formulario de seguridad de datos: "no recopila ni comparte datos" (verdadero mientras no haya red).
- [ ] Declaración de accesibilidad (si se publica Fase 4) + video de demostración si lo piden.
- [ ] Política de privacidad pública (página simple en GitHub Pages) que liste permisos y que nada sale del teléfono.
- [ ] Ficha sin keywords de marcas ajenas ("Niagara").

## 7. Amenazas consideradas

| Amenaza | Mitigación |
|---|---|
| App maliciosa envía intents a componentes de Velora | Componentes no exportados; servicios protegidos por permiso de sistema |
| Fuga de notificaciones por logs/crash reports | Logger desactivado en release; sin reporte de crashes externo |
| Atajo/intent malicioso guardado en favoritos | Solo se guardan `ComponentName` + usuario + `shortcutId`; se resuelven siempre vía `LauncherApps` |
| Mostrar contenido privado en el inicio con teléfono prestado | Opción "ocultar contenido de notificaciones en inicio"; respeta visibilidad del sistema |
| Espacio privado expuesto | Contenedor separado, oculto por defecto cuando está bloqueado, sin búsqueda de sus apps estando bloqueado |
| Dependencia comprometida (supply chain) | Pocas dependencias, versiones fijadas, Dependabot, verificación de checksums de Gradle |
