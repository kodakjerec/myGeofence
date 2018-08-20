/********* geofence.m Cordova Plugin Implementation *******/
#import "geofence.h"

@implementation geofence {
    // Member variables go here.
    CLLocationManager *locationManager;
    NSString *locationUpdateCallbackID;
    NSMutableArray* languageJson;  // 系統預設中英字串
}

- (void)initGeofence:(CDVInvokedUrlCommand *)command{
    
    if (locationManager == nil) {
        locationManager = [[CLLocationManager alloc] init];
        locationManager.distanceFilter = 1000;
        locationManager.allowsBackgroundLocationUpdates = YES;
    }
    locationUpdateCallbackID = command.callbackId;
    locationManager.delegate = self;
    
    // 啟用地理柵欄
    BOOL isGenfenceEnable = [[NSUserDefaults standardUserDefaults] boolForKey:@"geoFence_GeofenceStatus"];
    if(isGenfenceEnable == YES){
        if ([CLLocationManager authorizationStatus] == kCLAuthorizationStatusAuthorizedAlways) {
            [locationManager startMonitoringSignificantLocationChanges];
        } else {
            [locationManager requestAlwaysAuthorization];
        }
    } else {
        [locationManager stopMonitoringSignificantLocationChanges];
    }
}

- (void)setLocationUpdateEventListener:(CDVInvokedUrlCommand *)command{
}

- (void)setLocalNotificationEventListener:(CDVInvokedUrlCommand *)command{
    // 按下通知的時候, app is killed
    NSUserDefaults* defaults = [NSUserDefaults standardUserDefaults];
    BOOL isAppKilledPreviousRound = [defaults boolForKey:@"geoFence_ReceiveNotification"];
    NSLog(@"檢查上一輪的通知, checkNotification %d",isAppKilledPreviousRound);
    if(isAppKilledPreviousRound==YES){
        NSLog(@"按下通知的時候, app is killed");
        [defaults removeObjectForKey:@"geoFence_ReceiveNotification"];
        NSString* merchantID = [defaults valueForKey:@"geoFence_ReceiveNotification_merchantID"];
        [self sendUpdate_changePage:merchantID];
    }
}

// 設定語言
- (void)language:(CDVInvokedUrlCommand *)command{
    NSLog(@"geoFence language");
    languageJson = [command.arguments objectAtIndex:0];
}
// 取得對應的文字
- (NSString*)getLanguageText:(NSString*)key {
    if([languageJson valueForKey:key]){
        return [languageJson valueForKey:key];
    } else {
        return key;
    }
}

// 取得地理柵欄目前開啟狀態
- (void)getGeofenceStatus:(CDVInvokedUrlCommand *)command {
    BOOL isGenfenceEnable = [[NSUserDefaults standardUserDefaults] boolForKey:@"geoFence_GeofenceStatus"];
    CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsBool:isGenfenceEnable];
    [result setKeepCallbackAsBool:NO];
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}

// 啟用 地理柵欄
- (void)enableGeofence:(CDVInvokedUrlCommand *)command {
    NSLog(@"geoFence enableGeofence");
    [[NSUserDefaults standardUserDefaults] setBool:YES forKey:@"geoFence_GeofenceStatus"];
    
    if ([CLLocationManager authorizationStatus] == kCLAuthorizationStatusAuthorizedAlways) {
        [locationManager startMonitoringSignificantLocationChanges];
    } else {
        [locationManager requestAlwaysAuthorization];
    }
}

//停用 地理柵欄
- (void)disableGeofence:(CDVInvokedUrlCommand *)command {
    
    NSLog(@"geoFence disableGeofence");
    [[NSUserDefaults standardUserDefaults] setBool:NO forKey:@"geoFence_GeofenceStatus"];
    [locationManager stopMonitoringSignificantLocationChanges];
}

- (void)locationManager:(CLLocationManager *)manager didChangeAuthorizationStatus:(CLAuthorizationStatus)status{
    if ([CLLocationManager authorizationStatus] == kCLAuthorizationStatusAuthorizedAlways) {
        [locationManager startMonitoringSignificantLocationChanges];
    }
}

// 發送通知
- (void)sentLocalNotification:(CDVInvokedUrlCommand *)command {
    NSLog(@"geoFence sentLocalNotification");
    
    // 是否允許geoFence
    if ([[NSUserDefaults standardUserDefaults] boolForKey:@"geoFence_GeofenceStatus"]==NO){
        return;
    }
    
    // 是否有接受過通知
    [self checkReceiveStatus];
    if([[NSUserDefaults standardUserDefaults] boolForKey:@"geoFence_isReceiveStoreInfo"]==YES) {
        return;
    }

    NSString *merchantID = [command argumentAtIndex:0];
    UILocalNotification *localNotification = [[UILocalNotification alloc] init];
    localNotification.userInfo = [[NSDictionary alloc] initWithObjectsAndKeys:@"GeofenceNotification",@"name",merchantID,@"merchantID", nil];
    localNotification.alertTitle =[self getLanguageText:@"confirmTitle"];
    localNotification.alertBody = [self getLanguageText:@"confirmContent"];
    [[UIApplication sharedApplication] scheduleLocalNotification:localNotification];
    [self setReceiveStatus];
}

