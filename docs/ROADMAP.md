# Roadmap

> Fase actual: **Fase 0**. Cada fase cierra con la definición de "Hecho" de `VERIFICACIONES.md` §7.
> Marcar `[x]` al completar y anotar la fecha.

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

> **Siguiente sesión:** ver `PENDIENTES.md` §1. Primero el ícono que abre el inicio, luego la landing y la fuente Manrope, después la lista de apps.
- [ ] `AppsRepository` con `LauncherApps`, perfiles, caché Room y callbacks (B-008, B-009)
- [ ] `IconRepository` con caché memoria+disco
- [ ] Inicio: reloj/fecha + favoritos abajo
- [ ] Lista completa con secciones por letra
- [ ] **Alfabeto ola** (ESTILO §6) — el ítem más importante; iterar hasta que se sienta perfecto
- [ ] Búsqueda sin tildes
- [ ] Abrir apps con animación desde la fila
- [ ] Menú de pulsación larga: info, desinstalar, ocultar, favorito, atajos (B-018)
- [ ] Apps ocultas
- [ ] Ajustes: zurdo, íconos sí/no, tamaño de texto, reordenar favoritos
- [ ] Onboarding: elegir favoritos + "establecer como predeterminado"
- [ ] Back predictivo y `onNewIntent(HOME)` (B-010, B-011)
- [ ] Prevención B-001, B-005, B-012, B-013, B-014
- [ ] Baseline Profile de flujos críticos; presupuestos de RENDIMIENTO cumplidos
- [ ] Strings es/en

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
