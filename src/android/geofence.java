package geofence;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import com.scb.mb.tw.R;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class echoes a string called from JavaScript.
 */
public class geofence extends CordovaPlugin {

    public static final int ACCESS_FINE_LOCATION_REQUEST_CODE = 1;
    public static String locationUpdateCallbackID;
    public static String localNotificationCallbackID;
    public static CallbackContext geoCallbackContext;
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("initGeofence"))
        {
            geoCallbackContext = callbackContext;
            this.initGeofence(callbackContext);
            return true;
        }
        else if(action.equals("setLocationUpdateEventListener"))
        {
            this.setLocationUpdateEventListener(callbackContext);
            return true;
        }
        else if(action.equals("setLocalNotificationEventListener"))
        {
            this.setLocalNotificationEventListener(callbackContext);
            return true;
        }
        else if(action.equals("getGeofenceStatus"))
        {
            this.getGeofenceStatus(callbackContext);
            return true;
        }
        else if(action.equals("enableGeofence"))
        {
            this.enableGeofence(callbackContext);
            return true;
        }
        else if(action.equals("disableGeofence"))
        {
            this.disableGeofence(callbackContext);
            return true;
        }
        else if(action.equals("sentLocalNotification"))
        {
            this.sentLocalNotification(callbackContext);
            return true;
        }
        else if(action.equals("sentConfirmDialog"))
        {
            this.sentConfirmDialog(callbackContext);
            return true;
        }
        return false;
    }

    private void initGeofence(CallbackContext callbackContext)
    {
        int permission = android.support.v4.content.ContextCompat.checkSelfPermission(cordova.getActivity().getApplicationContext(),android.Manifest.permission.ACCESS_FINE_LOCATION);

        if(permission != PackageManager.PERMISSION_GRANTED)
        {
            android.support.v4.app.ActivityCompat.requestPermissions(cordova.getActivity(),new String[]{Manifest.permission.ACCESS_FINE_LOCATION},ACCESS_FINE_LOCATION_REQUEST_CODE);
        }

        Intent intent = new Intent(cordova.getActivity().getApplicationContext(), EveryTenMinuteCallThisService.class);
        cordova.getActivity().startService(intent);
    }

    private void setLocationUpdateEventListener(CallbackContext callbackContext)
    {
        geoCallbackContext = callbackContext;
        locationUpdateCallbackID = callbackContext.getCallbackId();
    }

    private void setLocalNotificationEventListener(CallbackContext callbackContext)
    {
        localNotificationCallbackID = callbackContext.getCallbackId();
    }

    private void getGeofenceStatus(CallbackContext callbackContext)
    {
        int permission = android.support.v4.content.ContextCompat.checkSelfPermission(cordova.getActivity().getApplicationContext(),android.Manifest.permission.ACCESS_FINE_LOCATION);
        if(permission != PackageManager.PERMISSION_GRANTED)
        {
            boolean boolOfgetGeofenceStatus = false;
            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, boolOfgetGeofenceStatus);
            pluginResult.setKeepCallback(false);
            callbackContext.sendPluginResult(pluginResult);
        }
        else
        {
            boolean boolOfgetGeofenceStatus = true;
            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, boolOfgetGeofenceStatus);
            pluginResult.setKeepCallback(false);
            callbackContext.sendPluginResult(pluginResult);
        }
    }

    private void enableGeofence(CallbackContext callbackContext)
    {
        int permission = android.support.v4.content.ContextCompat.checkSelfPermission(cordova.getActivity().getApplicationContext(),android.Manifest.permission.ACCESS_FINE_LOCATION);
        if(permission != PackageManager.PERMISSION_GRANTED)
        {
            android.support.v4.app.ActivityCompat.requestPermissions(cordova.getActivity(),new String[]{Manifest.permission.ACCESS_FINE_LOCATION},ACCESS_FINE_LOCATION_REQUEST_CODE);
        }
    }

    private void disableGeofence(CallbackContext callbackContext)
    {
        int permission = android.support.v4.content.ContextCompat.checkSelfPermission(cordova.getActivity().getApplicationContext(),android.Manifest.permission.ACCESS_FINE_LOCATION);
        if(permission == PackageManager.PERMISSION_GRANTED)
        {
            android.support.v4.app.ActivityCompat.requestPermissions(cordova.getActivity(),new String[]{Manifest.permission.ACCESS_FINE_LOCATION},ACCESS_FINE_LOCATION_REQUEST_CODE);
        }
    }

    private void sentLocalNotification(CallbackContext callbackContext)
    {
        NotificationManager nm =(NotificationManager) cordova.getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
        Intent it = new Intent(cordova.getActivity().getApplicationContext(), NotificationBroadcastReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(cordova.getActivity(),0,it,PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder notification = new Notification.Builder(cordova.getActivity())
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setOngoing(false)
                .setSmallIcon(R.mipmap.icon)
                .setContentTitle("SCBREEZE")
                .setContentText("你有一則來自渣打銀行活動訊息通知");

        nm.notify(5566,notification.build());
    }
    private void sentConfirmDialog(CallbackContext callbackContext)
    {
        new android.app.AlertDialog.Builder(cordova.getActivity())
                .setTitle("SCBreeze")
                .setMessage("你有一則來自渣打銀活動訊息通知")
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setPositiveButton("檢視", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        callbackContext.success("success");
                    }
                })
                .show();
    }
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException
    {
        switch (requestCode) {
            case ACCESS_FINE_LOCATION_REQUEST_CODE: {
                if (grantResults.length > 0
                        && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                } else {
                }
                return;
            }
        }
    }

    // 發送更新給前台UI, 提供給 webView 專用
    public static void sendUpdate(JSONObject obj){
        Log.d("OK","got update");
        Log.d("OK",obj.toString());
        if (obj != null) {
            PluginResult result = new PluginResult(PluginResult.Status.OK, obj);
            result.setKeepCallback(true);
            geoCallbackContext.sendPluginResult(result);
        } else {
            PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
            result.setKeepCallback(true);
            geoCallbackContext.sendPluginResult(result);
        }
    }
}