//
//  AppDelegate+Geofence.m
//  SCB Mobile Bank
//
//  Created by Ayden Chen on 2018/6/15.
//

#import "AppDelegate+Grofence.h"
#import "geofence.h"

@implementation AppDelegate (GroFence)

+ (void)load
{
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        Class class = [self class];
        
        SEL originalInitSelector = @selector(init);
        SEL swizzledInitSelector = @selector(geofenceSwizzledInit);
        
        Method originalInit = class_getInstanceMethod(class, originalInitSelector);
        Method swizzledInit = class_getInstanceMethod(class, swizzledInitSelector);
        
        BOOL didAddInitMethod =
        class_addMethod(class,
                        originalInitSelector,
                        method_getImplementation(swizzledInit),
                        method_getTypeEncoding(swizzledInit));
        
        if (didAddInitMethod) {
            class_replaceMethod(class,
                                swizzledInitSelector,
                                method_getImplementation(originalInit),
                                method_getTypeEncoding(originalInit));
        } else {
            method_exchangeImplementations(originalInit, swizzledInit);
        }
        
    });
}

- (AppDelegate *)geofenceSwizzledInit
{
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(geoFence_whenAppIsKilled:)
                                                 name:UIApplicationDidFinishLaunchingNotification
                                               object:nil];
    
    return [self geofenceSwizzledInit];
}

- (void)geoFence_whenAppIsKilled:(NSNotification*)notification{

    NSLog(@"geoFence didFinishLaunchingWithOptions");
    if(notification){
        NSDictionary* launchOptions = [notification userInfo];
        NSMutableArray* localNotification = [launchOptions objectForKey: @"UIApplicationLaunchOptionsLocalNotificationKey"];
        NSDictionary* userInfo = [localNotification valueForKey:@"userInfo"];
        // 按下通知的時候, app is killed
        [self geoFence_gotUserInfo:userInfo isAppKilled:1];
    }
}

#pragma mark - UNUserNotificationCenter Delegate // >= iOS 8
- (void)application:(UIApplication *)application didReceiveLocalNotification:(UILocalNotification *)notification{
    if(notification){
        NSDictionary *userInfo = notification.userInfo;
        [self geoFence_gotUserInfo:userInfo isAppKilled:0];
    }
}

#pragma mark - UNUserNotificationCenter Delegate // >= iOS 10
- (void)userNotificationCenter:(UNUserNotificationCenter *)center didReceiveNotificationResponse:(UNNotificationResponse *)response withCompletionHandler:(void (^)(void))completionHandler{
    NSDictionary *userInfo = response.notification.request.content.userInfo;
    NSString *value = [userInfo objectForKey:@"name"];
    if ([value isEqualToString:@"GeofenceNotification"]) {
        [self geoFence_gotUserInfo:userInfo isAppKilled:0];
    }

    completionHandler();
}

-(void)geoFence_gotUserInfo:(NSDictionary*)userInfo isAppKilled:(NSInteger)isAppKilled{
    NSLog(@"geoFence receive userInfo");
    NSString *value = [userInfo objectForKey:@"name"];
    NSLog(@"geoFence receive name %@", value);
    
    if ([value isEqualToString:@"GeofenceNotification"])
    {
        NSString *merchantID = [userInfo objectForKey:@"merchantID"];
        NSLog(@"geoFence geoFence_gotUserInfo merchantID: %@", merchantID);
        // 按下通知寫入設定值
        NSDateComponents *components = [[NSCalendar currentCalendar] components:NSCalendarUnitDay | NSCalendarUnitMonth | NSCalendarUnitYear fromDate:[NSDate date]];
        NSDate *receiveDate = [[NSCalendar currentCalendar] dateFromComponents:components];

        NSUserDefaults* defaults = [NSUserDefaults standardUserDefaults];
        [defaults setObject:receiveDate forKey:@"geoFence_ReceiveDate"];
        [defaults setBool:YES forKey:@"geoFence_isReceiveStoreInfo"];
        [defaults synchronize];
        
        if(isAppKilled==1) {
            // 按下通知的時候, app is killed
            [defaults setBool:YES forKey:@"geoFence_ReceiveNotification"];
            [defaults setValue:merchantID forKey:@"geoFence_ReceiveNotification_merchantID"];
            [defaults synchronize];
        } else {
            geofence* geofenceInstance = [self.viewController getCommandInstance:@"geofence"];
            [geofenceInstance sendUpdate_changePage:merchantID];
        }
    }
}

@end
