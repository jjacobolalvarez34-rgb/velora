# Bugs — conocidos, prevenidos y registro

> Dos partes: (A) **trampas conocidas** de launchers Android que hay que prevenir desde el diseño; (B) **registro** de bugs reales del proyecto.
> Antes de trabajar en un área, busca aquí su etiqueta (`[ciclo-vida]`, `[oem]`, `[notif]`, etc.).

## Severidad

| Nivel | Significado |
|---|---|
| Crítico | No se puede abrir apps, crash en arranque, el usuario queda "atrapado" |
| Alto | Función principal rota o UI congelada; ocurre a menudo |
| Medio | Función secundaria rota o glitch visible frecuente |
| Bajo | Detalle visual, caso raro |

---

## A. Trampas conocidas (prevenir desde el diseño)

### B-001 · UI congelada tras desbloquear `[ciclo-vida]` `[oem]` — Alto
**Síntoma:** después de que otra app enciende la pantalla (llamada, asistente, automatización) y el usuario vuelve, el inicio responde a toques pero no se redibuja; se abren apps distintas a las que se ven. Reportado en launchers de terceros, incluido Niagara.
**Causa probable:** la Activity no recibe correctamente la señal de volver al frente (`onResume`/foco) por la secuencia de keyguard; Compose deja de recibir frames.
**Prevención:**
- Escuchar `onWindowFocusChanged(true)`, `onTopResumedActivityChanged(true)` (API 29+) y `ACTION_USER_PRESENT`; en cualquiera, invalidar la vista raíz (`window.decorView.invalidate()`) y re-emitir estado.
- No depender solo de `Lifecycle.State.RESUMED` para pausar el reloj/animaciones.
- Prueba manual §5 de VERIFICACIONES (llamada y asistente con pantalla apagada).

### B-002 · Servicios especiales que el sistema apaga `[oem]` `[notif]` `[a11y]` — Alto
**Síntoma:** accesibilidad o acceso a notificaciones se desactivan solos (Samsung, Xiaomi/Poco, Oppo).
**Prevención:**
- Detectar al reanudar si el servicio sigue habilitado (`NotificationManagerCompat.getEnabledListenerPackages`, `AccessibilityManager.getEnabledAccessibilityServiceList`).
- Si se perdió: ocultar la función y mostrar un aviso discreto con botón a Ajustes; nunca bucles de diálogos.
- Para notificaciones: `requestRebind(ComponentName)` cuando corresponda.
- Página de ayuda por fabricante (batería sin restricciones, autoarranque).

### B-003 · `requestUnbind()` antes de conectar `[notif]` — Medio
**Síntoma:** el listener sigue enlazado aunque la función esté apagada; insignias inconsistentes en perfil de trabajo tras reiniciar (visto en Launcher3 derivados).
**Prevención:** solo llamar `requestUnbind()` dentro o después de `onListenerConnected()`.

### B-004 · Rebind lento en algunos OEM `[notif]` `[oem]` — Bajo
**Síntoma:** tras reactivar el acceso, las notificaciones tardan ~10+ s en aparecer.
**Prevención:** mostrar estado "conectando…"; no asumir conexión inmediata.

### B-005 · El fabricante fuerza el modo oscuro `[oem]` `[ui]` — Medio
**Síntoma:** MIUI/HyperOS altera los colores de la app en modo oscuro.
**Prevención:** `android:forceDarkAllowed="false"` en el tema; gestionar oscuro propio con `isSystemInDarkTheme()`.

### B-006 · Botón flotante de accesibilidad (Android 12+) `[a11y]` — Bajo
**Síntoma:** aparece un botón de atajo de accesibilidad al activar el servicio.
**Prevención:** en la config del servicio no pedir `FLAG_REQUEST_ACCESSIBILITY_BUTTON`; en la ayuda explicar cómo apagar el atajo.

### B-007 · Servicio de accesibilidad no listado (MagicOS) `[a11y]` `[oem]` — Bajo
**Prevención:** detectar fabricante y explicar la ruta alternativa; la función es opcional.

