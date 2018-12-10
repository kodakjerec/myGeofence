package geofence;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.mitake.android.scb.MainActivity;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class NotificationBroadcastReceiver extends BroadcastReceiver {
    String TAG = "geoFenceNotification";

    @Override
    public void onReceive(Context context, Intent intent)
    {
        String merchantID = intent.getStringExtra( "merchantID" );

        SharedPreferences settings = context.getApplicationContext().getSharedPreferences("pushEvent", 0);
        // 1. change isReceiveStoreInfo
        // 2. set receiveDate
        DateFormat df = new SimpleDateFormat("yyyy/MM/dd");
        String receiveDate = df.format( Calendar.getInstance().getTime());
        settings.edit()
                .putString("geoFence_ReceiveDate", receiveDate)
                .putBoolean("geoFence_isReceiveStoreInfo", true)
                .apply();
        if(geofence.settings!=null) {
            Log.d(TAG,"按下通知的時候, 喚醒app. merchantID:"+merchantID);
            // 喚醒
            Intent newTask = new Intent(context, MainActivity.class);
            newTask.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity( newTask );
            geofence.sendUpdate_changePage(merchantID);
        } else {
            // 新增
            Log.d(TAG,"geoFence被掛掉了. 按下通知的時候, app killed. merchantID:"+merchantID);
            // 本地通知下, 採用新增方式. 表示app已經被kill.
            // 為了讓下一輪開啟時能夠跳到地理柵欄畫面, 設定flag.
            settings.edit()
                    .putBoolean("geoFence_ReceiveNotification", true)
                    .putString( "geoFence_ReceiveNotification_merchantID", merchantID )
                    .apply();

            Intent newTask = new Intent(context, MainActivity.class);
            newTask.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            newTask.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(newTask);
        }
    }
}
