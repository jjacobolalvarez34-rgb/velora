# Pendientes

> Lista viva de lo que falta, en orden. Al terminar un punto: marcarlo `[x]` con fecha y moverlo a `ROADMAP.md` si corresponde.
> `[~]` = código hecho y probado en la JVM; falta verlo en el teléfono.
> Última actualización: 2026-10-24.

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

- [ ] Caché de apps en Room para que la lista y los favoritos estén en el **primer frame** del arranque en frío (hoy llegan unos ms después).
- [ ] Apps ocultas (menú + lista recuperable en Ajustes).
- [ ] Atajos de la app en el menú (`LauncherApps.getShortcuts`, B-018).
- [ ] Ajustes: modo zurdo (espejar alfabeto), íconos sí/no, tamaño de texto, reordenar favoritos, "Fuente del sistema".
- [ ] Onboarding: elegir favoritos la primera vez.
- [ ] Animación de back predictivo al cerrar la lista.
- [ ] Baseline Profile de los flujos nuevos (abrir lista, arrastrar alfabeto, buscar) y medir los presupuestos de RENDIMIENTO §2.
- [ ] Caché de íconos en disco.

## 4. Cierre de la Fase 0 (falta verificar)

- [ ] Crear el repositorio en GitHub y ver el CI en verde (`.github/workflows/ci.yml`).
- [ ] Primera medición de arranque con `:macrobenchmark` (necesita un dispositivo conectado).
- [ ] Borrar la carpeta vacía `cascada/` (cerrar VS Code primero: la tenía bloqueada).
