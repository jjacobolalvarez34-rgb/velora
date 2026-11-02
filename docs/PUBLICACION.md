# Publicación y firma

> Cómo se firma Velora y cómo se sube a Google Play. Leer antes de cualquier release.

## 1. La clave de release

Velora se firma con una clave propia desde la 0.9.1 (ADR-038). Antes se firmaba con la clave de
pruebas (`debug`) del PC, que no sirve para la Play Store.

| Qué | Dónde | ¿En git? |
|---|---|---|
| Clave (`velora-release.jks`, PKCS12, RSA 4096, válida 100 años, alias `velora`) | `keystore/velora-release.jks` | **No** (`.gitignore`: `*.jks`, `keystore/`) |
| Contraseñas y ruta | `keystore.properties` (raíz del proyecto) | **No** (`.gitignore`) |
| Cómo se usa | `app/build.gradle.kts` → `signingConfigs.release` | Sí |

**Por qué no va en git:** con la clave y su contraseña cualquiera podría firmar una versión falsa
de Velora que se instale encima de la verdadera. Nunca subirlas a git, a un chat ni a un correo sin
cifrar.

### Respaldo (hacerlo ya)
1. Copiar **los dos** archivos (`keystore/velora-release.jks` y `keystore.properties`) a dos lugares
   fuera de este PC: por ejemplo, un USB guardado y un gestor de contraseñas (Bitwarden, 1Password)
   o una nube personal con el archivo comprimido con contraseña.
2. Comprobar que la copia abre: `keytool -list -keystore velora-release.jks` con la contraseña de
   `keystore.properties`.

**Si se pierde la clave** y la app ya está en la Play Store con *Play App Signing* (recomendado,
ver §3), Google permite cambiar la clave de subida desde Play Console. Sin Play App Signing, una
clave perdida significa no poder actualizar nunca más la app.

### Sin `keystore.properties`
El build de release se firma con la clave `debug` del PC (sirve para benchmarks locales). Un APK
firmado así **no** puede instalarse encima de uno firmado con la clave de release, ni al revés.

## 2. Compilar

```
gradlew :app:assembleRelease   # APK para instalar a mano → app/build/outputs/apk/release/
gradlew :app:bundleRelease     # AAB para la Play Store   → app/build/outputs/bundle/release/
```
O todo junto, sin depender de nadie: `bash tools/build-release.sh` (deja `Velora-<versión>.apk`
en la raíz e informa en `build-report.txt`).

Comprobar la firma: `apksigner verify --print-certs Velora-<versión>.apk` → el certificado debe
decir `CN=Velora`, no `CN=Android Debug`.

## 3. Subir a Google Play (prueba interna)

Por qué: Play Protect bloquea la **instalación nueva** de APK sueltos que piden accesibilidad o
leer notificaciones (BUGS B-102). Desde la Play Store no hay bloqueo y las actualizaciones llegan
solas.

1. Crear una cuenta de desarrollador en Google Play Console (pago único de 25 USD).
2. Crear la app "Velora", idioma por defecto español.
3. Aceptar **Play App Signing**: Google guarda la clave de firma final y nuestra clave pasa a ser la
   *clave de subida* (si se pierde, se puede reemplazar desde Play Console).
4. Pruebas → **Prueba interna** → crear versión → subir `app-release.aab`.
5. Agregar a los probadores por correo y compartirles el enlace de prueba.
6. Completar lo que Play pide antes de publicar:
   - Política de privacidad (Velora no envía datos: SEGURIDAD §1).
   - Declaración de **accesibilidad** (solo bloquear la pantalla con doble toque, ADR-009) y de
     **acceso a notificaciones** (mostrar puntos y música en el inicio, ADR-023).
   - Permisos sensibles: `READ_CONTACTS` (búsqueda, opt-in), `PACKAGE_USAGE_STATS` (tiempo de uso,
     opt-in), `ACCESS_HIDDEN_PROFILES` (espacio privado).
   - Clasificación de contenido y público objetivo.
7. Para la pista cerrada/producción (cuentas personales nuevas): 12+ probadores durante 14 días
   (ROADMAP Fase 5; verificar el requisito vigente).

## 4. Cambio de clave (una sola vez, 0.9.1)

Quien tenga instalada una versión firmada con la clave `debug` (0.1.0–0.9.0) debe **desinstalar
Velora una vez** antes de instalar la 0.9.1. Antes de desinstalar, guardar la configuración con
Ajustes → Favoritos, widgets y ocultas → Respaldo (`.velora-backup`) y restaurarla después.
