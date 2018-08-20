//
//  AppDelegate+Grofence.h
//  SCB Mobile Bank
//
//  Created by Ayden Chen on 2018/6/15.
//

#import "AppDelegate.h"
#import <objc/runtime.h>
#import <UserNotifications/UserNotifications.h>

@interface AppDelegate (GroFence) <UNUserNotificationCenterDelegate>

@end
