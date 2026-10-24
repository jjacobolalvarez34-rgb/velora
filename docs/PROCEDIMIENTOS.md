# Procedimientos

## P-01. Configurar el proyecto desde cero (una sola vez)

1. Android Studio (última estable). JDK 17+.
2. Crear proyecto "Empty Activity (Compose)", paquete `app.velora.launcher`, `minSdk 26`.
3. Ajustar `targetSdk`/`compileSdk` a 36.
4. Crear módulos `:core:model`, `:core:data`, `:core:designsystem`, `:baselineprofile` (plantilla "Baseline Profile Generator"), `:macrobenchmark`.
5. Mover versiones a `gradle/libs.versions.toml`.
6. Agregar detekt + ktlint; configurar `gradlew lint detekt ktlintCheck`.
7. Manifiesto de `HomeActivity` (ver P-02).
8. Copiar esta carpeta `docs/` y `CLAUDE.md` a la raíz del repo. Primer commit: `chore: estructura inicial y documentación`.
9. Crear repositorio privado en GitHub; activar GitHub Actions con el workflow de P-06.

## P-02. Manifiesto base del launcher

```xml
<queries>
    <intent>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent>
</queries>

<activity
    android:name=".HomeActivity"
    android:exported="true"
    android:launchMode="singleTask"
    android:stateNotNeeded="true"
    android:clearTaskOnLaunch="true"
    android:excludeFromRecents="true"
    android:resumeWhilePausing="true"
    android:windowSoftInputMode="adjustNothing"
    android:configChanges="orientation|screenSize|screenLayout|smallestScreenSize|density|keyboard|keyboardHidden|navigation|uiMode"
    android:theme="@style/Theme.Velora.Home">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.HOME" />
        <category android:name="android.intent.category.DEFAULT" />
    </intent-filter>
</activity>

<!-- Entrada aparte para abrir los ajustes desde cualquier launcher -->
<activity
    android:name=".feature.settings.SettingsActivity"
    android:exported="true"
    android:label="@string/settings_launcher_label">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```
La `SettingsActivity` con `LAUNCHER` permite configurar Velora aunque todavía no sea el launcher predeterminado (y aparece como "Ajustes de Velora" en la propia lista). `Theme.Velora.Home` debe tener `android:windowShowWallpaper=true` y `android:windowBackground=@android:color/transparent`.

## P-03. Flujo de una tarea (feature o fix)

1. **Entender**: ¿en qué fase del `ROADMAP.md` está? Si no está, preguntar antes de construir.
2. **Leer**: docs relacionados + `BUGS.md` del área.
3. **Plan corto** (en el chat o en el PR): objetivo, archivos, riesgos, cómo se verifica.
4. **Rama** `feat/…` o `fix/…`.
5. **Implementar** en pasos que compilen; prueba primero cuando sea lógica pura.
6. **Verificar** con la checklist de `VERIFICACIONES.md` (§1 siempre; §2–§5 según aplique).
7. **Probar en teléfono real** el flujo afectado + "volver a inicio" + bloquear/desbloquear.
8. **Documentar**: actualizar docs tocados, `BUGS.md` si fue bug, `DECISIONES.md` si hubo decisión, `ROADMAP.md` si se cerró un ítem.
9. **Commit/PR** con la plantilla de §P-07.

## P-04. Registrar y corregir un bug

1. Reproducir. Escribir pasos exactos, dispositivo, versión de Android, capa del fabricante.
2. Abrir entrada en `BUGS.md` con estado `Abierto`.
3. Escribir prueba que falle (unitaria, UI o benchmark).
4. Encontrar **causa raíz** (no síntoma). Si es de OEM, documentar la capa (One UI, HyperOS, ColorOS…).
5. Corregir; la prueba pasa.
6. Actualizar la entrada: causa, solución, prueba, commit. Estado `Resuelto`.
7. Si revela una clase de errores, agregar regla a `REGLAS.md` o check a `VERIFICACIONES.md`.

## P-05. Agregar un permiso o servicio del sistema

1. Justificar: ¿hay una API más estrecha que lo evite? (Casi siempre sí.)
2. ADR en `DECISIONES.md`.
3. Entrada en `SEGURIDAD.md` §Inventario de permisos.
4. Pantalla de explicación previa (microcopy de `ESTILO.md` §10) + forma de revocar.
5. Modo degradado sin el permiso + prueba de ese modo.
6. Si es para Play: preparar declaración en Play Console y texto de divulgación.

## P-06. Integración continua (GitHub Actions)

En cada PR:
1. `gradlew ktlintCheck detekt lint`
2. `gradlew testDebugUnitTest`
3. `gradlew assembleRelease` (verifica R8) y reporte de tamaño de APK.
4. Semanal o antes de release (requiere emulador/dispositivo): `connectedDebugAndroidTest` y macrobenchmarks.

## P-07. Plantilla de PR / resumen de cambio

```
## Qué cambia
## Por qué
## Cómo probarlo en el teléfono (pasos)
## Checklist
- [ ] VERIFICACIONES §1 (siempre)
- [ ] §2 UI/Animación  - [ ] §3 Rendimiento  - [ ] §4 Seguridad  - [ ] §5 OEM
- [ ] Docs actualizados (cuáles: …)
## Métricas (si aplica): antes → después
```

## P-08. Release

1. Subir `versionCode`/`versionName` (SemVer: `0.MINOR.PATCH` hasta 1.0).
2. Regenerar Baseline Profile.
3. Correr macrobenchmarks y comparar con la versión anterior (tabla en el changelog interno).
4. Checklist de release de `VERIFICACIONES.md` §6.
5. Build firmado (keystore fuera del repo, respaldado en dos lugares).
6. Publicar primero en **pista interna/cerrada** de Play o como APK a probadores; esperar 3–7 días.
7. Notas de versión en español e inglés.

## P-09. Probar en un teléfono como launcher

1. `gradlew installDebug`.
2. Pulsar Inicio → elegir "Velora (debug)" → Siempre.
3. Si algo se rompe y no se puede salir: `adb shell cmd package set-home-activity <paquete del launcher original>/<actividad>` o desinstalar con `adb uninstall app.velora.launcher.debug`. Anota el paquete del launcher original de tu teléfono **antes** de empezar (`adb shell cmd package resolve-activity -c android.intent.category.HOME -a android.intent.action.MAIN`).

## P-10. Actualizar dependencias

Mensual: revisar actualizaciones estables, una por una o en grupos pequeños, correr §1 y §3 de verificaciones. Registrar saltos mayores en `DECISIONES.md`.
