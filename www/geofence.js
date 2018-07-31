var exec = require('cordova/exec');

var geofence = function () {
    this.channels = {
        'locationUpdate': cordova.addWindowEventHandler('locationUpdate'),
        'changePage': cordova.addWindowEventHandler('changePage')
    };
};
//  初始化地理柵欄
geofence.prototype.initGeofence = function (initCallback, latitude, longitude) {
    exec(fireEvent, null, 'geofence', 'initGeofence', [latitude, longitude]);
};
//  位置更新callback
geofence.prototype.setLocationUpdateEventListener = function (eventListener) {
    var eventname = 'locationUpdate';
    // 註冊事件
    try {
        this.channels[eventname].subscribe(eventListener);
    } catch (err) {
        if(failCallback) {
            failCallback(err)
        } else {
            iab._error(err)
        }
    }
    exec(null, null, 'geofence', 'setLocationUpdateEventListener', []);
};
//  本地推播callback
geofence.prototype.setLocalNotificationEventListener = function (eventListener) {
    var eventname = 'changePage';
    // 註冊事件
    try {
        this.channels[eventname].subscribe(eventListener);
    } catch (err) {
        if(failCallback) {
            failCallback(err)
        } else {
            iab._error(err)
        }
    }
    exec(eventListener, null, 'geofence', 'setLocalNotificationEventListener', []);
};
//  取得地理柵欄啟用狀態
geofence.prototype.getGeofenceStatus = function (geofenceStatuscallback) {
    // 一次性的查詢, 不用加入eventPool
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
geofence.prototype.sentLocalNotification = function (merchantId) {
    exec(null, null, 'geofence', 'sentLocalNotification', [merchantId]);
};
//  跳出確認視窗
geofence.prototype.sentConfirmDialog = function (confirmDialogCallback) {
    exec(null, null, 'geofence', 'sentConfirmDialog', []);
};
// 告知是否為前台程式
// false:background 
// true:foreground
geofence.prototype.isForeGround = function (successCallback) {
    // 一次性的查詢, 不用加入eventPool
    exec(successCallback, null, 'geofence', 'isForeGround', []);
};
// 設定語言
geofence.prototype.language = function (languageJson) {
    exec(null, null, 'geofence', 'language', [languageJson]);
};

// 取得plugin狀態變更, 通知前台的js
function fireEvent(info) {
    if (info) {
        if(iab.channels[info.type]!=null) {
            // 啟動事件
            iab.channels[info.type].fire(info);
        }
    }
}

// Export the module
var iab = new geofence()
module.exports = iab