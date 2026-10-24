# Producto — Velora Launcher

## Visión en una frase

Una pantalla de inicio para Android que se usa con una mano, abre cualquier app en menos de un segundo y no distrae.

## Para quién

- Personas con teléfonos grandes que usan el teléfono con una mano (bus, calle, caminando).
- Personas que quieren usar menos el celular sin renunciar a nada.
- Usuarios de gama media (Samsung A, Xiaomi Redmi, Motorola G): **la fluidez debe sentirse allí, no solo en un Pixel**.

## Principios de producto (en orden de prioridad)

1. **Rápido antes que bonito; bonito antes que completo.** Una animación hermosa que da jank no se publica.
2. **Una mano.** Todo lo frecuente está en el tercio inferior de la pantalla.
3. **Calma.** Pocos colores, poco texto, nada parpadea sin razón.
4. **Funciona sin permisos especiales.** Notificaciones y accesibilidad son mejoras opcionales.
5. **Privado por diseño.** Nada sale del teléfono.

## Funciones — alcance

### MVP (Fase 1) — "Ya lo puedo usar todos los días"
- Pantalla de inicio única: reloj/fecha arriba + lista de favoritos (máx. 8 sugeridos, 12 permitidos).
- Lista completa de apps alfabética con **alfabeto ola** a la derecha (o izquierda en modo zurdo).
- Búsqueda de apps (tolerante a tildes y mayúsculas; "camara" encuentra "Cámara").
- Abrir apps con animación de origen (la app "crece" desde su fila).
- Menú de app (pulsación larga): info de la app, desinstalar, ocultar, agregar/quitar favorito, atajos de la app (`ShortcutInfo`).
- Apps ocultas (lista recuperable desde ajustes).
- Respeta Material You (color dinámico) y modo claro/oscuro.
- Perfil de trabajo: apps de trabajo marcadas y agrupables.
- Ajustes básicos: modo zurdo, tamaño de texto, mostrar íconos sí/no, orden de favoritos.

### Fase 2 — "Me cuenta lo importante"
- Notificaciones integradas (opt-in): resumen en el inicio, punto en la app, leer y responder rápido.
- Reproductor de medios contextual (aparece solo cuando algo suena).
- Apps instaladas recientemente.
- Espacio privado (Android 15+).

### Fase 3 — "Lo organizo a mi manera"
- Pop-ups por deslizamiento sobre una app: atajos, notificaciones, apps relacionadas.
- Carpetas.
- Widgets (uno o varios en la parte superior).
- Paquetes de íconos de terceros y forma de íconos propia.

### Fase 4 — "Me ayuda a usarlo menos"
- Gesto de bloqueo (doble toque) vía accesibilidad opt-in.
- Recordatorio de uso por app (similar a un "freno" de uso), local.
- Respaldo y restauración de ajustes (Android Backup + exportar archivo).

### Fase 5 — Sostenibilidad
- Versión Pro (pago único + opcional anual) con funciones de personalización. **Sin anuncios nunca.**

### Fuera de alcance (por ahora)
- Feed tipo Google Discover, temas descargables online, cuentas de usuario, sincronización en la nube, versión iOS (imposible).

## Glosario

| Término | Definición |
|---|---|
| Inicio / Home | La pantalla principal del launcher |
| Favoritos | Apps fijadas en la pantalla de inicio |
| Cajón / Lista completa | Todas las apps, ordenadas alfabéticamente |
| Alfabeto ola | Barra lateral de letras con deformación tipo onda al arrastrar |
| Pop-up | Panel flotante que sale de una fila al deslizar sobre ella |
| Contextual | Elemento que aparece solo cuando es relevante (música, evento) |
| Perfil | Usuario de Android: personal, trabajo, espacio privado |

## Métrica de éxito personal

Usarlo como launcher principal 30 días seguidos sin volver al launcher del fabricante, en un teléfono de gama media.
