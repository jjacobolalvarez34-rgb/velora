# Pendientes

> Lista viva de lo que falta, en orden. Al terminar un punto: marcarlo `[x]` con fecha y moverlo a `ROADMAP.md` si corresponde.
> `[~]` = código hecho y probado en la JVM; falta verlo en el teléfono.
> Última actualización: 2026-10-23.

## 1. Probar en el teléfono (lo primero)

Todo lo de §2 está implementado pero nadie lo ha visto funcionar en un teléfono real. Instalar `Velora-0.1.0.apk` y revisar:

- [ ] Tocar el ícono de Velora → aparece la **landing** (si no es el inicio) → "Usar Velora como inicio" abre el diálogo del sistema.
- [ ] "Ahora no" → inicio con reloj, fecha y la pista "Desliza hacia arriba…".
- [ ] Deslizar hacia arriba abre la lista **siguiendo el dedo**; soltar a medias decide abrir/cerrar según la velocidad.
- [ ] **Alfabeto ola**: arrastrar el pulgar por las letras; la ola sigue al dedo, vibra al cambiar de letra, la lista salta y las otras letras se atenúan. Ajustar `WaveAmplitude`, `WAVE_SIGMA_ROWS` y la altura (`ALPHABET_HEIGHT_FRACTION`) hasta que se sienta perfecto.
- [ ] Buscar "camara" encuentra "Cámara"; Enter abre el primer resultado; el teclado no tapa el buscador.
- [ ] Abrir una app: crece desde su fila. Volver con Inicio: la lista ya está cerrada, sin animación.
- [ ] Mantener presionada una app → "Agregar a favoritos" → aparece abajo en el inicio. Quitarla. Información de la app. Desinstalar.
- [ ] Atrás cierra la búsqueda y luego la lista; en el inicio no hace nada.
- [ ] Navegación por gestos: arrastrar el alfabeto no dispara el gesto "atrás" (B-013).
- [ ] Instalar/desinstalar una app con Velora abierto → la lista se actualiza sola.
- [ ] Bloquear/desbloquear; llamada con pantalla apagada (B-001).
- [ ] Perfil de trabajo (si hay): apps con insignia que abren en su perfil.

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

- [ ] Ajustar el alfabeto ola tras probarlo (ver §1).
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
