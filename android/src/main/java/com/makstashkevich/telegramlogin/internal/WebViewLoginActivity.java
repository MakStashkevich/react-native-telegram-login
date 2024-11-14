package com.makstashkevich.telegramlogin.internal;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Intent;
import android.app.Activity;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.webkit.CookieSyncManager;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.ValueCallback;
import android.util.Log;
import android.util.JsonReader;
import android.util.JsonToken;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Headers;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;

import java.util.Map;
import java.util.List;
import java.util.Date;
import java.util.ArrayList;
import java.net.HttpCookie;

public class WebViewLoginActivity extends Activity {
  private final String TAG = "WebViewClient";
  private WebView mWebView;

  @SuppressLint("SetJavaScriptEnabled")
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Create WebView
    WebView webView = mWebView = new WebView(this);
    webView.setWebViewClient(new WebViewClient());

    // Setup Cookies
    enableCookies(webView);
    if (savedInstanceState == null) {
      clearCookies();
    }

    // Check whether we're recreating a previously destroyed instance
    if (savedInstanceState != null) {
      // Restore the previous URL and history stack
      webView.restoreState(savedInstanceState);
    }

    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
      // Add JavaScript interface to call back to Java on pre-KITKAT+
      // webView.addJavascriptInterface(null);
    }

    // Setup WebView Default Settings
    setUpWebViewDefaults(webView);

    // Load WebView
    webView.loadUrl("https://oauth.telegram.org/auth?bot_id=7911587539&origin=https%3A%2F%2Ftelegram.org&embed=1&request_access=write&return_to=https%3A%2F%2Ftelegram.org");
    setContentView(webView);
  }

  @Override
  public void onBackPressed() {
    // TODO: set result error!!!
    setResult(RESULT_OK, new Intent());
    finish();
  }

  // Pause WebView on app going to background
  @Override
  public void onPause() {
    mWebView.onPause();
    syncCookies();
    super.onPause();
  }

  // Resume WebView on app going to background
  @Override
  public void onResume() {
    mWebView.onResume();
    super.onResume();
  }

  @Override
  protected void onDestroy() {
    mWebView.destroy();
    super.onDestroy();
  }

  /**
   * Convenience method to set some generic defaults for a
   * given WebView
   *
   * @param webView
   */
  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  private void setUpWebViewDefaults(WebView webView) {
    WebSettings settings = webView.getSettings();

    // Use WideViewport and Zoom out if there is no viewport defined
    settings.setUseWideViewPort(true);
    settings.setLoadWithOverviewMode(true);

    // Enable pinch to zoom without the zoom buttons
    settings.setBuiltInZoomControls(true);

    if(Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB) {
      // Hide the zoom controls for HONEYCOMB+
      settings.setDisplayZoomControls(false);
    }

    // Enable remote debugging via chrome://inspect/#devices
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      WebView.setWebContentsDebuggingEnabled(true);
    }

    // Set custom "User-Agent"
    webView.getSettings().setUserAgentString("Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1");

    // Set custom "Cache-Control" = "no-cache"
    webView.clearCache(true);
