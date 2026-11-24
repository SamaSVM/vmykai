package com.example.vmykai.data.remote

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * kiroe.com.ua puts actual-gpv-graphs behind Cloudflare bot protection keyed on the request's own
 * TLS/JS fingerprint - a plain HTTP client (even with the right cookies, token and headers) gets a
 * 403 "Just a moment..." challenge page, because the request simply isn't coming from a real
 * browser engine. So instead of replaying the request via OkHttp, this drives a hidden WebView to
 * perform the POST itself (via `fetch`, from the page's own JS context), which is indistinguishable
 * from a real visitor.
 */
class GpvGraphsFetcher(private val context: Context) {

    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    suspend fun fetchGraphsJson(pageUrl: String, graphsUrl: String): String = withContext(Dispatchers.Main) {
        val webView = WebView(context.applicationContext)
        val bridge = ResponseBridge()
        try {
            webView.settings.javaScriptEnabled = true
            // Must be attached before the page (and its `window`) is created by loadUrl, otherwise
            // the injected JS below can't see it.
            webView.addJavascriptInterface(bridge, BRIDGE_NAME)

            withTimeout(PAGE_LOAD_TIMEOUT_MS) {
                awaitPageLoad(webView, pageUrl)
            }

            val token = findAjaxToken(webView)
                ?: throw CloudflareBypassFailedException(
                    "Could not locate the ajax token in the page. The site's internal JS structure " +
                        "may have changed; inspect it again via Chrome DevTools."
                )

            withTimeout(FETCH_TIMEOUT_MS) {
                fetchViaPageJs(webView, bridge, graphsUrl, token)
            }
        } finally {
            webView.destroy()
        }
    }

    private suspend fun awaitPageLoad(webView: WebView, url: String) = suspendCancellableCoroutine<Unit> { cont ->
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, finishedUrl: String) {
                if (cont.isActive) cont.resume(Unit)
            }
        }
        webView.loadUrl(url)
    }

    /**
     * Searches the page for its ajax token. The token isn't exposed on `window` directly, so this
     * tries the site's Livewire component data first, then falls back to a literal hex string
     * embedded in inline scripts. Confirmed working against the live site.
     */
    private suspend fun findAjaxToken(webView: WebView): String? = suspendCancellableCoroutine { cont ->
        val script = """
            (function() {
                try {
                    if (window.Livewire && typeof window.Livewire.all === 'function') {
                        for (const component of window.Livewire.all()) {
                            const json = JSON.stringify(component.data || {});
                            const match = json.match(/"[a-zA-Z_]*[tT]oken[a-zA-Z_]*"\s*:\s*"([a-f0-9]{16,64})"/);
                            if (match) return match[1];
                        }
                    }
                    const html = document.documentElement.outerHTML;
                    const literal = html.match(/token['"]?\s*[:=]\s*['"]([a-f0-9]{16,64})['"]/i);
                    if (literal) return literal[1];
                    return null;
                } catch (e) {
                    return null;
                }
            })();
        """.trimIndent()

        webView.evaluateJavascript(script) { rawResult ->
            val unquoted = rawResult?.takeIf { it != "null" }?.trim('"')
            cont.resume(unquoted)
        }
    }

    private suspend fun fetchViaPageJs(webView: WebView, bridge: ResponseBridge, url: String, token: String): String =
        suspendCancellableCoroutine { cont ->
            bridge.continuation = cont
            cont.invokeOnCancellation { bridge.continuation = null }

            val escapedToken = token.replace("\\", "\\\\").replace("'", "\\'")
            val escapedUrl = url.replace("\\", "\\\\").replace("'", "\\'")
            val script = """
                (function() {
                    try {
                        fetch('$escapedUrl?t=' + Date.now(), {
                            method: 'POST',
                            credentials: 'same-origin',
                            headers: {
                                'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
                                'X-Requested-With': 'XMLHttpRequest'
                            },
                            body: 'ajax=1&token=$escapedToken'
                        }).then(function(response) {
                            return response.text().then(function(text) {
                                $BRIDGE_NAME.onResult(response.status + '|' + text);
                            });
                        }).catch(function(error) {
                            $BRIDGE_NAME.onError('rejected: ' + String(error));
                        });
                    } catch (e) {
                        $BRIDGE_NAME.onError('sync: ' + String(e));
                    }
                })();
            """.trimIndent()
            webView.evaluateJavascript(script, null)
        }

    private class ResponseBridge {
        @Volatile var continuation: CancellableContinuation<String>? = null

        @JavascriptInterface
        fun onResult(statusAndText: String) {
            val cont = continuation?.takeIf { it.isActive } ?: return
            continuation = null
            val status = statusAndText.substringBefore('|').toIntOrNull()
            val text = statusAndText.substringAfter('|')
            if (status == 200) {
                cont.resume(text)
            } else {
                cont.resumeWithException(
                    CloudflareBypassFailedException("actual-gpv-graphs responded with $status: ${text.take(300)}"),
                )
            }
        }

        @JavascriptInterface
        fun onError(message: String) {
            val cont = continuation?.takeIf { it.isActive } ?: return
            continuation = null
            cont.resumeWithException(CloudflareBypassFailedException(message))
        }
    }

    companion object {
        private const val BRIDGE_NAME = "VmykaiBridge"
        private const val PAGE_LOAD_TIMEOUT_MS = 20_000L
        private const val FETCH_TIMEOUT_MS = 20_000L
    }
}
