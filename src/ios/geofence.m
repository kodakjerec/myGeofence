/********* geofence.m Cordova Plugin Implementation *******/
#import "geofence.h"

@implementation geofence {
    // Member variables go here.
    CLLocation *lastLocation;
    CLLocationManager *locationManager;
    NSString *locationUpdateCallbackID;
    NSString *localNotificationCallbackID;
}

- (void)initGeofence:(CDVInvokedUrlCommand *)command{
    
    if (locationManager == nil) {
        locationManager = [Global sharedInstance].locationManager;
        locationManager.distanceFilter = 1000;
        locationManager.allowsBackgroundLocationUpdates = YES;
    }
    locationManager.delegate = self;
    
    Global *global = [Global sharedInstance];
    BOOL isGenfenceEnable = [[NSUserDefaults standardUserDefaults] boolForKey:@"GeofenceStatus"];
    global.isGenfenceEnable = isGenfenceEnable;
    
    [self checkReceiveStatus];
}

- (void)setLocationUpdateEventListener:(CDVInvokedUrlCommand *)command{
    locationUpdateCallbackID = command.callbackId;
}

- (void)setLocalNotificationEventListener:(CDVInvokedUrlCommand *)command{
    localNotificationCallbackID = command.callbackId;
}

- (void)getGeofenceStatus:(CDVInvokedUrlCommand *)command {
    BOOL status = [Global sharedInstance].isGenfenceEnable;
    
    CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsBool:status];
    [result setKeepCallbackAsBool:NO];
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}

- (void)enableGeofence:(CDVInvokedUrlCommand *)command {
    
    NSLog(@"startSLCService");
    [Global sharedInstance].isGenfenceEnable = YES;
    [[NSUserDefaults standardUserDefaults] setBool:YES forKey:@"GeofenceStatus"];
    
    if ([CLLocationManager authorizationStatus] == kCLAuthorizationStatusAuthorizedAlways) {
        [locationManager startMonitoringSignificantLocationChanges];
    } else {
        [locationManager requestAlwaysAuthorization];
    }
}

- (void)disableGeofence:(CDVInvokedUrlCommand *)command {
    
    NSLog(@"stopSLCService");
    [Global sharedInstance].isGenfenceEnable = NO;
    [[NSUserDefaults standardUserDefaults] setBool:NO forKey:@"GeofenceStatus"];
    [locationManager stopMonitoringSignificantLocationChanges];
    
}

- (void)locationManager:(CLLocationManager *)manager didChangeAuthorizationStatus:(CLAuthorizationStatus)status{
    
    if ([CLLocationManager authorizationStatus] == kCLAuthorizationStatusAuthorizedAlways) {
        lastLocation = [locationManager location];
        [locationManager startMonitoringSignificantLocationChanges];
    }
}

- (void)sentLocalNotification:(CDVInvokedUrlCommand *)command {
    
    [self checkReceiveStatus];
    if ([Global sharedInstance].isReceiveStoreInfo) {
        return;
    }
    
    UILocalNotification *localNotification = [[UILocalNotification alloc] init];
    localNotification.userInfo = [[NSDictionary alloc] initWithObjectsAndKeys:@"GeofenceNotification",@"name", nil];
    localNotification.alertTitle = @"SC Mobile";
    localNotification.alertBody = @"您有1則來自渣打銀行活動訊息通知";
    [[UIApplication sharedApplication] scheduleLocalNotification:localNotification];
}

- (void)userDidReceiveNotification:(UILocalNotification *)notification{
    
    if (localNotificationCallbackID == nil) {
        return;
    }
    
    [self setReceiveStatus];
    
    CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [result setKeepCallbackAsBool:NO];
    [self.commandDelegate sendPluginResult:result callbackId:localNotificationCallbackID];
}

- (void)sentConfirmDialog:(CDVInvokedUrlCommand *)command {
    
    [self checkReceiveStatus];
    if ([Global sharedInstance].isReceiveStoreInfo) {
        return;
    }
    
    UIAlertController *alertController = [UIAlertController alertControllerWithTitle:@"SC Mobile" message:@"您有1則來自渣打銀行活動訊息通知" preferredStyle:UIAlertControllerStyleAlert];
    [alertController addAction:[UIAlertAction actionWithTitle:@"檢視" style:UIAlertActionStyleDefault handler:^(UIAlertAction *action) {
        
        CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        [result setKeepCallbackAsBool:NO];
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
        
    }]];
    
    [alertController addAction:[UIAlertAction actionWithTitle:@"取消" style:UIAlertActionStyleDefault handler:^(UIAlertAction *action) {
        [self.viewController dismissViewControllerAnimated:YES completion:nil];
    }]];
    
    [self.viewController presentViewController:alertController animated:YES completion:^(){
        [self setReceiveStatus];
    }];
}

- (void)locationUpdate:(CLLocation*) location{
    //檢查是否有callback method
    if (locationUpdateCallbackID == nil) {
        return;
    }
    //檢查今日是否有顯示popup
    [self checkReceiveStatus];
    if ([Global sharedInstance].isReceiveStoreInfo) {
        return;
    }
    
    NSMutableDictionary *locationInfo = [[NSMutableDictionary alloc] init];
    [locationInfo setObject: [NSNumber numberWithDouble: location.coordinate.latitude] forKey:@"latitude"];
    [locationInfo setObject: [NSNumber numberWithDouble: location.coordinate.longitude] forKey:@"longitude"];
    
    CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:locationInfo];
    [result setKeepCallbackAsBool:YES];
    [self.commandDelegate sendPluginResult:result callbackId:locationUpdateCallbackID];
    
}

- (void)locationManager:(CLLocationManager *)manager didUpdateLocations:(NSArray<CLLocation *> *)locations{
    
    CLLocation *newLocation = locations.lastObject;
    CLLocationDistance distance = [lastLocation distanceFromLocation:newLocation];
    
    //  避免啟用位置偵測時馬上抓店家
    if (distance > 2000) {
        [self locationUpdate:newLocation];
    }
    
}

- (void)setReceiveStatus{
    [Global sharedInstance].isReceiveStoreInfo = YES;
    NSDateComponents *components = [[NSCalendar currentCalendar] components:NSCalendarUnitDay | NSCalendarUnitMonth | NSCalendarUnitYear fromDate:[NSDate date]];
    NSDate *receiveDate = [[NSCalendar currentCalendar] dateFromComponents:components];
    [[NSUserDefaults standardUserDefaults] setObject:receiveDate forKey:@"ReceiveDate"];
}

- (void)checkReceiveStatus{
    
    // 用日期判斷今天是否已經顯示過
    NSDateComponents *components = [[NSCalendar currentCalendar] components:NSCalendarUnitDay | NSCalendarUnitMonth | NSCalendarUnitYear fromDate:[NSDate date]];
    NSDate *currentDate = [[NSCalendar currentCalendar] dateFromComponents:components];
    NSDate *receiveDate = [[NSUserDefaults standardUserDefaults] objectForKey:@"ReceiveDate"];
    BOOL isReceive = [receiveDate isEqualToDate:currentDate];
    
    [Global sharedInstance].isReceiveStoreInfo = isReceive;
}

@end
