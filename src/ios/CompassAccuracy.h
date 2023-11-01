#import <Cordova/CDVPlugin.h>
#import <CoreLocation/CoreLocation.h>


@interface CompassAccuracy : CDVPlugin <CLLocationManagerDelegate>{}

@property (nonatomic, strong) CLLocationManager* locationManager;
@property (nonatomic, retain) CLHeading* currentHeading;

- (void) startMonitoring:(CDVInvokedUrlCommand*)command;
- (void) stopMonitoring:(CDVInvokedUrlCommand*)command;
- (void) getCurrentAccuracy:(CDVInvokedUrlCommand*)command;
- (void) simulateAccuracyChange:(CDVInvokedUrlCommand*)command;

@end
