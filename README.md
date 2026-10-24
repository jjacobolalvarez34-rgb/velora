# Velora Launcher

Launcher minimalista para Android: una sola pantalla, lista vertical, alfabeto ola, pensado para una mano. Ligero, rápido y sin anuncios. Nada sale de tu teléfono.

> Proyecto en Fase 0.

## Empezar

- Si eres Claude Code: lee `CLAUDE.md`.
- Si eres humano: lee `docs/PRODUCTO.md` y luego `docs/ROADMAP.md`.

## Documentación

| | |
|---|---|
| [Producto](docs/PRODUCTO.md) | Visión, usuarios, alcance |
| [Investigación](docs/INVESTIGACION.md) | Niagara y el terreno técnico, con fuentes |
| [Estilo](docs/ESTILO.md) | Diseño visual, movimiento, alfabeto ola |
| [Arquitectura](docs/ARQUITECTURA.md) | Módulos, flujos de datos, ciclo de vida |
| [Rendimiento](docs/RENDIMIENTO.md) | Presupuestos y reglas de Compose |
| [Reglas](docs/REGLAS.md) | Reglas de código |
| [Procedimientos](docs/PROCEDIMIENTOS.md) | Setup, flujo de tareas, bugs, release |
| [Verificaciones](docs/VERIFICACIONES.md) | Checklists |
| [Seguridad](docs/SEGURIDAD.md) | Permisos, privacidad, políticas de Play |
| [Bugs](docs/BUGS.md) | Trampas conocidas y registro |
| [Roadmap](docs/ROADMAP.md) | Fases |
| [Pendientes](docs/PENDIENTES.md) | Lo que sigue, en orden |
| [Decisiones](docs/DECISIONES.md) | ADRs |

## Compilar

Requiere JDK 17+ y el SDK de Android (ruta en `local.properties`: `sdk.dir=C:/Users/.../Android/Sdk`).

```
gradlew.bat assembleDebug      # Windows (macOS/Linux: ./gradlew)
gradlew.bat installDebug
```
Luego pulsa Inicio y elige "Velora (debug)". Mantén presionado el fondo para abrir los ajustes y volver a tu launcher anterior cuando quieras.
