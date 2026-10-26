package wang.xiaonuan.words

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import wang.xiaonuan.words.ui.theme.NancyWordsTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemUI()

        setContent {
            NancyWordsTheme {
                WebViewScreen(url = "https://words.xiaonuan.wang")
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(url: String) {
    var webView: WebView? by remember { mutableStateOf(null) }
    var canGoBack by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }

    // 当处于错误状态时，每隔 5 秒自动重试
    LaunchedEffect(isError) {
        if (isError) {
            delay(5000)
            isError = false
            reloadWebView(webView, url)
        }
    }

    BackHandler(enabled = canGoBack && !isError) {
        webView?.goBack()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    // 电视遥控器方向键与焦点响应配置
                    isFocusable = true
                    isFocusableInTouchMode = true
                    requestFocus()

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        builtInZoomControls = true
                        displayZoomControls = false
                        setSupportZoom(true)
                        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        allowFileAccess = true
                        allowContentAccess = true
                        cacheMode = WebSettings.LOAD_NO_CACHE
                    }

                    clearCache(true)

                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            return false
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            canGoBack = view?.canGoBack() == true
                            view?.requestFocus()
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            super.onReceivedError(view, request, error)
                            if (request?.isForMainFrame == true) {
                                isError = true
                            }
                        }
                    }

                    webChromeClient = WebChromeClient()

                    loadUrl(url)
                    webView = this
                }
            },
            update = {
                canGoBack = it.canGoBack()
            },
            modifier = Modifier.fillMaxSize()
        )

        // 网站无法访问时显示的重试界面
        if (isError) {
            ErrorRetryScreen(
                onRetryNow = {
                    isError = false
                    reloadWebView(webView, url)
                }
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webView?.destroy()
        }
    }
}

private fun reloadWebView(webView: WebView?, defaultUrl: String) {
    if (webView != null) {
        val currentUrl = webView.url
        if (!currentUrl.isNullOrEmpty() && currentUrl != "about:blank") {
            webView.reload()
        } else {
            webView.loadUrl(defaultUrl)
        }
    }
}

@Composable
fun ErrorRetryScreen(
    onRetryNow: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "网络连接失败或网站暂时无法访问",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "正在自动重试连接，请稍候...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRetryNow) {
                Text("立即重试")
            }
        }
    }
}