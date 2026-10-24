# Roadmap

> Fase actual: **Fase 0**. Cada fase cierra con la definición de "Hecho" de `VERIFICACIONES.md` §7.
> Marcar `[x]` al completar y anotar la fecha. `[~]` = código hecho, falta verificar en dispositivo.

## Fase 0 — Cimientos (1–2 semanas)
- [x] Proyecto creado con módulos y `libs.versions.toml` (P-01) — 2026-10-23
- [x] Manifiesto de launcher (P-02) — 2026-10-23. _Falta comprobar en teléfono que se puede elegir como inicio._
- [x] Tema base, tokens de color/tipo/espaciado/motion en `core/designsystem` — 2026-10-23
- [x] detekt + ktlint + CI básico (P-06) — 2026-10-23. _Workflow escrito; falta verlo en verde en GitHub._
- [x] Módulos `:baselineprofile` y `:macrobenchmark` con un benchmark de arranque — 2026-10-23. _Falta la primera medición en dispositivo._
- [x] StrictMode y LeakCanary en debug — 2026-10-23
- [x] Ícono adaptativo desde el logo (`docs/marca/logo-velora.jpg`), con capa monocromática — 2026-10-23
- [x] Salida de emergencia: mantener presionado el inicio → Ajustes → "App de inicio" — 2026-10-23

**Salida:** una pantalla vacía con el wallpaper y el reloj, instalable como launcher, con CI en verde y primera medición de arranque.

## Fase 1 — MVP usable (4–6 semanas)

> 2026-10-24: la 0.1.0 se probó en el teléfono del autor y todo funcionó (`PENDIENTES.md` §1). Los ítems probados pasan a `[x]`. La fase cierra tras 7 días de uso como launcher principal (VERIFICACIONES §7).

- [~] `AppsRepository` con `LauncherApps`, perfiles y callbacks (B-008, B-009). _Falta: caché de apps en Room para el primer frame._
- [~] `IconRepository` con caché en memoria. _Falta: caché en disco._
- [x] Inicio: reloj/fecha + favoritos abajo (Room) — 2026-10-24
- [x] Lista completa con secciones por letra — 2026-10-24
- [x] **Alfabeto ola** (ESTILO §6), primera versión (2026-10-24): un solo `Canvas`, háptica por letra, alternativa con TalkBack. _Iterar en el teléfono hasta que se sienta perfecto._
- [x] Búsqueda sin tildes — 2026-10-24
- [x] Abrir apps con animación desde la fila — 2026-10-24
- [~] Menú de pulsación larga: atajos (B-018), favorito, ocultar, info, desinstalar
- [~] Apps ocultas (recuperables en Ajustes)
- [ ] Ajustes: zurdo, íconos sí/no, tamaño de texto, reordenar favoritos, fuente del sistema
- [~] Onboarding: landing con "establecer como predeterminado". _Falta: elegir favoritos._
- [~] Back y `onNewIntent(HOME)` (B-010, B-011). _Falta: animación de back predictivo._
- [~] Prevención B-001, B-005, B-012, B-013, B-014
- [ ] Baseline Profile de flujos críticos; presupuestos de RENDIMIENTO cumplidos
- [x] Strings es/en — 2026-10-24
- [x] ~~Fuente Manrope (ADR-015)~~ — reemplazada por el Tema (ADR-016)
- [~] Tema: fuentes (ADR-016), relojes Clásico/Centrado/Día, íconos Círculo/Sistema/Minimalista/Sin íconos
- [~] Recién instaladas y buscador limpio con animaciones
- [x] El ícono abre el inicio (ADR-014) — 2026-10-24

**Salida:** usarlo 7 días como launcher principal.

## Fase 2 — Contexto (3–4 semanas)
- [ ] Listener de notificaciones opt-in + pantalla de explicación (SEGURIDAD §3.2, B-002/B-003/B-004)
- [ ] Resumen de notificaciones en inicio + punto por app
- [ ] Respuesta rápida
- [ ] Reproductor de medios contextual (`MediaSessionManager` vía listener)
- [ ] Apps recién instaladas
- [ ] Espacio privado Android 15+ (B-016)

## Fase 3 — Organización (4–6 semanas)
- [ ] Pop-ups por deslizamiento (atajos, notificaciones, apps relacionadas)
- [ ] Carpetas
- [ ] Widgets con `AppWidgetHost` (B-019)
- [ ] Paquetes de íconos de terceros; formas de ícono
- [ ] Fuente opcional (descargable)

## Fase 4 — Bienestar y robustez (3–4 semanas)
- [ ] Bloqueo con doble toque vía accesibilidad opt-in (SEGURIDAD §3.3, B-006/B-007)
- [ ] Freno de uso por app (local)
- [ ] Respaldo con Android Backup + exportar/importar JSON
- [ ] Página de ayuda por fabricante

## Fase 5 — Publicación y sostenibilidad
- [ ] Nombre definitivo, ícono, ficha de Play es/en, política de privacidad
- [ ] Pista cerrada con 12+ probadores durante 14 días (requisito actual de Play para cuentas personales nuevas — verificar al momento)
- [ ] Pro: pago único (y anual opcional) con Google Play Billing; qué es Pro se decide con ADR. **Nunca anuncios.**
- [ ] Lanzamiento

## Ideas en espera (no comprometidas)
- Calculadora en la búsqueda · Contactos en la búsqueda · Agenda/clima (requiere red → ADR de `INTERNET`) · Modo plegable de dos columnas · Temas.
