package geofence;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.webkit.WebChromeClient;

import com.scb.mb.tw.MainActivity;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPreferences;
import org.apache.cordova.CordovaResourceApi;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CordovaWebViewEngine;
import org.apache.cordova.ICordovaCookieManager;
import org.apache.cordova.PluginEntry;
import org.apache.cordova.PluginManager;
import org.apache.cordova.PluginResult;

import java.util.List;
import java.util.Map;

public class NotificationBroadcastReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(Context context, Intent intent)
    {
          Intent it = new Intent(context, MainActivity.class);
          context.startActivity(it);
          geofence theGeofence = new geofence();
          CallbackContext callbackContext = theGeofence.geoCallbackContext;
          PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, "MESSAGE_TYPE_STRING");
          pluginResult.setKeepCallback(false);
          callbackContext.sendPluginResult(pluginResult);
          callbackContext.success("success");
    }
}
