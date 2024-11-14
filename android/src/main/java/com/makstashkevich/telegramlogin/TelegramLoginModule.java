package com.makstashkevich.telegramlogin;

import androidx.annotation.NonNull;

import com.facebook.react.module.annotations.ReactModule;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.util.Log;
import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;

import com.makstashkevich.telegramlogin.internal.WebViewLoginActivity;

import java.util.Base64;

@ReactModule(name = TelegramLoginModule.NAME)
public class TelegramLoginModule extends ReactContextBaseJavaModule {
  public static final String NAME = "TelegramLogin";
  private final String TAG = "TelegramLoginModule";

  private static final int TELEGRAM_LOGIN_REQUEST = 1;
  private static final String E_ACTIVITY_DOES_NOT_EXIST = "E_ACTIVITY_DOES_NOT_EXIST";
  private static final String E_FAILED_TO_SHOW_LOGIN = "E_FAILED_TO_SHOW_LOGIN";
  private static final String E_FAILED_HANDLE_RESULT = "E_FAILED_HANDLE_RESULT";
  @Nullable
  private Promise mTelegramLoginPromise;

  public TelegramLoginModule(ReactApplicationContext reactContext) {
    super(reactContext);
    ActivityEventListener activityEventListener = new BaseActivityEventListener() {
      @Override
      public void onActivityResult(Activity activity, int requestCode, int resultCode, @Nullable Intent data) {
        Log.d(TAG, "reqCode = " + requestCode);
        if (requestCode == TELEGRAM_LOGIN_REQUEST) {
          Log.d(TAG, "TELEGRAM_LOGIN_REQUEST = " + requestCode);
          if (mTelegramLoginPromise != null) {
              Log.d(TAG, "data = " + data);
              resolveToken();
          }
        } else {
          super.onActivityResult(activity, requestCode, resultCode, data);
        }
      }
    };

    reactContext.addActivityEventListener(activityEventListener);
  }

  @Override
  @NonNull
  public String getName() {
    return NAME;
  }

  @ReactMethod
  public void login(final Promise promise) {
    Activity currentActivity = getCurrentActivity();

    if (currentActivity == null) {
      promise.reject(E_ACTIVITY_DOES_NOT_EXIST, "Activity doesn't exist");
      return;
    }

    mTelegramLoginPromise = promise;

    try {
      Intent intent = new Intent(getReactApplicationContext(), WebViewLoginActivity.class);
//      intent.putExtra("EXTRA_OPTIONS", options)

      currentActivity.startActivityForResult(intent, TELEGRAM_LOGIN_REQUEST);
    } catch (Exception e) {
      mTelegramLoginPromise.reject(E_FAILED_TO_SHOW_LOGIN, e);
      mTelegramLoginPromise = null;
    }
  }

  private void resolveToken() {
    try {
      WritableMap params = Arguments.createMap();
      params.putString("token", "test-token");
      mTelegramLoginPromise.resolve(params);
    } catch (Exception e) {
      mTelegramLoginPromise.reject(E_FAILED_HANDLE_RESULT, e);
      mTelegramLoginPromise = null;
    }
  }
}
