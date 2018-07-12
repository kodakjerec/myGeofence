//
//  Global.h
//  SCB Mobile Bank
//
//  Created by Ayden Chen on 2018/6/15.
//

#import <Foundation/Foundation.h>
#import <CoreLocation/CoreLocation.h>

@interface Global : NSObject {
    CLLocationManager *_locationManager;
    BOOL _isGenfenceEnable;
    BOOL _isReceiveStoreInfo;
}

+ (Global *)sharedInstance;

@property(strong, nonatomic) CLLocationManager *locationManager;
@property(nonatomic) BOOL isGenfenceEnable;
@property(nonatomic) BOOL isReceiveStoreInfo;
@end