// 發送確認視窗
- (void)sentConfirmDialog:(CDVInvokedUrlCommand *)command {
    NSLog(@"geoFence sentConfirmDialog");
    
    // 是否允許geoFence
    if ([[NSUserDefaults standardUserDefaults] boolForKey:@"geoFence_GeofenceStatus"]==NO){
        return;
    }
    
    // 是否有接受過通知
    [self checkReceiveStatus];
    if([[NSUserDefaults standardUserDefaults] boolForKey:@"geoFence_isReceiveStoreInfo"]==YES) {
        return;
    }
    
    UIAlertController *alertController = [UIAlertController alertControllerWithTitle:[self getLanguageText:@"confirmTitle"] message:[self getLanguageText:@"confirmContent"] preferredStyle:UIAlertControllerStyleAlert];
    [alertController addAction:[UIAlertAction actionWithTitle:[self getLanguageText:@"confirmOK"] style:UIAlertActionStyleDefault handler:^(UIAlertAction *action) {
        [self sendUpdate_changePage:@""];
    }]];
    
    [alertController addAction:[UIAlertAction actionWithTitle:[self getLanguageText:@"confirmCancel"] style:UIAlertActionStyleDefault handler:^(UIAlertAction *action) {
        [self.viewController dismissViewControllerAnimated:YES completion:nil];
    }]];
    
    [self.viewController presentViewController:alertController animated:YES completion:^(){
        [self setReceiveStatus];
    }];
}

- (void)sendUpdate:(NSMutableDictionary*) locationInfo{
    CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:locationInfo];
    [result setKeepCallbackAsBool:YES];
    [self.commandDelegate sendPluginResult:result callbackId:locationUpdateCallbackID];
}
- (void)sendUpdate_changePage:(NSString*)Id{
    NSMutableDictionary *locationInfo = [[NSMutableDictionary alloc] init];
    [locationInfo setValue:@"changePage" forKey:@"type"];
    [locationInfo setValue:Id forKey:@"merchantID"];
    
    CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:locationInfo];
    [result setKeepCallbackAsBool:YES];
    [self.commandDelegate sendPluginResult:result callbackId:locationUpdateCallbackID];
}

- (void)locationManager:(CLLocationManager *)manager didUpdateLocations:(NSArray<CLLocation *> *)locations{
    CLLocation *newLocation = locations.lastObject;
    
    NSMutableDictionary *locationInfo = [[NSMutableDictionary alloc] init];
    [locationInfo setValue:@"locationUpdate" forKey:@"type"];
    [locationInfo setObject: [NSString stringWithFormat:@"%f", newLocation.coordinate.latitude] forKey:@"latitude"];
    [locationInfo setObject: [NSString stringWithFormat:@"%f", newLocation.coordinate.longitude] forKey:@"longitude"];
    
    [self sendUpdate:locationInfo];
}

// 設定 地理柵欄通知
- (void)setReceiveStatus{
    NSDateComponents *components = [[NSCalendar currentCalendar] components:NSCalendarUnitDay | NSCalendarUnitMonth | NSCalendarUnitYear fromDate:[NSDate date]];
    NSDate *receiveDate = [[NSCalendar currentCalendar] dateFromComponents:components];
    [[NSUserDefaults standardUserDefaults] setObject:receiveDate forKey:@"geoFence_ReceiveDate"];
    
    [[NSUserDefaults standardUserDefaults] setBool:YES forKey:@"geoFence_isReceiveStoreInfo"];
}

// 檢查 是否已經接受過地理柵欄通知
- (void)checkReceiveStatus{
    
    // 用日期判斷今天是否已經顯示過
    NSDateComponents *components = [[NSCalendar currentCalendar] components:NSCalendarUnitDay | NSCalendarUnitMonth | NSCalendarUnitYear fromDate:[NSDate date]];
    NSDate *currentDate = [[NSCalendar currentCalendar] dateFromComponents:components];
    NSDate *receiveDate = [[NSUserDefaults standardUserDefaults] objectForKey:@"geoFence_ReceiveDate"];
    BOOL isReceive = [receiveDate isEqualToDate:currentDate];

    [[NSUserDefaults standardUserDefaults] setBool:isReceive forKey:@"geoFence_isReceiveStoreInfo"];
}

@end
