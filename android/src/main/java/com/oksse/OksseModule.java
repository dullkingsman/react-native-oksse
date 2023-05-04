package com.oksse;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.module.annotations.ReactModule;

import java.util.Map;
import java.util.HashMap;

import java.util.Arrays;
import java.io.IOException;

import android.util.Log;
import androidx.annotation.WorkerThread;

import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.ReadableMap;

import okhttp3.Request;
import okhttp3.Response;
import okhttp3.HttpUrl;
import okhttp3.Headers;

import okio.BufferedSource;

import com.here.oksse.OkSse;
import com.here.oksse.ServerSentEvent;

@ReactModule(name = OksseModule.NAME)
public class OksseModule extends ReactContextBaseJavaModule {

  public static final String NAME = "EventSource";

  ReactApplicationContext reactContext;
  OkSse okSse;
  Map<String, Source> sources = new HashMap<>();

  public OksseModule(ReactApplicationContext reactContext) {
    super(reactContext);

    this.reactContext = reactContext;
    this.okSse = new OkSse();
  }

  @Override
  @NonNull
  public String getName() {
    return NAME;
  }

  @ReactMethod
  public void initialize(String hash, ReadableMap args) {
    sources.put(hash, new Source(
      this.reactContext,
      this.okSse,
      args
    ));
  }

  @ReactMethod
  public void close(String hash) {
    Source _source = sources.get(hash);

    if (_source != null) {
      _source.close();
      sources.remove(hash);
    }
  }

  private int listenerCount = 0;

  @ReactMethod
  public void addListener(String eventName) {
    if (listenerCount == 0) {
      // Set up any upstream listeners or background tasks as necessary
    }

    listenerCount += 1;
  }

  @ReactMethod
  public void removeListeners(Integer count) {
    listenerCount -= count;

    if (listenerCount == 0) {
      // Remove upstream listeners, stop unnecessary background tasks
    }
  }

}

class Source {

  private String url;
  private Request request;
  private ServerSentEvent sse;

  Source(
    ReactApplicationContext reactContext,
    OkSse okSse,
    ReadableMap args
  ) {

    String url = args.hasKey("url")? args.getString("url"): "";
    ReadableMap config = args.hasKey("config")? args.getMap("config"): null;

    HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();

    if (config != null) {
      ReadableMap query = config.hasKey("query")? config.getMap("query"): null;

      if (query != null) {
        for (Map.Entry<String, Object> entry: query.toHashMap().entrySet()) {
          String key = entry.getKey();
          String value = entry.getValue().toString();

          Log.d("EVENT_SOURCE", "qKey: " + key + "and aValue: " + value);

          urlBuilder.addQueryParameter(key, value);
        }
      }
    }

    this.url = urlBuilder.build().toString();

    Log.d("EVENT_SOURCE", "URL: " + this.url);

    Request.Builder requestBuilder = new Request.Builder().url(this.url);

    if (config != null) {
      ReadableMap headers = config.hasKey("headers")? config.getMap("headers"): null;

      if (headers != null) {
        for (Map.Entry<String, Object> entry: headers.toHashMap().entrySet()) {
          String key = entry.getKey();
          String value = entry.getValue().toString();

          Log.d("EVENT_SOURCE", "hKey: " + key + "and hValue: " + value);

          requestBuilder.addHeader(key, value);
        }
      }
    }

    this.request = requestBuilder.build();

    this.sse = okSse.newServerSentEvent(request, new ServerSentEvent.Listener() {

      @Override
      public void onOpen(ServerSentEvent sse, Response response) {
        Log.d("EVENT_SOURCE", "||======================================================");
        Log.d("EVENT_SOURCE", "CONNECTION_OPEN");

        reactContext
          .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
          .emit("OPEN", null);
      }

      @Override
      public void onMessage(ServerSentEvent sse, String id, String event, String data) {
        WritableMap evt = new WritableNativeMap();

        evt.putString("id", id);
        evt.putString("event", event);
        evt.putString("data", data);

        Log.d("EVENT_SOURCE", "|-------------------------------------------------------");
        Log.d("EVENT_SOURCE", "MESSAGE: " + evt.toString());
        Log.d("EVENT_SOURCE", "|-------------------------------------------------------");

        reactContext
          .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
          .emit("MESSAGE", evt);
      }

      @WorkerThread
      @Override
      public void onComment(ServerSentEvent sse, String comment) {
        Log.d("EVENT_SOURCE", "|-------------------------------------------------------");
        Log.d("EVENT_SOURCE", "COMMENT: " + comment);
        Log.d("EVENT_SOURCE", "|-------------------------------------------------------");

        reactContext
          .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
          .emit("COMMENT", comment);
      }

      @WorkerThread
      @Override
      public boolean onRetryTime(ServerSentEvent sse, long retryTime) {
        Log.d("EVENT_SOURCE", "|-------------------------------------------------------");
        Log.d("EVENT_SOURCE", "RETRY_TIME: " + retryTime);
        Log.d("EVENT_SOURCE", "|-------------------------------------------------------");

        reactContext
          .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
          .emit("RETRY_TIME", (double) retryTime);

        return true; // True to use the new retry time received by SSE
      }

      @WorkerThread
      @Override
      public boolean onRetryError(ServerSentEvent sse, Throwable throwable, Response response) {
        WritableMap errorRes = new WritableNativeMap();

        if (throwable != null) {
          WritableMap throwableMap = new WritableNativeMap();

          String message = throwable.getMessage();
          StackTraceElement[] stackTraceElements = throwable.getStackTrace();
          String stackTrace = Arrays.toString(stackTraceElements);

          throwableMap.putString("message", message);
          throwableMap.putString("stackTrace", stackTrace);


          errorRes.putMap("throwable", throwableMap);
        } else {
          errorRes.putMap("throwable", null);
        }

        if (response != null) {
          WritableMap responseMap = new WritableNativeMap();

          responseMap.putString("status", Integer.toString(response.code()));

          String responseBodyString = null;

          try {
            BufferedSource reponseBodyBufferedSource = response.body().source();
            responseBodyString = reponseBodyBufferedSource.readUtf8();
          } catch (IOException e) {}

          responseMap.putString("body", responseBodyString);

          Headers headers = response.headers();

          WritableMap headersMap = new WritableNativeMap();

          for (String key : headers.names()) {
            headersMap.putString(key, headers.get(key));
          }

          responseMap.putMap("headers", headersMap);

          errorRes.putMap("response", responseMap);
        } else {
          errorRes.putMap("response", null);
        }

        Log.d("EVENT_SOURCE", "|-------------------------------------------------------");
        Log.d("EVENT_SOURCE", "RETRY_ERROR: " + errorRes.toString());
        Log.d("EVENT_SOURCE", "|-------------------------------------------------------");

        reactContext
          .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
          .emit("RETRY_ERROR", errorRes);

        return true; // True to retry, false otherwise
      }

      @WorkerThread
      @Override
      public void onClosed(ServerSentEvent sse) {
        Log.d("EVENT_SOURCE", "CONNECTION_CLOSED");
        Log.d("EVENT_SOURCE", "||======================================================");

        reactContext
          .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
          .emit("CLOSED", null);
      }

      @Override
      public Request onPreRetry(ServerSentEvent sse, Request originalRequest) {
        Log.d("EVENT_SOURCE", "|-------------------------------------------------------");
        Log.d("EVENT_SOURCE", "ABOUT_TO_RETRY");
        Log.d("EVENT_SOURCE", "|-------------------------------------------------------");

        return originalRequest; // A requet object to retry, null otherwise
      }

    });

  }

  void close() {
    this.sse.close();
  }
}
