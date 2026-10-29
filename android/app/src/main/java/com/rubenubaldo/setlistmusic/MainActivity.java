package com.rubenubaldo.setlistmusic;

import android.content.Context;
import android.view.ViewParent;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {

    private WebView webView;

    private ValueCallback<Uri[]> filePathCallback;
    private static final int FILE_CHOOSER_REQUEST_CODE = 1001;

    private View pdfViewer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);

        webView.setWebViewClient(new WebViewClient());

        webView.addJavascriptInterface(
                new PDFInterface(),
                "AndroidPDF"
        );

        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public boolean onShowFileChooser(
                    WebView webView,
                    ValueCallback<Uri[]> filePathCallback,
                    FileChooserParams fileChooserParams) {

                if (MainActivity.this.filePathCallback != null) {
                    MainActivity.this.filePathCallback.onReceiveValue(null);
                }

                MainActivity.this.filePathCallback = filePathCallback;

                try {

                    Intent intent = fileChooserParams.createIntent();

                    intent.addCategory(
                            Intent.CATEGORY_OPENABLE
                    );

                    startActivityForResult(
                            intent,
                            FILE_CHOOSER_REQUEST_CODE
                    );

                } catch (Exception e) {

                    MainActivity.this.filePathCallback = null;

                    return false;
                }

                return true;
            }
        });

        webView.loadUrl(
                "file:///android_asset/index.html"
        );
    }


    /**
     * Puente entre JavaScript y Android.
     *
     * Permite:
     * - Abrir PDFs.
     * - Guardar copias JSON en Descargas.
     */
    public class PDFInterface {

        /**
         * ABRIR PDF
         */

@JavascriptInterface
public void abrirPDF(String base64) {
    runOnUiThread(() -> {
        File archivo = null;

        try {
            if (base64 == null || base64.trim().isEmpty()) {
                throw new Exception("El contenido del PDF está vacío");
            }

            String datos = base64.trim();

            if (datos.contains(",")) {
                datos = datos.substring(datos.indexOf(",") + 1);
            }

            byte[] pdfBytes = Base64.decode(datos, Base64.DEFAULT);

            if (pdfBytes.length < 5
                    || pdfBytes[0] != '%'
                    || pdfBytes[1] != 'P'
                    || pdfBytes[2] != 'D'
                    || pdfBytes[3] != 'F'
                    || pdfBytes[4] != '-') {
                throw new Exception("El contenido recibido no es un PDF válido");
            }

            archivo = new File(getCacheDir(), "chart_setlist_music.pdf");

            try (FileOutputStream fos = new FileOutputStream(archivo)) {
                fos.write(pdfBytes);
                fos.flush();
            }

            if (!archivo.exists() || archivo.length() == 0) {
                throw new Exception("El archivo PDF quedó vacío");
            }

            android.net.Uri uri =
        androidx.core.content.FileProvider.getUriForFile(
                MainActivity.this,
                MainActivity.this.getPackageName() + ".fileprovider",
                archivo
        );

            Intent verPDF = new Intent(Intent.ACTION_VIEW);
            verPDF.setDataAndType(uri, "application/pdf");
            verPDF.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            Intent selector = Intent.createChooser(verPDF, "Abrir PDF con");
            startActivity(selector);

        } catch (android.content.ActivityNotFoundException e) {
            Toast.makeText(
                    MainActivity.this,
                    "No hay una aplicación para abrir PDF instalada",
                    Toast.LENGTH_LONG
            ).show();

        } catch (Exception e) {
            Toast.makeText(
                    MainActivity.this,
                    "Error al abrir PDF: " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();

            e.printStackTrace();
        }
    });
}



        /**
         * EXPORTAR JSON A DESCARGAS
         */
        @JavascriptInterface
        public void exportarJSON(
                String contenido,
                String nombreArchivo) {

            try {

                if (contenido == null || contenido.isEmpty()) {

                    runOnUiThread(() ->
                            Toast.makeText(
                                    MainActivity.this,
                                    "El JSON está vacío",
                                    Toast.LENGTH_LONG
                            ).show()
                    );

                    return;
                }


                if (nombreArchivo == null
                        || nombreArchivo.trim().isEmpty()) {

                    nombreArchivo =
                            "Setlist_Music_backup.json";
                }


                /*
                 * ANDROID 10 O SUPERIOR
                 *
                 * Se utiliza MediaStore para guardar
                 * directamente en Descargas.
                 */
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

                    ContentValues values =
                            new ContentValues();

                    values.put(
                            MediaStore.Downloads.DISPLAY_NAME,
                            nombreArchivo
                    );

                    values.put(
                            MediaStore.Downloads.MIME_TYPE,
                            "application/json"
                    );

                    values.put(
                            MediaStore.Downloads.RELATIVE_PATH,
                            Environment.DIRECTORY_DOWNLOADS
                    );

                    values.put(
                            MediaStore.Downloads.IS_PENDING,
                            1
                    );


                    Uri uri =
                            getContentResolver().insert(
                                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                                    values
                            );


                    if (uri == null) {

                        throw new Exception(
                                "No se pudo crear el archivo en Descargas."
                        );
                    }


                    try (
                            OutputStream salida =
                                    getContentResolver()
                                            .openOutputStream(uri)
                    ) {

                        if (salida == null) {

                            throw new Exception(
                                    "No se pudo abrir el archivo."
                            );
                        }

                        salida.write(
                                contenido.getBytes(
                                        StandardCharsets.UTF_8
                                )
                        );

                        salida.flush();
                    }


                    ContentValues terminado =
                            new ContentValues();

                    terminado.put(
                            MediaStore.Downloads.IS_PENDING,
                            0
                    );

                    getContentResolver().update(
                            uri,
                            terminado,
                            null,
                            null
                    );


                    runOnUiThread(() ->
                            Toast.makeText(
                                    MainActivity.this,
                                    "✓ JSON guardado en Descargas",
                                    Toast.LENGTH_LONG
                            ).show()
                    );

                } else {

                    /*
                     * ANDROID 9 O ANTERIOR
                     */
                    File carpeta =
                            Environment.getExternalStoragePublicDirectory(
                                    Environment.DIRECTORY_DOWNLOADS
                            );

                    if (!carpeta.exists()) {
                        carpeta.mkdirs();
                    }


                    File archivo =
                            new File(
                                    carpeta,
                                    nombreArchivo
                            );


                    FileOutputStream salida =
                            new FileOutputStream(archivo);

                    salida.write(
                            contenido.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

                    salida.flush();
                    salida.close();


                    runOnUiThread(() ->
                            Toast.makeText(
                                    MainActivity.this,
                                    "✓ JSON guardado en Descargas",
                                    Toast.LENGTH_LONG
                            ).show()
                    );
                }


            } catch (Exception e) {

                e.printStackTrace();

                String mensaje =
                        e.getMessage();

                if (mensaje == null) {
                    mensaje = "Error desconocido";
                }

                final String errorFinal =
                        mensaje;

                runOnUiThread(() ->
                        Toast.makeText(
                                MainActivity.this,
                                "Error al guardar JSON: "
                                        + errorFinal,
                                Toast.LENGTH_LONG
                        ).show()
                );
            }
        }
    }


    /**
     * Muestra el PDF dentro de la aplicación.
     */
    private void mostrarPDF(File archivo) {

        try {

            ParcelFileDescriptor descriptor =
                    ParcelFileDescriptor.open(
                            archivo,
                            ParcelFileDescriptor.MODE_READ_ONLY
                    );

            PdfRenderer renderer =
                    new PdfRenderer(descriptor);


            /*
             * CONTENEDOR PRINCIPAL DE LAS PÁGINAS
             */
            LinearLayout contenido =
                    new LinearLayout(this);

            contenido.setOrientation(
                    LinearLayout.VERTICAL
            );

            contenido.setBackgroundColor(
                    Color.WHITE
            );

            contenido.setPadding(
                    0,
                    0,
                    0,
                    0
            );


            /*
             * CREAR TODAS LAS PÁGINAS
             */
            for (int i = 0;
                 i < renderer.getPageCount();
                 i++) {

                PdfRenderer.Page page =
                        renderer.openPage(i);

                int ancho =
                        page.getWidth();

                int alto =
                        page.getHeight();


                /*
                 * Resolución inicial del PDF.
                 */
                float escalaInicial = 2.0f;

                int nuevoAncho =
                        (int) (ancho * escalaInicial);

                int nuevoAlto =
                        (int) (alto * escalaInicial);


                Bitmap bitmap =
                        Bitmap.createBitmap(
                                nuevoAncho,
                                nuevoAlto,
                                Bitmap.Config.ARGB_8888
                        );

                bitmap.eraseColor(
                        Color.WHITE
                );


                page.render(
                        bitmap,
                        null,
                        null,
                        PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                );

                page.close();


                /*
                 * VISOR DE LA PÁGINA
                 */
                ZoomImageView imagen =
                        new ZoomImageView(this);

                imagen.setImageBitmap(
                        bitmap
                );

                imagen.setScaleType(
                        ImageView.ScaleType.FIT_CENTER
                );

                imagen.setAdjustViewBounds(
                        true
                );

                imagen.setBackgroundColor(
                        Color.WHITE
                );


                LinearLayout.LayoutParams parametros =
                        new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                        );

                parametros.bottomMargin = 0;


                contenido.addView(
                        imagen,
                        parametros
                );
            }


            renderer.close();
            descriptor.close();


            /*
             * SCROLL VERTICAL
             */
            ScrollView scroll =
                    new ScrollView(this);

            scroll.setBackgroundColor(
                    Color.WHITE
            );

            scroll.setFillViewport(
                    true
            );

            scroll.setPadding(
                    0,
                    0,
                    0,
                    0
            );

            scroll.addView(
                    contenido
            );


            /*
             * CONTENEDOR PRINCIPAL
             */
            LinearLayout principal =
                    new LinearLayout(this);

            principal.setOrientation(
                    LinearLayout.VERTICAL
            );

            principal.setBackgroundColor(
                    Color.rgb(30, 30, 30)
            );


            /*
             * BARRA SUPERIOR
             */
            TextView barra =
                    new TextView(this);

            barra.setText(
                    "📄  CHART PDF"
            );

            barra.setTextColor(
                    Color.WHITE
            );

            barra.setTextSize(
                    17
            );

            barra.setGravity(
                    Gravity.CENTER_VERTICAL
            );

            barra.setPadding(
                    20,
                    15,
                    20,
                    15
            );

            barra.setBackgroundColor(
                    Color.rgb(20, 23, 30)
            );


            /*
             * BOTÓN CERRAR
             */
            TextView cerrar =
                    new TextView(this);

            cerrar.setText(
                    "✕  Cerrar"
            );

            cerrar.setTextColor(
                    Color.WHITE
            );

            cerrar.setTextSize(
                    15
            );

            cerrar.setGravity(
                    Gravity.CENTER
            );

            cerrar.setPadding(
                    20,
                    15,
                    20,
                    15
            );

            cerrar.setBackgroundColor(
                    Color.rgb(65, 95, 220)
            );

            cerrar.setOnClickListener(
                    v -> cerrarPDFAndroid()
            );


            /*
             * FILA SUPERIOR
             */
            LinearLayout fila =
                    new LinearLayout(this);

            fila.setOrientation(
                    LinearLayout.HORIZONTAL
            );

            fila.setGravity(
                    Gravity.CENTER_VERTICAL
            );


            fila.addView(
                    barra,
                    new LinearLayout.LayoutParams(
                            0,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            1
                    )
            );


            fila.addView(
                    cerrar,
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    )
            );


            principal.addView(
                    fila
            );


            principal.addView(
                    scroll,
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            0,
                            1
                    )
            );


            pdfViewer = principal;

            setContentView(
                    pdfViewer
            );


        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Error al mostrar el PDF",
                    Toast.LENGTH_LONG
            ).show();

            e.printStackTrace();
        }
    }


    /**
     * ImageView especial para PDF.
     *
     * Permite:
     *
     * - Zoom con dos dedos.
     * - Arrastre horizontal.
     * - Arrastre vertical.
     * - Zoom máximo 4x.
     */
    
