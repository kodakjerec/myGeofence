//
//  Global.m
//  SCB Mobile Bank
//
//  Created by Ayden Chen on 2018/6/15.
//

#import "Global.h"

@implementation Global

@synthesize locationManager = _locationManager;

+ (Global *)sharedInstance {
    static dispatch_once_t onceToken;
    static Global *instance = nil;
    dispatch_once(&onceToken, ^{
        instance = [[Global alloc] init];
    });
    return instance;
}

- (id)init {
    self = [super init];
    if (self) {
        _locationManager = [[CLLocationManager alloc] init];
        _isGenfenceEnable = NO;
        _isReceiveStoreInfo = NO;
    }
    return self;
}
@end
