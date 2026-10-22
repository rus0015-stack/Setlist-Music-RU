package com.rubenubaldo.setlistmusic;

import android.view.ViewParent;
import android.view.ViewConfiguration;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
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
import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends Activity {

    private WebView webView;

    private ValueCallback<Uri[]> filePathCallback;
    private static final int FILE_CHOOSER_REQUEST_CODE = 1001;

    private View pdfViewer;
    private final ArrayList<ZoomImageView> paginasPDF = new ArrayList<>();
    private float escalaPDF = 1.0f;

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

        webView.addJavascriptInterface(new PDFInterface(), "AndroidPDF");

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
                    intent.addCategory(Intent.CATEGORY_OPENABLE);

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

        webView.loadUrl("file:///android_asset/index.html");
    }

    /**
     * Puente entre JavaScript y Android para visualizar PDFs
     * directamente dentro de la aplicación.
     */
    public class PDFInterface {

        @JavascriptInterface
        public void abrirPDF(String base64) {

            runOnUiThread(() -> {

                try {

                    String datos = base64;

                    if (datos.contains(",")) {
                        datos = datos.substring(datos.indexOf(",") + 1);
                    }

                    byte[] pdfBytes = Base64.decode(datos, Base64.DEFAULT);

                    File archivo = new File(
                            getCacheDir(),
                            "chart_setlist_music.pdf"
                    );

                    FileOutputStream fos = new FileOutputStream(archivo);
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
     * Muestra el PDF dentro de la propia aplicación.
     */
    private void mostrarPDF(File archivo) {

        paginasPDF.clear();
        escalaPDF = 1.0f;

        try {

            ParcelFileDescriptor descriptor =
                    ParcelFileDescriptor.open(
                            archivo,
                            ParcelFileDescriptor.MODE_READ_ONLY
                    );

            PdfRenderer renderer = new PdfRenderer(descriptor);

            LinearLayout contenido = new LinearLayout(this);
            contenido.setOrientation(LinearLayout.VERTICAL);
            contenido.setBackgroundColor(Color.WHITE);
            contenido.setPadding(0, 0, 0, 0);

            for (int i = 0; i < renderer.getPageCount(); i++) {

                PdfRenderer.Page page = renderer.openPage(i);

                int ancho = page.getWidth();
                int alto = page.getHeight();

                float escala = 2.0f;

                int nuevoAncho = (int) (ancho * escala);
                int nuevoAlto = (int) (alto * escala);

                Bitmap bitmap = Bitmap.createBitmap(
                        nuevoAncho,
                        nuevoAlto,
                        Bitmap.Config.ARGB_8888
                );

                bitmap.eraseColor(Color.WHITE);

                page.render(
                        bitmap,
                        null,
                        null,
                        PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                );

                page.close();

                ZoomImageView imagen = new ZoomImageView(this);
                paginasPDF.add(imagen);
                imagen.setImageBitmap(bitmap);
imagen.setScaleType(ImageView.ScaleType.FIT_CENTER);
imagen.setAdjustViewBounds(true);
imagen.setPadding(0, 0, 0, 0);

imagen.setLayoutParams(
    new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )
);

                LinearLayout.LayoutParams parametros =
                        new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                        );

                parametros.bottomMargin = 0;

                contenido.addView(imagen, parametros);
            }

            renderer.close();
            descriptor.close();

            ScrollView scroll = new ScrollView(this);
scroll.setBackgroundColor(Color.WHITE);
scroll.setFillViewport(true);
scroll.setPadding(0, 0, 0, 0);
scroll.addView(contenido);

            LinearLayout principal = new LinearLayout(this);
            principal.setOrientation(LinearLayout.VERTICAL);
            principal.setBackgroundColor(Color.rgb(30, 30, 30));

            TextView barra = new TextView(this);

            barra.setText("📄  CHART PDF");
            barra.setTextColor(Color.WHITE);
            barra.setTextSize(17);
            barra.setGravity(Gravity.CENTER_VERTICAL);
            barra.setPadding(20, 15, 20, 15);
            barra.setBackgroundColor(Color.rgb(20, 23, 30));

            TextView cerrar = new TextView(this);

            cerrar.setText("✕  Cerrar");
            cerrar.setTextColor(Color.WHITE);
            cerrar.setTextSize(15);
            cerrar.setGravity(Gravity.CENTER);
            cerrar.setPadding(20, 15, 20, 15);
            cerrar.setBackgroundColor(Color.rgb(65, 95, 220));

            cerrar.setOnClickListener(v -> cerrarPDFAndroid());

            LinearLayout fila = new LinearLayout(this);
            fila.setOrientation(LinearLayout.HORIZONTAL);
            fila.setGravity(Gravity.CENTER_VERTICAL);

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

            principal.addView(fila);

            principal.addView(
                    scroll,
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            0,
                            1
                    )
            );

            pdfViewer = principal;

            setContentView(pdfViewer);

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Error al mostrar el PDF",
                    Toast.LENGTH_LONG
            ).show();

            e.printStackTrace();
        }
    }
private class ZoomImageView extends ImageView {

        private final ScaleGestureDetector detector;
        private long ultimoToqueArriba = 0L;
        private float ultimoToqueX = 0f;
        private float ultimoToqueY = 0f;
        private final int ventanaDobleToque;
        private final float distanciaDobleToque;

        public ZoomImageView(android.content.Context context) {
            super(context);

            setAdjustViewBounds(true);
            setScaleType(ImageView.ScaleType.FIT_CENTER);
            setClickable(true);
            setFocusable(true);
            setPivotX(0f);
            setPivotY(0f);

            ViewConfiguration configuracion = ViewConfiguration.get(context);
            ventanaDobleToque = ViewConfiguration.getDoubleTapTimeout();
            distanciaDobleToque = configuracion.getScaledDoubleTapSlop();

            detector = new ScaleGestureDetector(
                    context,
                    new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                        @Override
                        public boolean onScale(ScaleGestureDetector detectorEscala) {
                            aplicarZoomGlobal(
                                    escalaPDF * detectorEscala.getScaleFactor()
                            );
                            return true;
                        }
                    }
            );
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            detector.onTouchEvent(event);

            ViewParent padre = getParent();
            if (padre != null) {
                // El ScrollView puede desplazarse si no estamos pellizcando.
                padre.requestDisallowInterceptTouchEvent(detector.isInProgress());
            }

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    // No reiniciar el tiempo aquí: se compara al soltar cada toque.
                    return true;

                case MotionEvent.ACTION_POINTER_DOWN:
                    ultimoToqueArriba = 0L;
                    if (padre != null) {
                        padre.requestDisallowInterceptTouchEvent(true);
                    }
                    return true;

                case MotionEvent.ACTION_UP:
                    if (!detector.isInProgress()) {
                        long ahora = event.getEventTime();
                        float x = event.getX();
                        float y = event.getY();
                        long intervalo = ahora - ultimoToqueArriba;
                        float dx = x - ultimoToqueX;
                        float dy = y - ultimoToqueY;
                        float distancia = (float) Math.sqrt(dx * dx + dy * dy);

                        if (ultimoToqueArriba > 0L
                                && intervalo > 0L
                                && intervalo <= ventanaDobleToque
                                && distancia <= distanciaDobleToque) {
                            if (escalaPDF > 1.0f) {
                                aplicarZoomGlobal(1.0f);
                            } else {
                                aplicarZoomGlobal(2.0f);
                            }
                            ultimoToqueArriba = 0L;
                        } else {
                            ultimoToqueArriba = ahora;
                            ultimoToqueX = x;
                            ultimoToqueY = y;
                        }
                    }
                    performClick();
                    if (padre != null) {
                        padre.requestDisallowInterceptTouchEvent(false);
                    }
                    return true;

                case MotionEvent.ACTION_CANCEL:
                    ultimoToqueArriba = 0L;
                    if (padre != null) {
                        padre.requestDisallowInterceptTouchEvent(false);
                    }
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
     * Aplica el mismo zoom a todas las páginas del PDF.
     */
    private void aplicarZoomGlobal(float nuevaEscala) {
        escalaPDF = Math.max(1.0f, Math.min(nuevaEscala, 4.0f));

        for (ZoomImageView pagina : paginasPDF) {
            pagina.setScaleX(escalaPDF);
            pagina.setScaleY(escalaPDF);

            ViewGroup.LayoutParams parametros = pagina.getLayoutParams();
            if (parametros instanceof LinearLayout.LayoutParams) {
                LinearLayout.LayoutParams lp =
                        (LinearLayout.LayoutParams) parametros;

                int altoBase = pagina.getHeight();
                if (altoBase > 0) {
                    lp.bottomMargin = (int) (altoBase * (escalaPDF - 1.0f));
                    pagina.setLayoutParams(lp);
                }
            }
        }
    }

    private void cerrarPDFAndroid() {

        if (pdfViewer != null) {
            pdfViewer = null;
            setContentView(webView);
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_CHOOSER_REQUEST_CODE) {

            Uri[] results = null;

            if (resultCode == RESULT_OK && data != null) {

                if (data.getClipData() != null) {

                    int count = data.getClipData().getItemCount();
                    results = new Uri[count];

                    for (int i = 0; i < count; i++) {

                        results[i] = data.getClipData()
                                .getItemAt(i)
                                .getUri();
                    }

                } else if (data.getData() != null) {

                    results = new Uri[]{
                            data.getData()
                    };
                }
            }

            if (filePathCallback != null) {

                filePathCallback.onReceiveValue(results);
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
