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
        
        NSString *value = [userInfo objectForKey:@"name"];
        if ([value isEqualToString:@"GeofenceNotification"])
        {
            geofence* Geofence = [[geofence alloc] init];
            [Geofence geoFence_gotUserInfo:userInfo isAppKilled:1];
        }
    }
}

@end
