package geofence;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.IBinder;
import android.os.*;
import android.util.Log;
import android.content.*;
import java.util.List;
import com.scb.mb.tw.MainActivity;
import org.json.JSONException;
import org.json.JSONObject;

public class EveryTenMinuteCallThisService extends Service
{
    String provider;
    public class LocalBinder extends Binder
    {
        EveryTenMinuteCallThisService getService()
        {
            return  EveryTenMinuteCallThisService.this;
        }
    }
    private LocalBinder mLocBin = new LocalBinder();

    @Override
    public IBinder onBind(Intent arg0)
    {
        return mLocBin;
    }

    @Override
    public void onCreate()
    {

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        SharedPreferences sharedPreferences = getSharedPreferences("data" , MODE_PRIVATE);
        int i = sharedPreferences.getInt("score" , 0);
        sharedPreferences.edit().putInt("score" , 100).apply();

        //////
        int permission = android.support.v4.content.ContextCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_FINE_LOCATION);

        if(permission == PackageManager.PERMISSION_GRANTED)
        {
            LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

            List<String> providerList = locationManager.getProviders(true);
            List<String> providers = locationManager.getAllProviders();
            Location bestLocation = null;

            if(providerList.contains(LocationManager.GPS_PROVIDER))
            {
                provider = LocationManager.GPS_PROVIDER;
            }
            for (String provider : providers)
            {
                Location location = locationManager.getLastKnownLocation(provider);
                if (location == null)
                {
                    continue;
                }
                if (bestLocation == null || location.getAccuracy() < bestLocation.getAccuracy())
                {
                    bestLocation = location;
                }

            }
            if(bestLocation!=null)
            {
                String latitude = String.valueOf(bestLocation.getLatitude());
                String longitude = String.valueOf(bestLocation.getLongitude());

                 // 回傳網頁變更通知
                 JSONObject obj = new JSONObject();
                 try {
                     obj.put("type", "onStartCommand");
                     obj.put("latitude", latitude);
                     obj.put("longitude", longitude);
                 } catch (JSONException e) {
                     e.printStackTrace();
                 }
                 Log.d("OK","ready sendUpdate");
                 try {
                     geofence.sendUpdate(obj);
                 } catch(Exception e) {
                     Intent newTask = new Intent(this, MainActivity.class);
                     newTask.putExtra("cdvStartInBackground", true);
                     newTask.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                     newTask.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                     startActivity(newTask);
                 }
            }
        }

        AlarmManager alarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        Intent in  = new Intent(this,EveryTenMinuteCallThisService.class);
        PendingIntent pendingIntent = PendingIntent.getService(this,0,in,0);

        long startTime = SystemClock.elapsedRealtime()+600000;
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,startTime,pendingIntent);
        Log.d("Service", "onStartCommand: ");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public boolean onUnbind(Intent intent)
    {
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
    }
}
