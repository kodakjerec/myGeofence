//
//  AppDelegate+Grofence.m
//  SCB Mobile Bank
//
//  Created by Ayden Chen on 2018/6/15.
//

#import "AppDelegate+Grofence.h"
#import "Global.h"
#import "geofence.h"

@implementation AppDelegate (Grofence)

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
//    NSLog(@"setLocalNotification");
//    Global *global = [Global sharedInstance];
//    BOOL isGenfenceEnable = [[NSUserDefaults standardUserDefaults] boolForKey:@"GeofenceStatus"];
//    global.isGenfenceEnable = isGenfenceEnable;
//
//    [geofence checkReceiveStatus];
    
    return [self geofenceSwizzledInit];
}

- (void)application:(UIApplication *)application didReceiveLocalNotification:(UILocalNotification *)notification{
    NSDictionary *userInfo = notification.userInfo;
    NSString *value = [userInfo objectForKey:@"name"];
    if ([value isEqualToString:@"GeofenceNotification"])
    {
        geofence *geofenceInstance = [self.viewController getCommandInstance:@"geofence"];
        [geofenceInstance userDidReceiveNotification:notification];
    }
}


@end
