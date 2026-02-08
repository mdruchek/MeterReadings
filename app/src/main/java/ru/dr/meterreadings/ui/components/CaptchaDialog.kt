package ru.dr.meterreadings.ui.components

import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import org.json.JSONObject

data class CaptchaSession(
    val token: String,
    val userAgent: String,
    val cookies: String = ""
)

@Composable
fun CaptchaDialog(
    onCaptchaCompleted: (CaptchaSession) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.8f),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Проверка безопасности",
                        style = MaterialTheme.typography.titleLarge
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть")
                    }
                }

                HorizontalDivider()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    var isLoading by remember { mutableStateOf(true) }
                    var webViewRef by remember { mutableStateOf<WebView?>(null) }

                    AndroidView(
                        factory = { context ->
                            // ✅ Включаем отладку WebView (для Chrome DevTools)
                            WebView.setWebContentsDebuggingEnabled(true)

                            WebView(context).apply {
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    loadWithOverviewMode = true
                                    useWideViewPort = true
                                }

                                // ✅ Включаем cookies (хотя они не нужны)
                                CookieManager.getInstance().setAcceptCookie(true)
                                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                                webViewRef = this

                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(
                                        view: WebView?,
                                        url: String?,
                                        favicon: Bitmap?
                                    ) {
                                        isLoading = true
                                        println("🔵 [CaptchaDialog] onPageStarted url=$url")
                                    }

                                    override fun onPageFinished(
                                        view: WebView?,
                                        url: String?
                                    ) {
                                        isLoading = false
                                        println("🟢 [CaptchaDialog] onPageFinished url=$url")

                                        view?.evaluateJavascript(
                                            """
                                            (function() {
                                                console.log('🔧 [CaptchaDialog] Init XHR interceptor');

                                                const originalOpen = XMLHttpRequest.prototype.open;
                                                const originalSend = XMLHttpRequest.prototype.send;

                                                XMLHttpRequest.prototype.open = function(method, url, ...rest) {
                                                    this._url = url;
                                                    this._method = method;
                                                    console.log('📡 XHR open:', method, url);
                                                    return originalOpen.apply(this, [method, url, ...rest]);
                                                };

                                                XMLHttpRequest.prototype.send = function(...args) {
                                                    const xhr = this;
                                                    const url = xhr._url || '';
                                                    let body = null;
                                                    
                                                    try {
                                                        body = args && args.length > 0 ? args[0] : null;
                                                    } catch (e) {
                                                        console.log('⚠️ body access error:', e);
                                                    }

                                                    // ✅ ЕСЛИ ЭТО GetAbonentInfo — ПЕРЕХВАТЫВАЕМ ТОКЕН
                                                    if (url.includes('GetAbonentInfo') && body) {
                                                        console.log('🔍 [CaptchaDialog] Intercepting GetAbonentInfo');
                                                        
                                                        let captchaToken = null;
                                                        try {
                                                            const reqJson = JSON.parse(body);
                                                            captchaToken = reqJson.captchaToken || null;
                                                        } catch (e) {
                                                            console.log('⚠️ Failed to parse body:', e);
                                                        }

                                                        if (captchaToken && window.Android) {
                                                            console.log('📲 [CaptchaDialog] Sending FRESH token to Android');
                                                            console.log('   Token (first 80):', captchaToken.substring(0, 80));
                                                            
                                                            window.Android.onCaptchaToken(JSON.stringify({
                                                                captchaToken: captchaToken
                                                            }));
                                                            
                                                            console.log('🚫 [CaptchaDialog] BLOCKING WebView request');
                                                            // ✅ НЕ ОТПРАВЛЯЕМ ЗАПРОС ОТ WEBVIEW!
                                                            return;
                                                        }
                                                    }

                                                    // Все остальные запросы отправляем как обычно
                                                    return originalSend.apply(xhr, args);
                                                };

                                                console.log('✅ [CaptchaDialog] XHR interceptor installed');
                                            })();
                                            """.trimIndent(),
                                            null
                                        )
                                    }
                                }

                                addJavascriptInterface(
                                    object {
                                        @JavascriptInterface
                                        fun onCaptchaToken(data: String) {
                                            println("🔐 [CaptchaDialog] onCaptchaToken called")
                                            println("   Raw data: $data")

                                            try {
                                                val jsonData = JSONObject(data)
                                                val captchaToken = jsonData.getString("captchaToken")

                                                println("   Token (first 80): ${captchaToken.take(80)}")

                                                val currentWebView = this@apply

                                                Handler(Looper.getMainLooper()).post {
                                                    try {
                                                        val userAgent = currentWebView.settings.userAgentString

                                                        val session = CaptchaSession(
                                                            token = captchaToken,
                                                            userAgent = userAgent,
                                                            cookies = ""
                                                        )

                                                        println("✅ [CaptchaDialog] Got FRESH token, closing dialog")

                                                        // ✅ ЗАКРЫВАЕМ ДИАЛОГ СРАЗУ!
                                                        onCaptchaCompleted(session)
                                                        onDismiss()
                                                    } catch (e: Exception) {
                                                        println("❌ [CaptchaDialog] Error: ${e.message}")
                                                        e.printStackTrace()
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                println("❌ [CaptchaDialog] Parse error: ${e.message}")
                                                e.printStackTrace()
                                            }
                                        }
                                    },
                                    "Android"
                                )

                                loadUrl("https://send.kvc-nn.ru/")
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator()
                                Text(
                                    text = "Загрузка...",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Пройдите проверку и нажмите «Найти лицевой счёт»",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
