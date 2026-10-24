# Pendientes

> Lista viva de lo que falta, en orden. Al terminar un punto: marcarlo `[x]` con fecha y moverlo a `ROADMAP.md` si corresponde.
> `[~]` = código hecho y probado en la JVM; falta verlo en el teléfono.
> Última actualización: 2026-10-24 (tarde).

## 1. Prueba en el teléfono de la 0.1.0

> 2026-10-24: el autor instaló `Velora-0.1.0.apk` y reporta que **todo funcionó**.

- [x] Ícono → landing → "Usar Velora como inicio" / "Ahora no" — 2026-10-24
- [x] Lista que sigue al dedo, alfabeto ola, búsqueda sin tildes — 2026-10-24
- [x] Abrir apps desde la fila, volver con Inicio, atrás — 2026-10-24
- [x] Menú: favoritos, información, desinstalar — 2026-10-24
- [ ] Casos de borde por confirmar (VERIFICACIONES §5): llamada entrante con la pantalla apagada → colgar → el inicio responde (B-001); perfil de trabajo, si hay; instalar/desinstalar una app con Velora abierto; navegación por gestos junto al alfabeto (B-013).
- [ ] Afinar el alfabeto ola si algo se siente raro: `WaveAmplitude`, `WAVE_SIGMA_ROWS`, `ALPHABET_HEIGHT_FRACTION`.

## 2. Hecho en esta sesión (2026-10-23)

- [~] El ícono abre el inicio; ajustes internos (ADR-014).
- [~] Landing de bienvenida con `RoleManager` / `ACTION_HOME_SETTINGS`; "Ahora no" se recuerda mientras viva el proceso.
- [~] Fuente **Manrope** variable empaquetada (ADR-015).
- [~] Lista de apps con `LauncherApps` (todos los perfiles, callbacks, refresco al volver), orden con `Collator` (B-009, con pruebas), secciones con Ñ propia.
- [~] Íconos en memoria (`LruCache`, 1/8 de la memoria), renderizados en `Default`, liberados en `onTrimMemory`.
- [~] Cajón que sigue al dedo, se cierra con atrás, Inicio o deslizando hacia abajo desde arriba.
- [~] Alfabeto ola en un solo `Canvas` (0 recomposiciones al arrastrar), háptica, salto con `requestScrollToItem`, fuera de la zona de gestos (B-013), alternativa TalkBack.
- [~] Búsqueda sin tildes con ranking (empieza por > palabra > contiene).
- [~] Abrir con `makeClipRevealAnimation`; errores amables (perfil en pausa, app no disponible).
- [~] Menú: favorito, información, desinstalar (`REQUEST_DELETE_PACKAGES`).
- [~] Favoritos en Room (máximo 12), abajo en el inicio.

## 3. Siguiente (Fase 1, en orden)

- [~] Caché de apps (`apps_cache.db`, no se respalda): al arrancar la lista sale de ahí y luego se reconcilia con el sistema; los íconos esperan a esa reconciliación.
- [~] Apps ocultas: "Ocultar" en el menú; se recuperan en Ajustes → Apps ocultas (Room v2, migración automática).
- [~] Atajos de la app arriba del menú (`LauncherApps.getShortcuts`, B-018; solo si Velora es el inicio predeterminado).
- [~] Ajustes → Diseño: modo zurdo (todo el inicio espejado) y tamaño de texto (pequeño/normal/grande); Ajustes → Favoritos: subir y bajar.
- [ ] Onboarding: elegir favoritos la primera vez.
- [ ] Animación de back predictivo al cerrar la lista.
- [ ] Baseline Profile de los flujos nuevos (abrir lista, arrastrar alfabeto, buscar) y medir los presupuestos de RENDIMIENTO §2.
- [ ] Caché de íconos en disco.

## 4. Pedidos tras comparar con Niagara (2026-10-24)

- [~] Íconos redondos por defecto; estilos Círculo / Sistema / Minimalista / Sin íconos.
- [~] Fuente nueva por defecto (Plus Jakarta Sans) y elegible: Figtree, Fraunces, Quicksand, sistema (ADR-016).
- [~] Ajustes → Tema con vista previa en vivo; relojes Clásico / Centrado / Día (con batería).
- [~] "Recién instaladas" arriba de la lista y en el buscador vacío.
- [~] Buscador que se esconde al bajar; modo búsqueda limpio (la lista se va con fundido); resultados animados.
- [ ] **Widgets** (Fase 3, se puede adelantar). Plan:
  1. Una zona de widgets en el inicio, debajo del reloj, con altura ajustable.
  2. Mantener presionado el inicio → "Agregar widget" → selector propio con las vistas previas (`AppWidgetManager.getInstalledProvidersForProfile`, todos los perfiles).
  3. `AppWidgetHost` propio: `allocateAppWidgetId` → `bindAppWidgetIdIfAllowed`. Si no se permite, el diálogo del sistema `ACTION_APPWIDGET_BIND` (solo la primera vez por proveedor) → la actividad de configuración del widget, si la tiene.
  4. Se dibuja con `AppWidgetHostView` dentro de Compose (`AndroidView`). `startListening` en `onStart` y `stopListening` en `onStop`, para no gastar batería con el inicio oculto.
  5. Se redimensiona arrastrando un borde y se guarda en Room (id, proveedor, tamaño, orden).
  6. Quitar un widget o desinstalar su app libera el `appWidgetId` (B-019).
  7. Riesgos: widgets pesados de algunos fabricantes, y que los widgets son Views, no Compose. Hay que medir el scroll con Macrobenchmark antes de darlo por bueno.
- [ ] Carpetas / agrupar apps (como "Tareas"): Fase 3.

## 5. Pedidos del 2026-10-24 (noche)

- [~] Lista por letra por defecto: solo las apps de la letra elegida; se cambia con el alfabeto. Lista completa como opción.
- [~] Landing mejorada con logo animado (entrada, flotación, brillo).
- [~] Introducción de 5 páginas con demos animadas, una sola vez.
- [ ] Si Velora ya es el inicio, la última página de la introducción debería decir "Empezar" en vez de "Usar Velora como inicio".

## 6. Cierre de la Fase 0 (falta verificar)

- [ ] Crear el repositorio en GitHub y ver el CI en verde (`.github/workflows/ci.yml`).
- [ ] Primera medición de arranque con `:macrobenchmark` (necesita un dispositivo conectado).
- [ ] Borrar la carpeta vacía `cascada/` (cerrar VS Code primero: la tenía bloqueada).
