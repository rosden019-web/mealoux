package com.example

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.MealouxCream
import com.example.ui.theme.MealouxTheme
import java.io.InputStream

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MealouxTheme {
        MealouxApp()
      }
    }
  }
}

class AndroidWebAppInterface(private val context: Context) {

  @JavascriptInterface
  fun share(title: String?, text: String?, url: String?) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
      type = "text/plain"
      val content = listOfNotNull(
        title?.takeIf { it.isNotBlank() },
        text?.takeIf { it.isNotBlank() },
        url?.takeIf { it.isNotBlank() }
      ).joinToString("\n")
      putExtra(Intent.EXTRA_TEXT, content)
      if (!title.isNullOrBlank()) {
        putExtra(Intent.EXTRA_SUBJECT, title)
      }
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val chooser = Intent.createChooser(shareIntent, title ?: "Partager via")
    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(chooser)
  }

  @JavascriptInterface
  fun vibrate(durationMs: Long) {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        val vibrator = vibratorManager?.defaultVibrator
        vibrator?.vibrate(VibrationEffect.createOneShot(durationMs.coerceIn(10L, 500L), VibrationEffect.DEFAULT_AMPLITUDE))
      } else {
        @Suppress("DEPRECATION")
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        @Suppress("DEPRECATION")
        vibrator?.vibrate(durationMs.coerceIn(10L, 500L))
      }
    } catch (_: Exception) {
      // Ignore vibration errors on devices without vibrator hardware
    }
  }
}

fun getMimeType(path: String): String {
  return when {
    path.endsWith(".html", ignoreCase = true) -> "text/html"
    path.endsWith(".js", ignoreCase = true) || path.endsWith(".mjs", ignoreCase = true) -> "application/javascript"
    path.endsWith(".css", ignoreCase = true) -> "text/css"
    path.endsWith(".webp", ignoreCase = true) -> "image/webp"
    path.endsWith(".png", ignoreCase = true) -> "image/png"
    path.endsWith(".jpg", ignoreCase = true) || path.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
    path.endsWith(".svg", ignoreCase = true) -> "image/svg+xml"
    path.endsWith(".json", ignoreCase = true) -> "application/json"
    path.endsWith(".woff2", ignoreCase = true) -> "font/woff2"
    path.endsWith(".woff", ignoreCase = true) -> "font/woff"
    path.endsWith(".ttf", ignoreCase = true) -> "font/ttf"
    else -> "application/octet-stream"
  }
}

fun openAssetStream(context: Context, rawPath: String): Pair<InputStream, String>? {
  val cleanPath = rawPath.trim().removePrefix("/").substringBefore("?").substringBefore("#")
  val target = if (cleanPath.isEmpty()) "index.html" else cleanPath

  val pathsToTry = listOf(
    target,
    "mealoux/$target",
    target.removePrefix("assets/"),
    target.removePrefix("mealoux/"),
    "mealoux/" + target.removePrefix("assets/")
  ).distinct()

  for (p in pathsToTry) {
    try {
      val stream = context.assets.open(p)
      return Pair(stream, p)
    } catch (_: Exception) {
      if (p.endsWith(".webp", ignoreCase = true)) {
        try {
          val fallback = p.removeSuffix(".webp") + ".png"
          val stream = context.assets.open(fallback)
          return Pair(stream, fallback)
        } catch (_: Exception) {}
      } else if (p.endsWith(".png", ignoreCase = true)) {
        try {
          val fallback = p.removeSuffix(".png") + ".webp"
          val stream = context.assets.open(fallback)
          return Pair(stream, fallback)
        } catch (_: Exception) {}
      }
    }
  }
  return null
}

