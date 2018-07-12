# 地理柵欄 geofence #
提供給特定公司的地理柵欄

## 用法
***
1. 啟用
<pre>geofence.initGeofence()</pre>
2. 設定位置變更通知(傳送callback)
+ 回傳格式：{"latitude":@latitude@, "longitude":@longitude@}
`@latitude@ 經度 @longitude@ 緯度`
<pre>geofence.setLocationUpdateEventListener(successCallback)
function successCallback(object){
    console.log(object) //{latitude:123, longitude:456}
}</pre>
3. 設定位置變更本機通知(傳送callback)
+ 回傳格式：{"latitude":@latitude@, "longitude":@longitude@}
`@latitude@ 經度 @longitude@ 緯度`
<pre>geofence.setLocalNotificationEventListener(successCallback)
function successCallback(object){
    console.log(object) //{latitude:123, longitude:456}
}
</pre>
4. 取得geofence狀態
+ 回傳格式: true/false 
`geofence開啟狀態`
<pre>geofence.getGeofenceStatus()</pre>
5. 開啟geofence
<pre>geofence.enableGeofence()</pre>
6. 關閉geofence
<pre>geofence.disableGeofence()</pre>
7. 傳送本地推播(app在背景)
<pre>geofence.sentLocalNotification()</pre>
8. 傳送確認視窗(app在前景)
<pre>geofence.sentConfirmDialog()</pre>