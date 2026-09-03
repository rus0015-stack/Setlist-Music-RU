package com.rubenubaldo.setlistmusic;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
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
     * Puente entre JavaScript y Android
     * para visualizar PDFs.
     */
    public class PDFInterface {

        @JavascriptInterface
        public void abrirPDF(String base64) {

            runOnUiThread(() -> {

                try {

                    String datos = base64;

                    if (datos.contains(",")) {

                        datos = datos.substring(
                                datos.indexOf(",") + 1
                        );
                    }

                    byte[] pdfBytes =
                            Base64.decode(
                                    datos,
                                    Base64.DEFAULT
                            );

                    File archivo =
                            new File(
                                    getCacheDir(),
                                    "chart_setlist_music.pdf"
                            );

                    FileOutputStream fos =
                            new FileOutputStream(archivo);

                    fos.write(pdfBytes);
                    fos.close();

                    mostrarPDF(archivo);

                } catch (Exception e) {

                    Toast.makeText(
                            MainActivity.this,
                            "No se pudo abrir el PDF",
                            Toast.LENGTH_SHORT
                    ).show();

                    e.printStackTrace();
                }
            });
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

        private float escalaAnterior = 1.0f;

        private float ultimaX;
        private float ultimaY;

        private float desplazamientoX = 0;
        private float desplazamientoY = 0;

        private ScaleGestureDetector detector;

        private boolean moviendo = false;


        public ZoomImageView(
                android.content.Context context) {

            super(context);

            setAdjustViewBounds(
                    true
            );

            setScaleType(
                    ImageView.ScaleType.FIT_CENTER
            );


            /*
             * DETECTOR DE ZOOM
             */
            detector =
                    new ScaleGestureDetector(
                            context,
                            new ScaleGestureDetector.SimpleOnScaleGestureListener() {

                                @Override
                                public boolean onScaleBegin(
                                        ScaleGestureDetector detector) {

                                    escalaAnterior =
                                            escala;

                                    return true;
                                }


                                @Override
                                public boolean onScale(
                                        ScaleGestureDetector detector) {

                                    escala *=
                                            detector.getScaleFactor();


                                    /*
                                     * Límites
                                     */
                                    escala =
                                            Math.max(
                                                    1.0f,
                                                    Math.min(
                                                            escala,
                                                            4.0f
                                                    )
                                            );


                                    aplicarTransformacion();

                                    return true;
                                }
                            }
                    );
        }


        /**
         * Aplica zoom + desplazamiento.
         */
        private void aplicarTransformacion() {

            setScaleX(
                    escala
            );

            setScaleY(
                    escala
            );

            setTranslationX(
                    desplazamientoX
            );

            setTranslationY(
                    desplazamientoY
            );
        }


        @Override
        public boolean onTouchEvent(
                MotionEvent event) {

            /*
             * Procesar zoom.
             */
            detector.onTouchEvent(
                    event
            );


            switch (event.getActionMasked()) {


                case MotionEvent.ACTION_DOWN:

                    ultimaX =
                            event.getX();

                    ultimaY =
                            event.getY();

                    moviendo = true;


                    /*
                     * Pedimos al ScrollView que
                     * no intercepte el gesto.
                     */
                    getParent()
                            .requestDisallowInterceptTouchEvent(
                                    true
                            );

                    return true;


                case MotionEvent.ACTION_MOVE:

                    /*
                     * Solo arrastrar cuando
                     * estamos ampliados.
                     */
                    if (escala > 1.0f
                            && event.getPointerCount() == 1) {

                        float nuevaX =
                                event.getX();

                        float nuevaY =
                                event.getY();


                        float diferenciaX =
                                nuevaX - ultimaX;

                        float diferenciaY =
                                nuevaY - ultimaY;


                        desplazamientoX +=
                                diferenciaX;

                        desplazamientoY +=
                                diferenciaY;


                        /*
                         * Limitar el desplazamiento
                         * para evitar perder la página.
                         */
                        float limiteX =
                                getWidth()
                                        * (escala - 1)
                                        / 2f;

                        float limiteY =
                                getHeight()
                                        * (escala - 1)
                                        / 2f;


                        if (limiteX < 0) {
                            limiteX = 0;
                        }

                        if (limiteY < 0) {
                            limiteY = 0;
                        }


                        desplazamientoX =
                                Math.max(
                                        -limiteX,
                                        Math.min(
                                                desplazamientoX,
                                                limiteX
                                        )
                                );


                        desplazamientoY =
                                Math.max(
                                        -limiteY,
                                        Math.min(
                                                desplazamientoY,
                                                limiteY
                                        )
                                );


                        ultimaX =
                                nuevaX;

                        ultimaY =
                                nuevaY;


                        aplicarTransformacion();
                    }


                    return true;


                case MotionEvent.ACTION_POINTER_DOWN:

                    /*
                     * Segundo dedo:
                     * activar zoom.
                     */
                    getParent()
                            .requestDisallowInterceptTouchEvent(
                                    true
                            );

                    return true;


                case MotionEvent.ACTION_POINTER_UP:

                    return true;


                case MotionEvent.ACTION_UP:

                case MotionEvent.ACTION_CANCEL:

                    moviendo = false;


                    getParent()
                            .requestDisallowInterceptTouchEvent(
                                    false
                            );


                    /*
                     * Si volvemos a 1x,
                     * regresar al centro.
                     */
                    if (escala <= 1.0f) {

                        escala = 1.0f;

                        desplazamientoX = 0;

                        desplazamientoY = 0;

                        aplicarTransformacion();
                    }


                    return true;
            }


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