fun interceptAssetUri(context: Context, uri: Uri): WebResourceResponse? {
  val host = uri.host ?: ""
  if (host != "appassets.androidplatform.net" && host.isNotEmpty()) {
    return null
  }

  val path = uri.path ?: ""
  val targetPath = if (path.isEmpty() || path == "/") "index.html" else path
  val assetResult = openAssetStream(context, targetPath) ?: return null

  val (stream, resolvedPath) = assetResult
  val mimeType = getMimeType(resolvedPath)
  val encoding = if (mimeType.startsWith("text/") || mimeType == "application/javascript" || mimeType == "application/json") "UTF-8" else null

  val response = WebResourceResponse(mimeType, encoding, stream)
  response.responseHeaders = mapOf(
    "Content-Type" to (if (encoding != null) "$mimeType; charset=$encoding" else mimeType),
    "Access-Control-Allow-Origin" to "*",
    "Cache-Control" to "no-cache"
  )
  return response
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MealouxApp() {
  val context = LocalContext.current
  var webViewInstance by remember { mutableStateOf<WebView?>(null) }
  var canGoBack by remember { mutableStateOf(false) }

  BackHandler(enabled = canGoBack) {
    webViewInstance?.let { webView ->
      if (webView.canGoBack()) {
        webView.goBack()
      }
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MealouxCream)
      .safeDrawingPadding()
  ) {
    AndroidView(
      modifier = Modifier
        .fillMaxSize()
        .testTag("mealoux_webview"),
      factory = { ctx ->
        WebView(ctx).apply {
          layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
          )
          setBackgroundColor(android.graphics.Color.parseColor("#FFF4EA"))

          settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            cacheMode = WebSettings.LOAD_DEFAULT
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = true
            allowContentAccess = true
          }

          addJavascriptInterface(AndroidWebAppInterface(ctx), "AndroidBridge")

          webChromeClient = object : WebChromeClient() {}

          webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
              view: WebView?,
              request: WebResourceRequest?
            ): WebResourceResponse? {
              val uri = request?.url ?: return null
              return interceptAssetUri(ctx, uri)
            }

            @Suppress("DEPRECATION")
            override fun shouldInterceptRequest(
              view: WebView?,
              url: String?
            ): WebResourceResponse? {
              val uri = if (url != null) Uri.parse(url) else return null
              return interceptAssetUri(ctx, uri)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
              super.onPageStarted(view, url, favicon)
              canGoBack = view?.canGoBack() == true

              // Polyfill navigator.share to call AndroidBridge
              val polyfillScript = """
                (function() {
                  if (window.AndroidBridge && !window.__androidShareInjected) {
                    window.__androidShareInjected = true;
                    window.navigator.share = function(data) {
                      return new Promise(function(resolve, reject) {
                        try {
                          data = data || {};
                          window.AndroidBridge.share(data.title || '', data.text || '', data.url || '');
                          resolve();
                        } catch (e) {
                          reject(e);
                        }
                      });
                    };
                  }
                })();
              """.trimIndent()
              view?.evaluateJavascript(polyfillScript, null)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
              super.onPageFinished(view, url)
              canGoBack = view?.canGoBack() == true
            }

            override fun shouldOverrideUrlLoading(
              view: WebView?,
              request: WebResourceRequest?
            ): Boolean {
              val uri = request?.url ?: return false
              val host = uri.host ?: ""

              if (host == "appassets.androidplatform.net" || host.isEmpty()) {
                return false
              }

              return try {
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                  addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                ctx.startActivity(intent)
                true
              } catch (_: Exception) {
                false
              }
            }

            @Suppress("DEPRECATION")
            override fun shouldOverrideUrlLoading(
              view: WebView?,
              url: String?
            ): Boolean {
              if (url == null) return false
              val uri = Uri.parse(url)
              val host = uri.host ?: ""

              if (host == "appassets.androidplatform.net" || host.isEmpty()) {
                return false
              }

              return try {
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                  addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                ctx.startActivity(intent)
                true
              } catch (_: Exception) {
                false
              }
            }
          }

          loadUrl("https://appassets.androidplatform.net/index.html")
          webViewInstance = this
        }
      },
      update = { webView ->
        webViewInstance = webView
        canGoBack = webView.canGoBack()
      }
    )
  }
}
