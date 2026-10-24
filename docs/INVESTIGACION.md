# Investigación — Niagara Launcher y el terreno técnico

> Fecha: 23 oct 2026. Resumen propio de fuentes públicas (enlaces al final).
> Propósito: entender qué hace bien Niagara para construir **algo propio** del mismo tipo, no un clon.

## 1. Qué es Niagara Launcher

- Launcher para Android (paquete `bitpit.launcher`), creado en 2016 como proyecto de hobby por Peter Huber; hoy lo desarrolla un estudio indie pequeño (Mellowdrop), autofinanciado, de unas seis personas.
- Cifras públicas recientes: más de 10–13 millones de descargas, alrededor de 500 mil usuarios activos diarios, calificación ~4,7–4,8 en Google Play.
- Solo existe en Android: iOS no permite reemplazar la pantalla de inicio.

## 2. La idea central (lo que lo hace distinto)

| Concepto | Qué significa | Lección para Velora |
|---|---|---|
| **Lista en vez de cuadrícula** | Una sola pantalla de inicio con favoritos en lista vertical; recomiendan ~8 favoritos para que quepan sin scroll | Una sola pantalla. Favoritos limitados por diseño |
| **Alfabeto ola ("wave alphabet")** | Barra de letras a la derecha; al arrastrar el pulgar, las letras se desplazan en forma de ola y la lista salta a esa inicial | Es la firma del producto: debe sentirse perfecto. Diseñar uno propio (ver ESTILO §6) |
| **Lista adaptativa** | El reproductor de música, mensajes o eventos aparecen en la lista solo cuando hacen falta | Elementos contextuales que entran y salen con animación |
| **Notificaciones integradas** | Se leen y responden desde el inicio, no solo puntitos | Requiere `NotificationListenerService` (permiso especial, opt-in) |
| **Una mano** | Todo alcanzable con el pulgar en teléfonos altos y plegables | Zona de toque en el tercio inferior; nada crítico arriba |
| **Enfoque / bienestar** | Menos íconos, menos colores, menos distracción; agregaron "Usage Breaker" | Minimalismo como función, no solo estética |
| **Ligero y fluido** | Lo venden como rápido y suave en cualquier teléfono | Presupuestos de rendimiento medidos (RENDIMIENTO.md) |
| **Material You** | Se tematiza con el fondo de pantalla | Color dinámico desde el día 1 |
| **Sin anuncios** | Ni en la versión gratis | Igual. Ver modelo de negocio |

## 3. Funciones (gratis + Pro), según su ayuda y prensa

- Gratis: lista y alfabeto ola, búsqueda, notificaciones integradas, apps ocultas, lista de apps instaladas recientemente, respaldo de ajustes (integrado con el respaldo de Android).
- Pro (suscripción anual ~USD 10 o pago único ~USD 30; varía por país): agenda de calendario y clima, "swipe actions" / pop-ups (deslizar sobre una app para ver apps relacionadas, atajos, widgets), fuentes propias, paquete de íconos minimalista, gesto de bloqueo rápido, ocultar barra de estado, categoría de música automática, integración con Sesame (búsqueda), Usage Breaker, Theme Studio.
- Otras: carpetas integradas con pop-ups, widgets (varios), calculadora integrada, modo zurdo, soporte de plegables, doble toque para dormir, botón "Niagara" con acción personalizable, íconos adaptativos propios ("Anycons").

## 4. Problemas reales que reportan sus usuarios (oro para nosotros)

Tomado de su tablero público de bugs. Cada uno está convertido en prevención en `BUGS.md`.

1. **UI congelada tras desbloquear**: si otra app enciende la pantalla (asistente, llamada, Tasker), el launcher no se entera de que volvió al frente; sigue respondiendo a toques pero sin refrescar, y se abren apps equivocadas.
2. **Servicio de accesibilidad que se apaga solo** en Oppo, Samsung, Xiaomi/Poco; el usuario tiene que reactivarlo una y otra vez.
3. **En Honor/MagicOS el launcher no aparece** en la lista de servicios de accesibilidad.
4. **En Android 12 aparece un botón flotante** de accesibilidad no deseado al activar el servicio.
5. **MIUI altera la paleta del modo oscuro** del launcher.
6. **Bloqueo de pantalla**: solo es limpio con accesibilidad (Android 9+); las alternativas (administrador de dispositivo, bajar el tiempo de pantalla) rompen huella o ajustes.