//    webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
//    webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
    webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);

    // Enable Javascript with Dom Storage
    webView.getSettings().setJavaScriptEnabled(true);
    webView.getSettings().setDomStorageEnabled(true);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      webView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
      //    webView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
    }
  }

  private void enableCookies(WebView webView) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
      CookieSyncManager.createInstance(this);
    }

    CookieManager cookieManager = CookieManager.getInstance();
    cookieManager.setAcceptFileSchemeCookies(true);

    cookieManager.setAcceptCookie(true);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      cookieManager.setAcceptThirdPartyCookies(webView, true);
    }

    syncCookies();
  }

  private void clearCookies() {
    Log.d(TAG, "Cookie cleared!");
    CookieManager.getInstance().removeAllCookies(null);
    syncCookies();
  }

  private void syncCookies() {
    Log.d(TAG, "Cookie sync");
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      CookieManager.getInstance().flush();
    } else {
      CookieSyncManager.getInstance().sync();
    }
  }

  private void parseTokenFromUrl(String url) {
    Log.d(TAG, url);

    Intent resultIntent = new Intent();
    resultIntent.putExtra("user_id", "12345");

    setResult(RESULT_OK, resultIntent);
    finish();
  }

  private String openAssetFile(String filename) {
    try {
      // https://stackoverflow.com/questions/309424/how-do-i-read-convert-an-inputstream-into-a-string-in-java
      InputStream inputStream = getApplicationContext().getAssets().open(filename);
      ByteArrayOutputStream result = new ByteArrayOutputStream();
      byte[] buffer = new byte[1024];
      for (int length; (length = inputStream.read(buffer)) != -1; ) {
        result.write(buffer, 0, length);
      }
      return result.toString("UTF-8");
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * This method is designed to hide how Javascript is injected into
   * the WebView.
   *
   * In KitKat the new evaluateJavascript method has the ability to
   * give you access to any return values via the ValueCallback object.
   *
   * The String passed into onReceiveValue() is a JSON string, so if you
   * execute a javascript method which return a javascript object, you can
   * parse it as valid JSON. If the method returns a primitive value, it
   * will be a valid JSON object, but you should use the setLenient method
   * to true and then you can use peek() to test what kind of object it is,
   *
   * @param javascript
   * @example https://github.com/googlearchive/chromium-webview-samples/tree/master/jsinterface-example
   */
  private void loadJavascript(String javascript) {
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      // In KitKat+ you should use the evaluateJavascript method
      mWebView.evaluateJavascript(javascript, new ValueCallback<String>() {
        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        @Override
        public void onReceiveValue(String s) {
          JsonReader reader = new JsonReader(new StringReader(s));

          // Must set lenient to parse single values
          reader.setLenient(true);

          try {
            if(reader.peek() != JsonToken.NULL) {
              if(reader.peek() == JsonToken.STRING) {
                String msg = reader.nextString();
                if(msg != null) {
                  Log.i(TAG, "JavaScript response JSON: " + msg);
                }
              }
            }
          } catch (IOException e) {
            Log.e(TAG, "WebViewLoginActivity: IOException", e);
          } finally {
            try {
              reader.close();
            } catch (IOException e) {
              // NOOP
            }
          }
        }
      });
    } else {
      /**
       * For pre-KitKat+ you should use loadUrl("javascript:<JS Code Here>");
       * To then call back to Java you would need to use addJavascriptInterface()
       * and have your JS call the interface
       **/
      mWebView.loadUrl("javascript:"+javascript);
    }
  }

  private class WebViewClient extends android.webkit.WebViewClient {
    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
      Log.d(TAG, "Page loaded with url: " + url);

      String cookies = CookieManager.getInstance().getCookie(url);
      Log.d(TAG, "All the cookies (page started) in a string: " + cookies);

      if (url.startsWith("https://telegram.org/")) {
        parseTokenFromUrl(url);
      } else {
        super.onPageStarted(view, url, favicon);
      }
    }

    @Override
    public void onPageFinished(WebView view, String url) {
      super.onPageFinished(view, url);
      String cookies = CookieManager.getInstance().getCookie(url);
      Log.d(TAG, "All the cookies (page finished) in a string: " + cookies);
      syncCookies();
      view.clearCache(true);

      loadJavascript(openAssetFile("telegram.js"));
    }

    @SuppressLint("NewApi")
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
      if (request != null && request.getUrl() != null && request.getUrl().toString().contains("telegram.org")) {
        String scheme = request.getUrl().getScheme().trim();
        if (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https")) {
//          return executeRequest(request);
        }
      }
      return super.shouldInterceptRequest(view, request);
    }

    private WebResourceResponse executeRequest(WebResourceRequest request) {
      Log.d(TAG, "request.getRequestHeaders(url=" + request.getUrl().toString() + "; method=" + request.getMethod() + ")::" + request.getRequestHeaders());

      OkHttpClient okHttpClient = new OkHttpClient();
      final Call call = okHttpClient.newCall(new Request.Builder()
        .url(request.getUrl().toString())
        .method(request.getMethod(), request.getMethod().equalsIgnoreCase("POST") ? RequestBody.create(null, new byte[0]) : null)
        .headers(Headers.of(request.getRequestHeaders()))
//        .addHeader(HttpHeaders.COOKIE, cookies)
        .build()
      );
      try {
        final Response response = call.execute();

        // Get headers from response
        Headers headerResponse = response.headers();
        Log.d(TAG, request.getUrl().toString() + " /// " + headerResponse.toString());
        if (headerResponse.size() > 0 && headerResponse.get("Set-Cookie") != null) {
          List<HttpCookie> httpCookies = HttpCookie.parse(headerResponse.get("Set-Cookie"));
          Log.d(TAG, headerResponse.get("Set-Cookie"));
          Log.d(TAG, httpCookies.toString());

          String domain = "https://" + request.getUrl().getHost();
          for (HttpCookie cookie : httpCookies) {
//            if (cookie.hasExpired()) {
//              continue;
//            }

            String cookieString = cookie.getName() + "=" + cookie.getValue();
            Log.d(TAG, "CookieString : " + cookieString);
            CookieManager.getInstance().setCookie(domain, cookieString);
          }
          syncCookies();
        }

        return null;
//        return new WebResourceResponse(
//          response.header("content-type", "text/html; charset=utf-8"),
//          response.header("content-encoding", "utf-8"),
//          response.body().byteStream()
//        );
      } catch (IOException e) {
        e.printStackTrace();
        return null;
      }
    }

    @Override
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
      Log.v("USERAGENTBROWSE", "shouldOverrideUrlLoading api >= 21 called");
      return false;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
      Log.v("USERAGENTBROWSE", "shouldOverrideUrlLoading api < 21 called");
      return false;
    }
  }
}
