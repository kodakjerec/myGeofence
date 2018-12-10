package geofence;

import android.Manifest;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import com.mitake.android.scb.R;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
/**
 * This class echoes a string called from JavaScript.
 */
public class geofence extends CordovaPlugin {
    private static final int ACCESS_FINE_LOCATION_REQUEST_CODE = 1;
    private static CallbackContext myCallbackContext;
    private Handler geoFenceHandler_Terminate =new Handler();
    private Runnable geoFenceRunable_Terminate;
    private int deadTime = 30*1000; //背景模式下, 送出api多久沒回應就關閉app
    int startJobTime = 3*1000; // 第一次開啟服務, 需要幾秒
    String TAG = "geoFenceCordovaPlugin";
    String notificationGroup = R.class.getCanonicalName();
    static SharedPreferences settings;
    static JSONObject languageJson; // 系統預設中英字串

    private String [] permissions = { Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION };

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        Log.d(TAG,action);

        switch (action){
            case "initGeofence":
                this.initGeofence(callbackContext);return true;
            case "setLocationUpdateEventListener":
                this.setLocationUpdateEventListener(callbackContext);return true;
            case "setLocalNotificationEventListener":
                this.setLocalNotificationEventListener(callbackContext);return true;
            case "getGeofenceStatus":
                this.getGeofenceStatus(callbackContext);return true;
            case "enableGeofence":
                this.enableGeofence(callbackContext);return true;
            case "disableGeofence":
                this.disableGeofence(callbackContext);return true;
            case "sentLocalNotification":
                this.sentLocalNotification(callbackContext, args.getString( 0 ));return true;
            case "sentConfirmDialog":
                // 因為使用了背景喚醒 app 處理呼叫 api (EveryTenMinuteCallThisService.java)
                // 會導致在背景執行的"cordova" app, 誤認為自己是在前景
                // android就要幫忙在發送本地通知時, 先判斷前景背景=> 是背景狀態時, 轉而導到localNotification
                this.sentConfirmDialog( callbackContext );return true;
            case "isForeGround":
                boolean isForeGround = !isAppIsInBackground( cordova.getContext());

                PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, isForeGround);
                pluginResult.setKeepCallback(false);
                callbackContext.sendPluginResult(pluginResult);
                return  true;
            case "language":
                languageJson = args.getJSONObject( 0 );
                return  true;
        }

        return false;
    }
    // check if app is in Background
    // build_version >= 5.0 (API 21)
    private boolean isAppIsInBackground(Context context) {
        boolean isInBackground = true;
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningProcesses = am.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
            if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                for (String activeProcess : processInfo.pkgList) {
                    if (activeProcess.equals(context.getPackageName())) {
                        isInBackground = false;
                    }
                }
            }
        }

        return isInBackground;
    }

    private void initGeofence(CallbackContext callbackContext)
    {
        boolean permission = hasPermisssion();
        if(!permission)
        {
            ActivityCompat.requestPermissions(cordova.getActivity(),permissions,ACCESS_FINE_LOCATION_REQUEST_CODE);
        }

        // 取得SharedPreference設定("pushEvent"為設定檔的名稱)
        settings = cordova.getActivity().getApplicationContext().getSharedPreferences("pushEvent", 0);
        
        // 啟用地理柵欄
        Boolean isGenfenceEnable = settings.getBoolean("geoFence_GeofenceStatus", false);
        if(isGenfenceEnable){
            startJobScheduler();
        } else {
            stopJobScheduler();
        }

        myCallbackContext = callbackContext;
    }

    private void setLocationUpdateEventListener(CallbackContext callbackContext)
    {
        // 檢查上一輪接收到地理位置變更卻app killed
        String checkedLocationUpdate = settings.getString("geoFence_LocationUpdate", "");
        Log.d(TAG,"檢查上一輪的地理位置變更，checkedNotification:"+String.valueOf( checkedLocationUpdate ));

        if(!checkedLocationUpdate.equals("") ){
            Log.d(TAG,"上一輪接收到地理位置變更, 卻被app killed");
            // 1. 完成上一輪未完成的資訊
            try {
                JSONObject obj = new JSONObject( checkedLocationUpdate );
                sendUpdate(obj);
                terminateApp(deadTime);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    // 取得對應的文字
    private  String getLanguageText(String key){
        String result = languageJson.optString( key,key );
        return result;
    }

    // 主動關閉app
    private void terminateApp(int milliseconds){
        // 移除舊的runnable
        if(geoFenceRunable_Terminate!=null){
            geoFenceHandler_Terminate.removeCallbacks( geoFenceRunable_Terminate );
        }

        //新增新的runnable
        geoFenceRunable_Terminate= new Runnable(){
            @Override
            public void run() {
                settings.edit().remove("geoFence_LocationUpdate").commit();

                if(isAppIsInBackground( cordova.getContext() )){
                    Log.d(TAG,"背景呼叫api, 主動關閉app, 時間："+String.valueOf(milliseconds));
                    settings = null;    //geoFence使用settings來判斷啟動與否, 因此要記得設定為null, 表示程式被自我終結
                    webView.getPluginManager().postMessage( "exit",null );
                } else {
                    Log.d(TAG,"預計跑關閉流程，但是被移到前景，取消自動關閉");
                }
            }
        };
        geoFenceHandler_Terminate.postDelayed( geoFenceRunable_Terminate , milliseconds);
    }

    private void setLocalNotificationEventListener(CallbackContext callbackContext)
    {
        // 按下通知的時候, app 是 killed
        boolean checkedNotification = settings.getBoolean("geoFence_ReceiveNotification", false);
        Log.d(TAG,"檢查上一輪的通知，checkedNotification:"+String.valueOf( checkedNotification ));

        if (checkedNotification) {
            Log.d(TAG,"按下通知的時候, app killed");
            settings.edit().remove( "geoFence_ReceiveNotification" ).commit();
            String merchantID = settings.getString( "geoFence_ReceiveNotification_merchantID","" );
            sendUpdate_changePage(merchantID);
        }
    }

    // 發送更新給前台UI, 提供給 Notification 專用
    public static void sendUpdate_changePage(String Id){
        // 回傳網頁變更通知
        JSONObject obj = new JSONObject();
        try {
            obj.put("type", "changePage");
            obj.put("merchantID",Id);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, obj);
        pluginResult.setKeepCallback(true);
        myCallbackContext.sendPluginResult(pluginResult);
    }

    // 取得地理柵欄目前開啟狀態
    private void getGeofenceStatus(CallbackContext callbackContext)
    {
        boolean boolOfgetGeofenceStatus = settings.getBoolean("geoFence_GeofenceStatus", false);

        cordova.getThreadPool().execute( new Runnable() {
            @Override
            public void run() {
                PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, boolOfgetGeofenceStatus);
                pluginResult.setKeepCallback(false);
                callbackContext.sendPluginResult(pluginResult);
            }
        } );
    }

    // 啟用地理柵欄
    private void enableGeofence(CallbackContext callbackContext)
    {
        settings.edit().putBoolean("geoFence_GeofenceStatus", true).apply();
        startJobScheduler();
    }

    // 關閉地理柵欄
    private void disableGeofence(CallbackContext callbackContext)
    {
        settings.edit()
                .putBoolean("geoFence_GeofenceStatus", false)
                .putBoolean( "geoFence_isReceiveStoreInfo", false )
                .remove( "geoFence_ReceiveDate" )
                .remove( "geoFence_LocationUpdate" )
                .remove( "geoFence_ReceiveNotification" )
                .remove( "geoFence_ReceiveNotification_merchantID" )
                .apply();
        stopJobScheduler();
    }

    // 發送通知
    private void sentLocalNotification(CallbackContext callbackContext, String merchantID)
    {
        // 是否允許geoFence
        if(!settings.getBoolean( "geoFence_GeofenceStatus", false )) {
            return;
        }

        // 是否有接受過通知
        checkReceiveStatus();
        if(settings.getBoolean("geoFence_isReceiveStoreInfo", true)) {
            return;
        }

        NotificationManager nm =(NotificationManager) cordova.getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
        Intent it = new Intent(cordova.getActivity().getApplicationContext(), NotificationBroadcastReceiver.class);
        it.putExtra( "merchantID", merchantID );
        it.setPackage( null );
        PendingIntent pi = PendingIntent.getBroadcast(cordova.getActivity(),EveryTenMinuteCallThisService.jobId ,it, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder notification = new Notification.Builder(cordova.getActivity())
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setOngoing(false)
                .setSmallIcon(R.drawable.ic_scmobile)
                .setContentTitle(getLanguageText("confirmTitle"))
                .setContentText(getLanguageText("confirmContent"))
                .setGroup(notificationGroup);

        // android 8.0
        if (Build.VERSION.SDK_INT >=26) {
            String geoFenceChannelId = getLanguageText("confirmTitle");

            NotificationChannel nmChannel = new NotificationChannel( geoFenceChannelId, getLanguageText("confirmTitle"), NotificationManager.IMPORTANCE_DEFAULT );
            nm.createNotificationChannel( nmChannel );

            notification.setChannelId( geoFenceChannelId );
        }

        nm.notify(EveryTenMinuteCallThisService.jobId,notification.build());

        // 檢查上一輪是否接收到地理位置變更卻app killed
        // 送完通知之後, 終結app
        String checkedLocationUpdate = settings.getString("geoFence_LocationUpdate", "");
        if(!checkedLocationUpdate.equals("")) {
            terminateApp(0);
        }
    }

    // 發送確認視窗
    private void sentConfirmDialog(CallbackContext callbackContext)
    {
        // 是否允許geoFence
        if(!settings.getBoolean( "geoFence_GeofenceStatus", false )) {
            return;
        }

        // 是否有接受過通知
        checkReceiveStatus();
        if(settings.getBoolean("geoFence_isReceiveStoreInfo", true)) {
            return;
        }

        new android.app.AlertDialog.Builder(cordova.getActivity())
                .setTitle(getLanguageText("confirmTitle"))
                .setMessage(getLanguageText("confirmContent"))
                .setNegativeButton(getLanguageText("confirmCancel"), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setPositiveButton(getLanguageText("confirmOK"), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        cordova.getThreadPool().execute( new Runnable() {
                            @Override
                            public void run() {
                                sendUpdate_changePage( "" );
                            }
                        } );
                    }
                })
                .show();

        setReceiveStatus();
    }

    // 檢查 是否已經接受過地理柵欄通知
    private void checkReceiveStatus(){
        DateFormat df = new SimpleDateFormat("yyyy/MM/dd");
        String currentDate = df.format(Calendar.getInstance().getTime());
        String receiveDate = settings.getString("geoFence_ReceiveDate", "");
        boolean isReceive = receiveDate.equals(currentDate);

        settings.edit().putBoolean("geoFence_isReceiveStoreInfo", isReceive).apply();
    }

    // 設定 地理柵欄通知
    private static void setReceiveStatus() {

        DateFormat df = new SimpleDateFormat("yyyy/MM/dd");
        String receiveDate = df.format(Calendar.getInstance().getTime());
        settings.edit()
                .putString("geoFence_ReceiveDate", receiveDate)
                .putBoolean("geoFence_isReceiveStoreInfo", true)
                .apply();
    }

    // 檢查權限
    public boolean hasPermisssion() {
        for(String p : permissions)
        {
            if(ActivityCompat.checkSelfPermission(cordova.getActivity(), p)<0)
            {
                return false;
            }
        }
        return true;
    }

    // 發送更新給前台UI, 提供給 EveryTenMinuteCallThisService.java 專用
    public static void sendUpdate(JSONObject obj){
        if (obj != null) {
            PluginResult result = new PluginResult(PluginResult.Status.OK, obj);
            result.setKeepCallback(true);
            myCallbackContext.sendPluginResult(result);
        } else {
            PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
            result.setKeepCallback(true);
            myCallbackContext.sendPluginResult(result);
        }
    }

    // 開啟服務
    private void startJobScheduler(){
        // 設定 scheduler
        ComponentName mServiceComponent;
        mServiceComponent = new ComponentName( cordova.getActivity().getPackageName(), EveryTenMinuteCallThisService.class.getName() );
        JobInfo.Builder builder = new JobInfo.Builder(EveryTenMinuteCallThisService.jobId, mServiceComponent);
        if(Build.VERSION.SDK_INT >=24) {
            builder.setMinimumLatency( startJobTime );
        } else {
            builder.setPeriodic(EveryTenMinuteCallThisService.minimumLatency );
        }
        JobInfo jobInfo = builder.build();

        // Schedule job
        Log.d("START", "Scheduling job");
        JobScheduler tm = (JobScheduler) cordova.getActivity().getSystemService(Context.JOB_SCHEDULER_SERVICE);

        // Check whether this job is currently scheduled.
        boolean hasBeenScheduled = false ;

        List<JobInfo> jobs = tm.getAllPendingJobs();
        if (jobs == null) {
            hasBeenScheduled = false;
        } else {
            for (int i = 0; i < jobs.size(); i++) {
                if (jobs.get( i ).getId() == EveryTenMinuteCallThisService.jobId) {
                    hasBeenScheduled = true;
                }
            }
        }

        // start scheduler
        if(!hasBeenScheduled) {
            if (tm.schedule( jobInfo ) <= 0) {
                Log.d( TAG, "Scheduler error" );
            } else {
                Log.d( TAG, "Scheduler Start" );
            }
        }
    }
    // 關閉服務
    private void stopJobScheduler(){
        JobScheduler tm = (JobScheduler) cordova.getActivity().getSystemService(Context.JOB_SCHEDULER_SERVICE);
        tm.cancel( EveryTenMinuteCallThisService.jobId );
    }
}