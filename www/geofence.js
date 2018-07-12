var exec = require('cordova/exec');

var geofence = function () {

};
//  初始化地理柵欄
geofence.prototype.initGeofence = function () {
    exec(null, null, 'geofence', 'initGeofence', []);
};
//  位置更新callback
geofence.prototype.setLocationUpdateEventListener = function (eventListener) {
    exec(eventListener, null, 'geofence', 'setLocationUpdateEventListener', []);
};
//  本地推播callback
geofence.prototype.setLocalNotificationEventListener = function (eventListener) {
    exec(eventListener, null, 'geofence', 'setLocalNotificationEventListener', []);
};
//  取得地理柵欄啟用狀態
geofence.prototype.getGeofenceStatus = function (geofenceStatuscallback) {
    exec(geofenceStatuscallback, null, 'geofence', 'getGeofenceStatus', []);
};
//  啟用地理柵欄
geofence.prototype.enableGeofence = function () {
    exec(null, null, 'geofence', 'enableGeofence', []);
};
//  停用地理柵欄
geofence.prototype.disableGeofence = function () {
    exec(null, null, 'geofence', 'disableGeofence', []);
};
//  發送本地推播
geofence.prototype.sentLocalNotification = function () {
    exec(null, null, 'geofence', 'sentLocalNotification', []);
};
//  跳出確認視窗
geofence.prototype.sentConfirmDialog = function (confirmDialogCallback) {
    exec(confirmDialogCallback, null, 'geofence', 'sentConfirmDialog', []);
};

// Export the module
module.exports = new geofence();