### B-008 · Eventos de paquete perdidos con el proceso muerto `[datos]` — Alto
**Síntoma:** app instalada no aparece o app desinstalada sigue en la lista.
**Prevención:** `LauncherApps.Callback` + verificación de huella en `onResume` (ARQUITECTURA §4.1). Manejar `onPackagesSuspended/Unavailable` (SD, apps suspendidas por bienestar digital).

### B-009 · Orden alfabético incorrecto en español `[datos]` `[i18n]` — Medio
**Síntoma:** "Ñequi" al final, "Álbum" separado de la A, minúsculas después de mayúsculas.
**Prevención:** `Collator.getInstance(locale)` con `strength = PRIMARY`; letra de sección normalizada sin tildes excepto Ñ en `es`. Recalcular en cambio de idioma. Prueba unitaria con nombres: "álbum", "Banco", "ñequi", "Nequi", "Zoom", "123 Juego", "😀 App".

### B-010 · Back en API 36 `[ciclo-vida]` — Alto
**Síntoma:** con targetSdk 36 ya no llega `onBackPressed()`; el back puede cerrar o dejar estados inconsistentes.
**Prevención:** solo `BackHandler`/`PredictiveBackHandler` de Compose; en estado raíz no consumir ni terminar la Activity.

### B-011 · Recreación de la Activity = parpadeo `[ciclo-vida]` `[ui]` — Medio
**Prevención:** `configChanges` amplio (P-02); estado en ViewModel; nada de `recreate()`.

### B-012 · Insets edge-to-edge mal aplicados `[ui]` — Medio
**Síntoma:** favoritos tapados por la barra de navegación, gestos que chocan.
**Prevención:** `WindowInsets.safeDrawing`/`systemGestures`; probar con navegación por botones y por gestos.

### B-013 · Gestos propios que roban el gesto de "atrás" del sistema `[gestos]` — Alto
**Prevención:** el alfabeto está a ≥ 16 dp del borde o usa `systemGestureExclusion` solo en su área (máx. 200 dp por borde, límite del sistema). Probar navegación por gestos.

### B-014 · App que no se puede abrir (perfil en pausa, suspendida, deshabilitada) `[datos]` — Medio
**Prevención:** ver SEGURIDAD §3.1; mostrar estado gris y mensaje; nunca crash.

### B-015 · Fugas/consumo de memoria por íconos `[rendimiento]` — Medio
**Síntoma:** OOM o GC frecuente con muchos íconos o paquetes de íconos grandes.
**Prevención:** LruCache dimensionada, bitmaps al tamaño final, `onTrimMemory` (RENDIMIENTO §5).

### B-016 · Espacio privado visible o roto `[privacidad]` — Alto
**Prevención:** contenedor separado; ocultar contenidos y resultados de búsqueda cuando está bloqueado; manejar que sus apps se detienen al bloquear (sin notificaciones).

### B-017 · Abrir panel de notificaciones con gesto `[oem]` — Bajo
**Síntoma:** `expandNotificationsPanel` es API oculta; puede fallar en algunos OEM/versiones.
**Prevención:** envolver en `try/catch`; si falla, desactivar el gesto y avisar.

### B-018 · Atajos de app (`ShortcutInfo`) sin permiso `[datos]` — Bajo
**Síntoma:** `LauncherApps.getShortcuts` lanza `SecurityException` si Velora no es el launcher predeterminado.
**Prevención:** verificar `hasShortcutHostPermission()`; si no, ocultar atajos.

### B-019 · Widgets huérfanos `[widgets]` — Bajo
**Prevención:** liberar `appWidgetId` al quitar un widget o si el proveedor fue desinstalado.

---

## B. Registro de bugs del proyecto

Plantilla (copiar al final, numeración continúa desde B-100):

```
### B-1XX · Título corto `[etiquetas]` — Severidad — Estado (Abierto | En progreso | Resuelto | No reproducible)
**Dispositivo / Android / capa:** 
**Pasos para reproducir:**
1.
**Esperado:** 
**Actual:** 
**Causa raíz:** 
**Solución:** 
**Prueba que lo cubre:** (archivo y nombre del test)
**Commit/PR:** 
**Lección (si aplica → regla o check nuevo):** 
```

_(Sin bugs registrados todavía.)_
