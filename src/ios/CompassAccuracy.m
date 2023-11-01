#import "CompassAccuracy.h"

@implementation CompassAccuracy

// Initialization
@synthesize locationManager;
@synthesize currentHeading;

// Constants
static NSString*const LOG_TAG = @"CompassAccuracyPlugin[native]";

static int const highAccuracyThreshold = 5;
static int const mediumAccuracyThreshold = 10;
static int const lowAccuracyThreshold = 15;

static int const unknownAccuracyStatus = -1;
static int const unreliableAccuracyStatus = 0;
static int const lowAccuracyStatus = 1;
static int const mediumAccuracyStatus = 2;
static int const highAccuracyStatus = 3;

// Variables
static bool isMonitoring = false;
static int requiredAccuracy = highAccuracyStatus;
static int currentAccuracy = unknownAccuracyStatus;
static int simulatedCurrentAccuracy = unknownAccuracyStatus;
static CDVInvokedUrlCommand* currentWatchContext = nil;


- (void)pluginInitialize
{
    [self runOnMainThread:^{
        @try {
            self.locationManager = [[CLLocationManager alloc] init];
            self.locationManager.delegate = self;
        }@catch (NSException *exception) {
            [self handlePluginExceptionWithoutContext:exception];
        }
    }];
}

#pragma mark Plugin API
- (void) startMonitoring:(CDVInvokedUrlCommand*)command;{
    @try {
        if([command.arguments objectAtIndex:0] != nil){
            requiredAccuracy = [[command.arguments objectAtIndex:0] intValue];
        }else{
            requiredAccuracy = highAccuracyStatus;
        }
        
        currentWatchContext = command;
        [self setCurrentAccuracy];
        [self.locationManager startUpdatingHeading];
        
        NSMutableDictionary* result = [[NSMutableDictionary alloc] init];
        [result setValue:@"started" forKey:@"type"];
        [result setValue:[NSNumber numberWithInteger:requiredAccuracy] forKey:@"requiredAccuracy"];
        [result setValue:[NSNumber numberWithInteger:currentAccuracy] forKey:@"currentAccuracy"];
        [self sendPluginDictionaryResultAndKeepCallback:result command:command];
        isMonitoring = true;
        
    }@catch (NSException *exception) {
        [self handlePluginExceptionWithContext:exception:command];
    }
}

- (void) stopMonitoring:(CDVInvokedUrlCommand*)command;{
    @try {
        [self.locationManager stopUpdatingHeading];
        isMonitoring = false;
        currentWatchContext = nil;
        [self sendPluginSuccess:command];
    }@catch (NSException *exception) {
        [self handlePluginExceptionWithContext:exception:command];
    }
}

- (void) getCurrentAccuracy:(CDVInvokedUrlCommand*)command;{
    @try {
        NSMutableDictionary* result = [[NSMutableDictionary alloc] init];
        [result setValue:[NSNumber numberWithInteger:currentAccuracy] forKey:@"currentAccuracy"];
        [self sendPluginDictionaryResult:result command:command];
    }@catch (NSException *exception) {
        [self handlePluginExceptionWithContext:exception:command];
    }
}

- (void) simulateAccuracyChange:(CDVInvokedUrlCommand*)command;{
    @try {
        if([command.arguments objectAtIndex:0] != nil){
            simulatedCurrentAccuracy = [[command.arguments objectAtIndex:0] intValue];
        }else{
            @throw([NSException exceptionWithName:@"Error" reason:@"simulated accuracy not specified when calling simulateAccuracyChange" userInfo:nil]);
        }
    }@catch (NSException *exception) {
        [self handlePluginExceptionWithContext:exception:command];
    }
}

#pragma mark Internal functions
- (void) sendPluginSuccess:(CDVInvokedUrlCommand*)command{
    [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK] callbackId:command.callbackId];
}

- (void) sendPluginDictionaryResult:(NSDictionary*)result command:(CDVInvokedUrlCommand*)command {
    CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:result];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void) sendPluginDictionaryResultAndKeepCallback:(NSDictionary*)result command:(CDVInvokedUrlCommand*)command {
    CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:result];
    [pluginResult setKeepCallbackAsBool:YES];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void) handlePluginExceptionWithContext: (NSException*) exception :(CDVInvokedUrlCommand*)command
{
    [self handlePluginExceptionWithoutContext:exception];
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:exception.reason];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void) handlePluginExceptionWithoutContext: (NSException*) exception
{
    NSLog(@"%@ ERROR: %@", LOG_TAG, exception.reason);
}

-(int)getAccuracyStatusForHeadingAccuracy:(CLLocationDirection)headingAccuracy{
    if(!headingAccuracy) return unknownAccuracyStatus;
    if(headingAccuracy < 0) return unreliableAccuracyStatus;
    if(headingAccuracy < highAccuracyThreshold) return highAccuracyStatus;
    if(headingAccuracy < mediumAccuracyThreshold) return mediumAccuracyStatus;
    if(headingAccuracy < lowAccuracyThreshold) return  lowAccuracyStatus;
    return unreliableAccuracyStatus;
}

-(void) setCurrentAccuracy{
    currentAccuracy = [self getAccuracyStatusForHeadingAccuracy:self.currentHeading.headingAccuracy];
}

#pragma mark locationManager delegate

- (BOOL)locationManagerShouldDisplayHeadingCalibration:(CLLocationManager *)manager{
    NSLog(@"%@ locationManagerShouldDisplayHeadingCalibration", LOG_TAG);
    [self setCurrentAccuracy];
    if(!isMonitoring) return NO;
    
    int accuracy = currentAccuracy;
    if(simulatedCurrentAccuracy != unknownAccuracyStatus){
        accuracy = simulatedCurrentAccuracy;
    }
    
    if(currentWatchContext != nil){
        NSMutableDictionary* result = [[NSMutableDictionary alloc] init];
        [result setValue:@"accuracy_changed" forKey:@"type"];
        [result setValue:[NSNumber numberWithInteger:requiredAccuracy] forKey:@"requiredAccuracy"];
        [result setValue:[NSNumber numberWithInteger:accuracy] forKey:@"currentAccuracy"];
        [self sendPluginDictionaryResultAndKeepCallback:result command:currentWatchContext];
    }
    return accuracy < requiredAccuracy;
}

- (void)locationManager:(CLLocationManager*)manager
       didUpdateHeading:(CLHeading*)heading
{
    
    NSLog(@"%@ didUpdateHeading: %f", LOG_TAG, heading.headingAccuracy);
}

- (void)runOnMainThread:(void (^)(void))completeBlock {
    if (![NSThread isMainThread]) {
        dispatch_sync(dispatch_get_main_queue(), ^{
            @try {
                completeBlock();
            }@catch (NSException *exception) {
                [self handlePluginExceptionWithoutContext:exception];
            }
        });
    } else {
        @try {
            completeBlock();
        }@catch (NSException *exception) {
            [self handlePluginExceptionWithoutContext:exception];
        }
    }
}

@end