private class ZoomImageView extends ImageView {

    private float escala = 1.0f;
    private float ultimaX;
    private float ultimaY;
    private float desplazamientoX = 0f;
    private float desplazamientoY = 0f;

    private ScaleGestureDetector detector;

    public ZoomImageView(Context context) {
        super(context);

        setScaleType(ImageView.ScaleType.FIT_CENTER);
        setAdjustViewBounds(true);
        setClickable(true);

        detector = new ScaleGestureDetector(
                context,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {

                    @Override
                    public boolean onScale(ScaleGestureDetector detectorEscala) {

                        escala *= detectorEscala.getScaleFactor();

                        // Limitar el zoom para evitar ampliaciones excesivas.
                        escala = Math.max(1.0f, Math.min(4.0f, escala));

                        setScaleX(escala);
                        setScaleY(escala);

                        if (escala <= 1.0f) {
                            escala = 1.0f;
                            desplazamientoX = 0f;
                            desplazamientoY = 0f;

                            setScaleX(1.0f);
                            setScaleY(1.0f);
                            setTranslationX(0f);
                            setTranslationY(0f);
                        }

                        return true;
                    }
                }
        );
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        // Procesar primero los gestos de ampliación con dos dedos.
        detector.onTouchEvent(event);

        ViewParent padre = getParent();

        switch (event.getActionMasked()) {

            case MotionEvent.ACTION_DOWN:

                ultimaX = event.getX();
                ultimaY = event.getY();

                if (padre != null) {
                    padre.requestDisallowInterceptTouchEvent(escala > 1.0f);
                }

                return true;

            case MotionEvent.ACTION_POINTER_DOWN:

                // Permitir el gesto de zoom con dos dedos.
                if (padre != null) {
                    padre.requestDisallowInterceptTouchEvent(true);
                }

                return true;

            case MotionEvent.ACTION_MOVE:

                if (escala > 1.0f
                        && !detector.isInProgress()
                        && event.getPointerCount() == 1) {

                    float diferenciaX = event.getX() - ultimaX;
                    float diferenciaY = event.getY() - ultimaY;

                    desplazamientoX += diferenciaX;
                    desplazamientoY += diferenciaY;

                    setTranslationX(desplazamientoX);
                    setTranslationY(desplazamientoY);

                    if (padre != null) {
                        padre.requestDisallowInterceptTouchEvent(true);
                    }

                } else if (escala <= 1.0f) {

                    // Sin zoom, el ScrollView puede desplazarse verticalmente.
                    if (padre != null) {
                        padre.requestDisallowInterceptTouchEvent(false);
                    }
                }

                ultimaX = event.getX();
                ultimaY = event.getY();

                return true;

            case MotionEvent.ACTION_POINTER_UP:

                ultimaX = event.getX(0);
                ultimaY = event.getY(0);

                if (padre != null) {
                    padre.requestDisallowInterceptTouchEvent(escala > 1.0f);
                }

                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:

                if (padre != null) {
                    padre.requestDisallowInterceptTouchEvent(false);
                }

                performClick();
                return true;

            default:
                return true;
        }
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }
}


    /**
     * Cierra el visor PDF.
     */
    private void cerrarPDFAndroid() {

        if (pdfViewer != null) {

            pdfViewer = null;

            setContentView(
                    webView
            );
        }
    }


    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );


        if (requestCode ==
                FILE_CHOOSER_REQUEST_CODE) {

            Uri[] results = null;


            if (resultCode == RESULT_OK
                    && data != null) {


                if (data.getClipData() != null) {

                    int count =
                            data.getClipData()
                                    .getItemCount();

                    results =
                            new Uri[count];


                    for (int i = 0;
                         i < count;
                         i++) {

                        results[i] =
                                data.getClipData()
                                        .getItemAt(i)
                                        .getUri();
                    }


                } else if (
                        data.getData() != null) {

                    results =
                            new Uri[]{
                                    data.getData()
                            };
                }
            }


            if (filePathCallback != null) {

                filePathCallback.onReceiveValue(
                        results
                );

                filePathCallback = null;
            }
        }
    }


    @Override
    public void onBackPressed() {

        if (pdfViewer != null) {

            cerrarPDFAndroid();

        } else if (webView.canGoBack()) {

            webView.goBack();

        } else {

            super.onBackPressed();
        }
    }
}
