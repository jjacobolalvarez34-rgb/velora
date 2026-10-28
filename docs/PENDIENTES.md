# Pendientes

> Lista viva de lo que falta, en orden. Al terminar un punto: marcarlo `[x]` con fecha y moverlo a `ROADMAP.md` si corresponde.
> `[~]` = código hecho y probado en la JVM; falta verlo en el teléfono.
> Última actualización: 2026-10-27 (0.7.0).

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
- [~] Atrás predictivo al cerrar la lista (0.7.0).
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
- [x] Si Velora ya es el inicio, la última página dice "Empezar" — 2026-10-25

## 6. Pedidos del 2026-10-25

- [~] Íconos al instante: caché en disco, precarga y control de versiones (B-100).
- [~] Burbuja con la letra junto al dedo en el alfabeto.
- [~] Estética más fina: íconos y textos más chicos, sombra suave en vez de velo pesado.
- [~] Temas completos: Tu fondo, Sakura, Bosque, Noche pixel, Aurora (ADR-018), con miniaturas en Ajustes.
- [x] "Empezar" en la introducción cuando Velora ya es el inicio.
- [~] Más temas: Atardecer, Océano, Nieve y Café (0.7.0). A futuro: y un fondo animado opcional que respete batería y "Quitar animaciones".

## 7. Pedidos del 2026-10-26 (fotos de referencia)

- [~] Ola grande (media pantalla) y burbuja blanca.
- [~] Reloj delgado y sencillo, más abajo; estilos sin adornos forzados.
- [~] Fuente Nunito por defecto.
- [~] Íconos minimalistas = logo en línea blanco en un círculo translúcido (no "un solo color azul").
- [~] Temas más sencillos; nuevos Noche y Papel.
- [~] Ajustes en tarjetas con íconos; menú de app en panel con íconos (ADR-019).
- [~] Widgets (ADR-020).
- [~] Calculadora y "Buscar en la web" en el buscador.

### Funciones de Niagara que Velora aún no tiene
- [~] **Notificaciones en el inicio**: punto en cada app y último aviso bajo los favoritos (ADR-023, 0.6.0); leerlas, abrirlas, descartarlas y responder desde el panel de la app (0.7.0).
- [~] **Reproductor de música** que aparece solo cuando algo suena (0.6.0, mismo acceso).
- [~] **Deslizar sobre una app** hacia el centro abre su panel con atajos y sus notificaciones (0.7.0).
- [~] **Carpetas** (0.7.0, ADR-026): "Añadir a carpeta", arriba de los favoritos, renombrar y eliminar.
- [~] **Doble toque para bloquear** (accesibilidad opt-in, adelantado a 0.6.0).
- [~] **Tiempo de uso** de hoy en el panel de cada app (0.7.0) y **freno de uso** (0.8.0, ADR-024).
- [~] Búsqueda de **contactos** (0.7.0, ADR-025).
- [~] Favoritos en el alfabeto: la estrella arriba de la primera letra (0.6.0). Falta: acceso a ajustes al final.
- [~] Widgets: reordenar, redimensionar arrastrando y quitar desde el inicio (0.7.0).
- [~] **Quitar widgets desde el inicio** (hecho en 0.7.0: mantener presionado → "Quitar widget" en rojo; pedido 2026-10-26): hoy solo se puede en Ajustes → Widgets → "Quitar". Idea: mantener presionado un widget → panel desde abajo con "Quitar", "Más alto/Más bajo" y "Mover". Al quitar, liberar el `appWidgetId` (B-019). Hay que ver que mantener presionado no choque con los widgets que usan su propio toque largo.

## 7d. Pedidos del 2026-10-28 — 0.8.0

- [~] Personalizar rediseñado: vista previa en vivo y pestañas Tema · Fondo · Reloj · Letra · Íconos · Color · Elementos (ADR-028).
- [~] Íconos de Velora con 8 diseños, 4 formas, 3 fuentes de color, grosor y relleno suave; packs de terceros (ADR-030).
- [~] Fondos procedurales con diseño (texturas, grano fino) y temas Mármol, Mármol negro y Grafito.
- [~] Relojes Apilado, Banda y Dúo; color del tema; elementos visibles (reloj, fecha, barra de estado, consejos).
- [~] Consejos de inicio deslizables (como notificaciones de los primeros días).
- [~] El buscador solo aparece con las recientes (no estorba en la lista por letra ni bajando por la lista completa).
- [~] Entrada de los íconos al volver de una app y al desbloquear (ADR-029).
- [~] Logo vectorial animado y landing con luces (ADR-029).
- [~] Freno de uso: límite diario por app desde su panel; al pasarlo, se pregunta antes de abrir.
- [ ] Acceso a Ajustes al final del alfabeto (hoy: mantener presionado el inicio).
- [ ] Baseline Profile y Macrobenchmark de los flujos nuevos: necesitan un teléfono conectado.

## 7c. Pedidos del 2026-10-27 — 0.7.0

- [~] Noche pixel con más definición: cielo en tres tonos, nebulosa tramada, estrellas en cruz, luna escalonada con cráteres, montañas de píxeles y letras con sombra dura (sin desenfoque).
- [~] Temas nuevos: Galaxia, Neón (texto que brilla), Matcha y Lavanda; antes Atardecer, Océano, Nieve y Café.
- [~] Compartir temas de Velora: guardar en `.velora-theme` y abrir uno compartido (ADR-027).
- [~] **Packs de íconos de terceros** (0.8.0, ADR-030).
- Aclaración: no se importan temas de Niagara (ADR-027): su formato es propio y los recursos tienen dueño.

## 7b. Pedidos del 2026-10-26 (tarde) — 0.6.0

- [~] Foto propia como fondo de cualquier tema y botón para cambiar el fondo del teléfono (ADR-022).
- [~] Tema **Ceniza** (fantasía oscura propia: hoguera, brasas, Cinzel). No usa nada de Dark Souls (marca registrada).
- [~] Íconos minimalistas iguales para todas las apps: círculo + glifo de línea propio (Tabler Icons, ADR-021) o la inicial.
- [~] La ola sigue al dedo: poco movimiento, poca ola.
- Aclaración: la pantalla de bloqueo del sistema **no se puede reemplazar** desde un launcher (Android no se lo permite a apps normales). Lo que sí se puede, el doble toque para bloquear, ya está.

## 8. Cierre de la Fase 0 (falta verificar)

- [ ] Crear el repositorio en GitHub y ver el CI en verde (`.github/workflows/ci.yml`).
- [ ] Primera medición de arranque con `:macrobenchmark` (necesita un dispositivo conectado).
- [ ] Borrar la carpeta vacía `cascada/` (cerrar VS Code primero: la tenía bloqueada).
