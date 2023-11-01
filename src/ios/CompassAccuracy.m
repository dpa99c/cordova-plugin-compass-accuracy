#import "CompassAccuracy.h"

@implementation CompassAccuracy

// Initialization
@synthesize locationManager;
@synthesize currentHeading;



- (void)pluginInitialize
{
    self.locationManager = [[CLLocationManager alloc] init];
    self.locationManager.delegate = self;
}

// Plugin API
- (void) startMonitoring:(CDVInvokedUrlCommand*)command;{
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    @try {
      // TODO
    }@catch (NSException *exception) {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:exception.reason];
    }
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void) stopMonitoring:(CDVInvokedUrlCommand*)command;{
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    @try {
      // TODO
    }@catch (NSException *exception) {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:exception.reason];
    }
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}


#pragma mark - locationManager delegate

- (BOOL)locationManagerShouldDisplayHeadingCalibration:(CLLocationManager *)manager{
  if( !self.currentHeading ) return YES; // Got nothing, We can assume we got to calibrate.
  else if( self.currentHeading.headingAccuracy < 0 ) return YES; // 0 means invalid heading. we probably need to calibrate
  else if( self.currentHeading.headingAccuracy > 5 )return YES; // 5 degrees is a small value correct for my needs. Tweak yours according to your needs.
  else return NO; // All is good. Compass is precise enough.
}

@end
