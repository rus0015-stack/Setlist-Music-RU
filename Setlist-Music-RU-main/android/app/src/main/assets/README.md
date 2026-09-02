# SETLIST MUSIC 4.3 FIX

Aplicación de Rubén Ubaldo. Esta versión parte de la PWA 4.3 FIX entregada y queda preparada para:

- GitHub Pages / PWA.
- Android nativo mediante Capacitor.
- Funcionamiento offline.
- Biblioteca de canciones con almacenamiento local.
- SETLIST — MODO DIRECTO.
- Botón `|` en cada sección para insertar separadores de acordes.
- Tonalidad/transposición en modo EN VIVO.
- Letras, charts PDF, secuencias de audio y referencias.
- Exportación/importación de biblioteca.
- Logo/icono de Setlist Music y texto “By Rubén Ubaldo”.

## SUBIR A GITHUB

1. Crea un repositorio nuevo, por ejemplo `setlist-music`.
2. Sube **todo el contenido de esta carpeta**, incluyendo `.github/workflows/android.yml`.
3. En GitHub, entra a **Settings → Pages** y selecciona `Deploy from a branch`, rama `main`, carpeta `/ (root)`.
4. Guarda y espera la publicación.

## INSTALAR COMO APP NATIVA ANDROID

El repositorio ya incluye un workflow de GitHub Actions que genera un APK Android.

1. Sube el proyecto a GitHub.
2. Entra a **Actions**.
3. Selecciona **Build Setlist Music Android**.
4. Pulsa **Run workflow** (o haz un push a `main`).
5. Cuando termine, abre la ejecución y descarga el artefacto:
   `Setlist-Music-v4.3-FIX-Android`
6. Dentro encontrarás `app-debug.apk`.
7. Pásalo al celular Android e instálalo.

El APK de esta versión es **debug**, adecuado para instalación personal/pruebas. Para publicar en Google Play se debe crear una firma de lanzamiento (release).

## IMPORTANTE SOBRE LOS DATOS

Las canciones, setlists, PDFs y preferencias se almacenan localmente en el dispositivo. La exportación JSON permite hacer copias de seguridad. Los archivos de audio grandes permanecen en IndexedDB y no forman parte del JSON.

## VERSIÓN

Setlist Music 4.3 FIX
By Rubén Ubaldo
