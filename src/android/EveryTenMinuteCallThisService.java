package geofence;

import android.Manifest;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.mitake.android.scb.MainActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Service to handle callbacks from the JobScheduler. Requests scheduled with the JobScheduler
 * ultimately land on this service's "onStartJob" method. It runs jobs for a specific amount of time
 * and finishes them. It keeps the activity updated with changes via a Messenger.
 */
public class EveryTenMinuteCallThisService extends JobService {
    public static int jobId = 5566;  // 工作ID
    public static int minimumLatency = 600*1000;           // 下一次工作, 最小等待時間, 不可低於 5 秒
    int locationFastestInterval = 1*1000;   // 幾秒更新一次GPS
    int locationInterval = 6*1000;          // 最長幾秒更新一次GPS(可能有其他問題)
    int locationServiceKillTime = 180*1000;  // GPS多久沒獲得資訊就自動終結
    private Handler geoFenceHandler_Terminate =new Handler();
    private Runnable geoFenceRunable_Terminate;

    static final String TAG = "geoFenceEveryTenMinute";
    SharedPreferences settings;
    LocationRequest mLocationRequest;
    FusedLocationProviderClient mFusedLocationClient;


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");

        if (settings == null)
            settings = this.getApplicationContext().getSharedPreferences("pushEvent", 0);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service destroyed");
    }

    @Override
    public boolean onStartJob(final JobParameters params) {
        // The work that this service "does" is simply wait for a certain duration and finish
        // the job (on another thread).

        // 如果已經重啟程式了, 不要再加重負擔
        if (settings.getString( "geoFence_LocationUpdate","" ).equals("")) {
            // Uses a handler to delay the execution of jobFinished().
            Handler handler = new Handler();
            handler.post( new Runnable() {
                @Override
                public void run() {
                    Log.d( TAG, "on start job: " + params.getJobId() );
                    mainThread();
                }
            } );
        }
        Log.d(TAG,"job finished");

        // 結束工作前, 提前安排 重新排程
        if (Build.VERSION.SDK_INT >= 24) {
            schedulerRefresh();
        }

        // 結束工作
        jobFinished(params, false);

        // true, 還有工作需要繼續執行
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        // Stop tracking these job parameters, as we've 'finished' executing.
        Log.d(TAG, "on stop job: " + params.getJobId());

        // Return false to drop the job.
        return false;
    }

    // 主要執行功能
    private void mainThread(){
        boolean willStartService = true;    // 是否啟用服務

        // 檢查 geoFence是否開啟
        willStartService = settings.getBoolean( "geoFence_GeofenceStatus", false);
        Log.d("geoFence是否開啟",String.valueOf(willStartService));

        // 檢查 geoFence今天是否已經收過通知
        // if true, it will not run location-get. remember add positive.
        if(willStartService) {
            willStartService = !settings.getBoolean( "geoFence_isReceiveStoreInfo",false );
            Log.d(TAG,"geoFence今天是否已經收過通知 "+String.valueOf(!willStartService));
        }

        // 檢查 geoFence android權限
        if (willStartService) {
            String[] permissions = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};

            for (String p : permissions) {
                if (ActivityCompat.checkSelfPermission( this, p ) < 0) {
                    willStartService = false;
                }
            }
            Log.d(TAG,"geoFence android權限 "+String.valueOf(willStartService));
        }

        //  抓取位置
        if (willStartService) {
            // 設定位置抓取時間
            mLocationRequest = LocationRequest.create();
            mLocationRequest.setInterval( locationInterval )
                    .setPriority( LocationRequest.PRIORITY_HIGH_ACCURACY )
                    .setFastestInterval( locationFastestInterval );

            // 超過時間未獲取GPS, 關閉服務
            terminateService( locationServiceKillTime );

            // 啟動服務
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            mFusedLocationClient.requestLocationUpdates( mLocationRequest,mLocationCallback, Looper.myLooper() );
        }
        Log.d( TAG, "onStartCommand: ");
    }

    // 重新排程
    private void schedulerRefresh(){
        JobScheduler mJobScheduler = (JobScheduler)getApplicationContext()
                .getSystemService(JOB_SCHEDULER_SERVICE);
        JobInfo.Builder builder =
                new JobInfo.Builder(jobId,
                        new ComponentName(getPackageName(),
                                EveryTenMinuteCallThisService.class.getName()));

        /* For Android N and Upper Versions */
        if (android.os.Build.VERSION.SDK_INT >= 24) {
            builder
                    .setMinimumLatency( minimumLatency ) //YOUR_TIME_INTERVAL
                    .setRequiredNetworkType( JobInfo.NETWORK_TYPE_ANY );
        }
        JobInfo jobInfo = builder.build();
        mJobScheduler.schedule(jobInfo);
    }

    // 設定最佳位置
    private void getAddress(Location location){
        String latitude = String.valueOf(location.getLatitude());
        String longitude = String.valueOf(location.getLongitude());
        Log.d(TAG,"ready sendUpdate");

        // 回傳網頁變更通知
        JSONObject obj = new JSONObject();
        try {
            obj.put("type", "locationUpdate");
            obj.put("latitude", latitude);
            obj.put("longitude", longitude);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (geofence.settings != null) {
            // 嘗試回應, 如果有錯誤就是死亡
            Log.d( TAG, "嘗試呼叫sendUpdate" );
            geofence.sendUpdate( obj );
        } else {
            Log.d( TAG, "geoFence被掛掉了. 新增主程式, 並且在後台" );
            settings.edit().putString( "geoFence_LocationUpdate", obj.toString() ).commit();

            // 新增主程式, 並且在後台
            Intent newTask = new Intent( this, MainActivity.class );
            newTask.putExtra( "cdvStartInBackground", true );
            newTask.addFlags( Intent.FLAG_ACTIVITY_CLEAR_TASK );
            newTask.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
            startActivity( newTask );
        }
    }

    // 設定最佳位置 callBack
    LocationCallback mLocationCallback = new LocationCallback(){
        @Override
        public void onLocationResult(LocationResult locationResult) {
            List<Location> locationList = locationResult.getLocations();
            if (locationList.size() > 0) {
                //The last location in the list is the newest
                Location location = locationList.get(locationList.size() - 1);
                Log.i(TAG, "Location: " + location.getLatitude() + " " + location.getLongitude());
                getAddress( location );
                terminateService( 0 );
            }
        }
    };

    // 主動關閉服務
    private void terminateService(int milliseconds){
        // 移除舊的runnable
        if(geoFenceRunable_Terminate!=null){
            geoFenceHandler_Terminate.removeCallbacks( geoFenceRunable_Terminate );
        }

        //新增新的runnable
        geoFenceRunable_Terminate= new Runnable(){
            @Override
            public void run() {
                mFusedLocationClient.removeLocationUpdates( mLocationCallback );
            }
        };
        geoFenceHandler_Terminate.postDelayed( geoFenceRunable_Terminate , milliseconds);
    }
}