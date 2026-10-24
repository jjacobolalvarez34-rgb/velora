# Pendientes

> Lista viva de lo que falta, en orden. Al terminar un punto: marcarlo `[x]` con fecha y moverlo a `ROADMAP.md` si corresponde.
> Última actualización: 2026-10-23.

## 1. Pedidos del autor (siguiente sesión, en este orden)

- [ ] **Tocar el ícono abre el inicio.** Hoy el ícono del cajón abre "Ajustes de Velora".
  - `HomeActivity` también con `<category android:name="android.intent.category.LAUNCHER" />` (etiqueta "Velora").
  - `SettingsActivity` sin `LAUNCHER` y con `exported="false"`. Se abre manteniendo presionado el inicio y desde la landing.
  - ADR-014; actualizar `PROCEDIMIENTOS.md` P-02 y `SEGURIDAD.md` §3.5.
- [ ] **Landing de bienvenida** antes del inicio.
  - Se muestra solo si Velora **no** es el inicio predeterminado. En API 29+: `RoleManager.isRoleHeld(ROLE_HOME)`. Antes de 29: `resolveActivity` del intent HOME, en `Dispatchers.IO`.
  - Fondo negro de marca (igual al del logo), logo, "Velora", una frase corta (ej. "Tu inicio, en calma y a una mano.").
  - Botón principal **"Usar Velora como inicio"** → `RoleManager.createRequestRoleIntent(ROLE_HOME)`; respaldo `Settings.ACTION_HOME_SETTINGS`.
  - Botón secundario **"Ahora no"** → va al inicio; se recuerda mientras viva el proceso.
  - Lógica en `core:data` (`DefaultHomeRepository`); se revisa de nuevo en `onResume`.
  - Entrada con `StaggeredEntrance`; textos en `strings.xml` es/en.
- [ ] **Fuente elegante: Manrope** (ver §2).
- [ ] **Lista de apps (Fase 1)**, para que el inicio sirva de verdad (ver §3).

## 2. Fuente — decisión propuesta: Manrope

| | |
|---|---|
| Familia | **Manrope** (sans geométrica-humanista), licencia SIL OFL 1.1 |
| Por qué | Elegante y calmada (encaja con "agua tranquila" y la cinta plateada del logo); muy legible a 13 sp (alfabeto) y preciosa en pesos finos a 56 sp (reloj); soporta español completo (á, ñ, ü, ¿, ¡) |
| Pesos | ExtraLight 200 → `clock` · Regular 400 → `date`, `favorite`, `appRow`, `caption` · Medium 500 → `letter` · SemiBold 600 → `letterActive` |
| Cómo se incluye | **Empaquetada** en `core/designsystem/src/main/res/font/` (subconjunto latino, ~60–90 KB), no descargable: una fuente que llega tarde hace que el texto "salte" en el primer frame del launcher |
| Respaldo | Fuente del sistema, si falla la carga |
| Opción en ajustes | "Fuente del sistema" para quien la prefiera (Fase 1, Ajustes) |

**Cambia:** `ESTILO.md` §3 (hoy dice "fuente del sistema; propia opcional y descargable en Fase 3"); necesita un ADR nuevo y `THIRD_PARTY_NOTICES.md` con la licencia OFL. El presupuesto de APK (8 MB) lo aguanta: hoy pesa 980 KB.
**Alternativas, si no convence:** Outfit (más redonda), Urbanist (más fina), Sora (más técnica).

## 3. Fase 1 — lista de apps y lo que viene con ella

- [ ] `AppsRepository`: `LauncherApps.getProfiles()` + `getActivityList()` en IO, `LauncherApps.Callback`, refresco en `onResume` (B-008) y al cambiar el idioma.
- [ ] Orden y secciones con `Collator` (`PRIMARY`); Ñ como letra propia en español; `#` para números y símbolos. Prueba B-009 con: "álbum", "Banco", "ñequi", "Nequi", "Zoom", "123 Juego", "😀 App".
- [ ] `IconRepository`: `LruCache` (1/8 de la memoria), `getBadgedIcon` renderizado en `Default` al tamaño final; liberar íconos de la lista en `onTrimMemory`.
- [ ] Cajón: lista con encabezados por letra; se abre deslizando hacia arriba (sigue al dedo y suelta con resorte); se cierra con atrás, Inicio (`onNewIntent`) o deslizando hacia abajo desde arriba.
- [ ] **Alfabeto ola** (ESTILO §6), en un solo `Canvas`, con 0 recomposiciones al arrastrar; salto con `requestScrollToItem`; háptica al cambiar de letra; fuera de la zona de gestos del sistema (B-013); alternativa con botones para TalkBack.
- [ ] Búsqueda sin tildes ("camara" → "Cámara"), abajo, en la zona del pulgar; Enter abre el primer resultado.
- [ ] Abrir apps con `LauncherApps.startMainActivity` + `makeClipRevealAnimation` desde la fila; errores (perfil en pausa, app suspendida) con mensaje amable (B-014).
- [ ] Menú de pulsación larga: agregar/quitar favorito, información de la app, desinstalar (`REQUEST_DELETE_PACKAGES`, ya en el inventario de SEGURIDAD).
- [ ] Favoritos fijados abajo en el inicio, guardados en Room (KSP 2.3.12 ya funciona con AGP 9 y su Kotlin integrado; Room 2.8.5).
- [ ] Después: apps ocultas, atajos (B-018), ajustes (zurdo, íconos sí/no, tamaño de texto, reordenar favoritos), caché de apps en Room, Baseline Profile de los flujos nuevos.

## 4. Cierre de la Fase 0 (falta verificar)

- [ ] Probar en el teléfono como inicio predeterminado: bloquear/desbloquear, llamada con la pantalla apagada (B-001).
- [ ] Crear el repositorio en GitHub y ver el CI en verde (`.github/workflows/ci.yml`).
- [ ] Primera medición de arranque con `:macrobenchmark` (necesita un dispositivo conectado).
- [ ] Borrar la carpeta vacía `cascada/` que dejó el renombre (Windows la tenía bloqueada con VS Code abierto).
- [ ] Primer commit: `chore: estructura inicial y documentación`.
