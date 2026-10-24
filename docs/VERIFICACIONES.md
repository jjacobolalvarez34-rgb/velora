# Verificaciones

> Nada está "terminado" hasta pasar las secciones que apliquen. §1 aplica siempre.

## §1. Siempre (todo cambio)

- [ ] `gradlew ktlintCheck detekt lint` sin errores nuevos.
- [ ] `gradlew testDebugUnitTest` en verde.
- [ ] `gradlew assembleRelease` compila (R8 no rompe nada).
- [ ] Probado en teléfono real como launcher predeterminado.
- [ ] Prueba de humo (2 min):
  - [ ] Pulsar Inicio desde otra app → vuelve instantáneo, estado limpio.
  - [ ] Bloquear y desbloquear → la UI responde y se actualiza (ver B-001).
  - [ ] Girar el teléfono (si aplica) / cambiar modo oscuro → sin parpadeo ni recreación visible.
  - [ ] Abrir 3 apps desde favoritos y 3 desde la lista.
  - [ ] Instalar y desinstalar una app (ej. una calculadora) → aparece/desaparece sin reiniciar.
- [ ] Sin textos hardcodeados; sin colores/medidas sueltos.
- [ ] Docs actualizados si cambió comportamiento.

## §2. UI y animación

- [ ] Claro y oscuro. Wallpaper claro, oscuro y muy colorido (contraste legible).
- [ ] Modo zurdo espejado correctamente.
- [ ] Tamaño de fuente del sistema al 200 %: nada cortado.
- [ ] "Quitar animaciones" activado en Accesibilidad → funciona sin movimiento ni esperas.
- [ ] Animación interrumpible: tocar a mitad de animación no causa saltos.
- [ ] TalkBack: se puede navegar y abrir una app; el alfabeto tiene alternativa.
- [ ] Usa solo tokens de `ESTILO.md` (motion, color, tipografía, espaciado).
- [ ] Previews claro/oscuro presentes.
- [ ] Gestos del sistema (back, home, recientes) no chocan con gestos propios en los bordes (respetar `systemGestureExclusion` solo donde haga falta, máx. 200 dp verticales por borde).

## §3. Rendimiento

- [ ] Medido en **build release** en el dispositivo de referencia.
- [ ] Macrobenchmark de arranque y scroll sin regresión > 10 % (ver `RENDIMIENTO.md` §2).
- [ ] Layout Inspector: arrastrar alfabeto / hacer scroll no recompone filas sin cambios.
- [ ] StrictMode (debug) sin violaciones de disco/red en main thread.
- [ ] Sin asignaciones por frame en gestos (revisar con Memory Profiler si se tocó un gesto).
- [ ] Tamaño de APK reportado; no crece > 200 KB sin justificación.
- [ ] Baseline Profile regenerado si se tocó un flujo crítico.

## §4. Seguridad y privacidad

- [ ] Ningún permiso nuevo sin ADR y entrada en `SEGURIDAD.md`.
- [ ] Ningún `exported="true"` nuevo sin revisión (servicios protegidos por permiso de sistema).
- [ ] `PendingIntent` con `FLAG_IMMUTABLE` (salvo respuestas inline que exigen `MUTABLE`, documentadas).
- [ ] Nada de contenido de notificaciones en disco ni en logs.
- [ ] Sin dependencias de red/analítica.
- [ ] Función con permiso especial probada **con el permiso revocado** (modo degradado).

## §5. Matriz de dispositivos / OEM (antes de cada release y al tocar ciclo de vida, servicios o gestos)

| Capa | Ejemplo | Qué revisar especialmente |
|---|---|---|
| One UI (Samsung) | Galaxy A | Navegación por gestos, "apps en suspensión", listener de notificaciones |
| HyperOS / MIUI (Xiaomi, Redmi, Poco) | Redmi Note | Permiso "mostrar en pantalla de bloqueo"/autoarranque, forzado de modo oscuro, ahorro de batería |
| Moto (Motorola) | Moto G | Cercano a AOSP; buena referencia |
| Pixel / AOSP | Emulador API 36 | Back predictivo, espacio privado |
| ColorOS / Realme / Oppo | Si hay acceso | Servicios que se apagan solos |
| Honor MagicOS | Si hay acceso | Accesibilidad no listada |

Casos a probar en cada uno:
- [ ] Establecer como predeterminado y volver al original.
- [ ] Llamada entrante con pantalla apagada → colgar → inicio responde (B-001).
- [ ] Asistente de Google con pantalla bloqueada → volver → inicio responde.
- [ ] Reiniciar el teléfono → Velora arranca, favoritos intactos.
- [ ] Perfil de trabajo (si se puede crear con "Test DPC" o "Island"): apps con insignia, abren en su perfil.
- [ ] Android 15+: espacio privado bloquear/desbloquear (Fase 2+).

## §6. Release

- [ ] §1–§5 completos.
- [ ] `versionCode` incrementado; changelog es/en.
- [ ] Baseline Profile regenerado e incluido.
- [ ] Tabla de métricas vs versión anterior.
- [ ] Probado actualizar desde la versión anterior (migraciones de Room y DataStore sin perder favoritos).
- [ ] Probado instalación limpia (onboarding completo).
- [ ] Política de privacidad y declaraciones de Play al día con los permisos usados.
- [ ] Keystore respaldado; build firmado verificado con `apksigner verify`.
- [ ] Prueba en pista cerrada ≥ 3 días sin crashes nuevos (Play Console → Android vitals).

## §7. Definición de "Hecho" para una fase del Roadmap

- Todas las funciones de la fase cumplen §1–§4.
- Usado como launcher principal por el autor ≥ 7 días sin volver al launcher del fabricante.
- Sin bugs `Crítico` o `Alto` abiertos en `BUGS.md`.
