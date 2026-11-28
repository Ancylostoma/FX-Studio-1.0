package com.ancylostoma.tutor;

import android.app.Activity;
import android.content.ClipData;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.webkit.WebViewAssetLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Contenedor nativo de Mis Tutorías: la app corre en su propio WebView
 * con los archivos dentro de la APK (sin Chrome y sin internet).
 */
public class MainActivity extends Activity {

  private static final String HOST = "appassets.androidplatform.net";
  private static final String START_URL = "https://" + HOST + "/assets/www/index.html";
  private static final int THEME_COLOR = Color.parseColor("#1F3A5F");
  private static final int REQ_PICK_FILE = 1;
  private static final int REQ_SAVE_FILE = 2;
  private static final int REQ_NOTIFICACIONES = 3;

  private WebView webView;
  private ValueCallback<Uri[]> pendingFileCallback;
  private byte[] pendingSaveBytes;
  private String pendingSaveMime;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
    getWindow().setStatusBarColor(Color.TRANSPARENT);
    getWindow().setNavigationBarColor(Color.TRANSPARENT);

    FrameLayout root = new FrameLayout(this);
    root.setBackgroundColor(THEME_COLOR);
    webView = new WebView(this);
    webView.setBackgroundColor(Color.parseColor("#EEF2F7"));
    root.addView(webView, new FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
    setContentView(root);

    WindowInsetsControllerCompat bars = new WindowInsetsControllerCompat(getWindow(), root);
    bars.setAppearanceLightStatusBars(false);
    bars.setAppearanceLightNavigationBars(false);

    // Deja la web entre la barra de estado, la de navegación y el teclado.
    ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
      Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars()
          | WindowInsetsCompat.Type.displayCutout());
      Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
      v.setPadding(sys.left, sys.top, sys.right, Math.max(sys.bottom, ime.bottom));
      return WindowInsetsCompat.CONSUMED;
    });

    WebSettings s = webView.getSettings();
    s.setJavaScriptEnabled(true);
    s.setDomStorageEnabled(true);
    s.setDatabaseEnabled(true);
    s.setAllowFileAccess(false);
    s.setAllowContentAccess(false);

    WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
        .setDomain(HOST)
        .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
        .build();

    webView.addJavascriptInterface(new Bridge(), "AndroidBridge");

    webView.setWebViewClient(new WebViewClient() {
      @Override
      public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        return assetLoader.shouldInterceptRequest(request.getUrl());
      }

      @Override
      public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        Uri uri = request.getUrl();
        if (HOST.equals(uri.getHost())) return false;
        // Enlaces externos (WhatsApp, teléfono, web) se abren en su app.
        try {
          startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException e) {
          Toast.makeText(MainActivity.this, "No hay app para abrir este enlace", Toast.LENGTH_SHORT).show();
        }
        return true;
      }
    });

    webView.setWebChromeClient(new WebChromeClient() {
      @Override
      public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                                       FileChooserParams params) {
        if (pendingFileCallback != null) pendingFileCallback.onReceiveValue(null);
        pendingFileCallback = callback;
        // "*/*" porque muchos exploradores no marcan los .json como application/json.
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("*/*");
        try {
          startActivityForResult(Intent.createChooser(intent, "Elegir respaldo"), REQ_PICK_FILE);
        } catch (ActivityNotFoundException e) {
          pendingFileCallback = null;
          return false;
        }
        return true;
      }
    });

    if (savedInstanceState != null) {
      webView.restoreState(savedInstanceState);
    } else {
      webView.loadUrl(START_URL);
    }
  }

  private class Bridge {
    /** Comparte un archivo (el reporte Word). Con app = "whatsapp" abre WhatsApp, que pregunta a qué chat enviarlo. */
    @JavascriptInterface
    public void shareFile(String name, String mime, String base64, String app) {
      byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
      runOnUiThread(() -> compartir(name, mime, bytes, app));
    }

    /** Datos para el aviso diario (ver Recordatorio). */
    @JavascriptInterface
    public void setRecordatorio(String json) {
      Recordatorio.guardar(getApplicationContext(), json);
    }

    /** Android 13+ exige pedir permiso para mostrar notificaciones. */
    @JavascriptInterface
    public void pedirPermisoNotificaciones() {
      runOnUiThread(() -> {
        if (Build.VERSION.SDK_INT >= 33
            && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
          requestPermissions(new String[] {"android.permission.POST_NOTIFICATIONS"}, REQ_NOTIFICACIONES);
        } else if (!NotificationManagerCompat.from(MainActivity.this).areNotificationsEnabled()) {
          Toast.makeText(MainActivity.this,
              "Las notificaciones de Mis Tutorías están apagadas en los ajustes del teléfono", Toast.LENGTH_LONG).show();
        }
      });
    }

    @JavascriptInterface
    public void saveFile(String name, String mime, String base64) {
      byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
      runOnUiThread(() -> {
        pendingSaveBytes = bytes;
        pendingSaveMime = mime;
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType(mime)
            .putExtra(Intent.EXTRA_TITLE, name);
        try {
          startActivityForResult(intent, REQ_SAVE_FILE);
        } catch (ActivityNotFoundException e) {
          pendingSaveBytes = null;
          Toast.makeText(MainActivity.this, "No se pudo guardar el archivo", Toast.LENGTH_LONG).show();
        }
      });
    }
  }

  private void compartir(String name, String mime, byte[] bytes, String app) {
    try {
      File carpeta = new File(getCacheDir(), "compartir");
      if (!carpeta.isDirectory() && !carpeta.mkdirs()) throw new IOException("sin carpeta");
      File[] viejos = carpeta.listFiles();
      if (viejos != null) for (File f : viejos) f.delete();
      String limpio = name.replaceAll("[\\\\/:*?\"<>|]+", " ").trim();
      File archivo = new File(carpeta, limpio.isEmpty() ? "reporte.docx" : limpio);
      try (FileOutputStream out = new FileOutputStream(archivo)) {
        out.write(bytes);
      }
      Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".archivos", archivo);
      Intent enviar = new Intent(Intent.ACTION_SEND)
          .setType(mime)
          .putExtra(Intent.EXTRA_STREAM, uri)
          .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
      enviar.setClipData(ClipData.newRawUri(limpio, uri));
      if ("whatsapp".equals(app)) {
        for (String paquete : new String[] {"com.whatsapp", "com.whatsapp.w4b"}) {
          try {
            startActivity(new Intent(enviar).setPackage(paquete));
            return;
          } catch (ActivityNotFoundException noInstalada) {
            // Se prueba con WhatsApp Business y, si tampoco está, con cualquier app.
          }
        }
        Toast.makeText(this, "WhatsApp no está instalado: elige otra app", Toast.LENGTH_LONG).show();
      }
      startActivity(Intent.createChooser(enviar, "Enviar reporte"));
    } catch (Exception e) {
      Toast.makeText(this, "No se pudo compartir: " + e.getMessage(), Toast.LENGTH_LONG).show();
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == REQ_PICK_FILE) {
      if (pendingFileCallback == null) return;
      Uri[] result = null;
      if (resultCode == RESULT_OK && data != null && data.getData() != null) {
        result = new Uri[] {data.getData()};
      }
      pendingFileCallback.onReceiveValue(result);
      pendingFileCallback = null;
    } else if (requestCode == REQ_SAVE_FILE) {
      byte[] bytes = pendingSaveBytes;
      pendingSaveBytes = null;
      if (resultCode != RESULT_OK || data == null || data.getData() == null || bytes == null) return;
      try (OutputStream out = getContentResolver().openOutputStream(data.getData())) {
        out.write(bytes);
        Toast.makeText(this, "Archivo guardado", Toast.LENGTH_SHORT).show();
        abrirSiEsWord(data.getData());
      } catch (Exception e) {
        Toast.makeText(this, "Error al guardar: " + e.getMessage(), Toast.LENGTH_LONG).show();
      }
    }
  }

  /** Tras guardar un reporte, lo abre en Word (o la app de documentos que haya) para imprimirlo. */
  private void abrirSiEsWord(Uri uri) {
    if (pendingSaveMime == null || !pendingSaveMime.contains("wordprocessingml")) return;
    Intent ver = new Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, pendingSaveMime)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    try {
      startActivity(ver);
    } catch (Exception e) {
      Toast.makeText(this, "Reporte guardado. Instala Word o Google Docs para abrirlo.", Toast.LENGTH_LONG).show();
    }
  }

  @Override
  public void onBackPressed() {
    // Si hay una ventana abierta en la web, Atrás la cierra primero.
    webView.evaluateJavascript("window.cerrarModal ? window.cerrarModal() : false", cerro -> {
      if (!"true".equals(cerro)) volver();
    });
  }

  private void volver() {
    // El Atrás del teléfono vuelve a la sección anterior (lista de alumnos).
    if (webView.canGoBack()) {
      webView.goBack();
    } else {
      super.onBackPressed();
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    webView.saveState(outState);
  }
}
