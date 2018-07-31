//
//  geofence.h
//  SCB Mobile Bank
//
//  Created by Ayden Chen on 2018/6/19.
//

#import <Cordova/CDV.h>
#import <CoreLocation/CoreLocation.h>

@interface geofence : CDVPlugin<CLLocationManagerDelegate>
//  取得地理柵欄啟用狀態
- (void)initGeofence:(CDVInvokedUrlCommand*)command;
//  取得地理柵欄啟用狀態
- (void)setLocationUpdateEventListener:(CDVInvokedUrlCommand*)command;
//  取得地理柵欄啟用狀態
- (void)setLocalNotificationEventListener:(CDVInvokedUrlCommand*)command;
//  取得地理柵欄啟用狀態
- (void)getGeofenceStatus:(CDVInvokedUrlCommand*)command;
//  啟用地理柵欄
- (void)enableGeofence:(CDVInvokedUrlCommand*)command;
//  停用地理柵欄
- (void)disableGeofence:(CDVInvokedUrlCommand*)command;
//  發送推播
- (void)sentLocalNotification:(CDVInvokedUrlCommand*)command;
//  跳出使用者確認視窗
- (void)sentConfirmDialog:(CDVInvokedUrlCommand*)command;
//  使用者接收推播
- (void)sendUpdate_changePage:(NSString*)Id;
@end