## 5. Terreno técnico y de políticas (2026)

- **Target API**: desde el 31 ago 2026 las apps nuevas y actualizaciones en Play deben apuntar a Android 16 (API 36). Implica *edge-to-edge* obligatorio y *predictive back* (no llega `onBackPressed`).
- **`QUERY_ALL_PACKAGES`**: Play lo trata como dato sensible; solo se permite si la función central lo exige y con formulario de declaración. Un launcher **no lo necesita**: usa `LauncherApps` y `<queries>` con `MAIN/LAUNCHER`.
- **AccessibilityService**: permitido, pero si la app no es herramienta de accesibilidad debe tener divulgación destacada + consentimiento, y declaración en Play Console. Niagara lo usa solo para apagar la pantalla, opcional y desactivado por defecto. Copiamos esa disciplina.
- **Espacio privado (Android 15+)**: para ver esas apps, el launcher debe ser el predeterminado (`ROLE_HOME`), declarar `ACCESS_HIDDEN_PROFILES`, y ofrecer un contenedor separado que se pueda ocultar/mostrar y bloquear/desbloquear, con botón "Instalar apps".
- **Compose**: Google reporta que desde Compose 1.9 el scroll iguala a Views en jank. Claves: Baseline Profiles, estabilidad, entender fases (composición → layout → dibujo).
- **Material 3 Expressive**: movimiento por física de resortes (`MotionScheme.standard()` / `.expressive()`), resortes para lo espacial y tweens para efectos.
- **NotificationListener**: no pedir `requestUnbind()` antes de `onListenerConnected()`; en algunos OEM el rebind tarda varios segundos.

## 6. Referencias de código abierto (para estudiar, respetando licencias)

- **Kvaesitso** — launcher centrado en búsqueda, Kotlin + Compose, GPL-3.0. Útil para ver soluciones; **no copiar código** (GPL obligaría a licenciar todo igual). Su SDK de plugins es Apache-2.0.
- **Focus Launcher** — minimalista en Compose (GPL-3.0). Mismo cuidado.
- **Launcher3 (AOSP)** — Apache-2.0. Referencia canónica para íconos, perfiles de trabajo, notificaciones y widgets.

## 7. Conclusiones

1. El valor no está en tener muchas funciones sino en que **5 cosas se sientan perfectas**: arranque instantáneo, alfabeto ola, abrir una app, buscar, volver a inicio.
2. Los bugs más dolorosos de la categoría son de **ciclo de vida y OEM**, no de UI. Hay que probar en Samsung, Xiaomi y Motorola reales desde temprano.
3. Los permisos especiales (notificaciones, accesibilidad) son **opcionales, explicados y degradables**: la app funciona completa sin ellos.

## Fuentes

- Niagara — kit de prensa y ayuda: https://help.niagaralauncher.app/article/83-press-kit · https://help.niagaralauncher.app/article/40-niagara-pro-features · https://help.niagaralauncher.app/article/73-lock-screen-on-older-android-versions
- Niagara — ficha y tablero de bugs: https://niagaralauncher.app/get · https://niagaralauncher.featurebase.app/
- Prensa: Android Police (lanzamiento estable, pop-ups, Niagara ID), Android Authority (Studio Update), Digital Trends (respaldos, Usage Breaker), TuAppPara (ficha 2026)
- Google Play — QUERY_ALL_PACKAGES: https://support.google.com/googleplay/android-developer/answer/10158779
- Google Play — AccessibilityService: https://support.google.com/googleplay/android-developer/answer/10964491
- Target API: https://developer.android.com/google/play/requirements/target-sdk
- Android 15 espacio privado: https://developer.android.com/about/versions/15/behavior-changes-all
- Compose performance: https://developer.android.com/develop/ui/compose/performance
- Kvaesitso: https://github.com/MM2-0/Kvaesitso
