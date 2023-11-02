// Type definitions for cordova-plugin-compass-accuracy
// Project: https://github.com/dpa99c/cordova-plugin-compass-accuracy
// Definitions by: Dave Alden <https://github.com/dpa99c>
// Definitions: https://github.com/DefinitelyTyped/DefinitelyTyped

export interface CompassAccuracyAccuracy{
    HIGH: number;
    MEDIUM: number;
    LOW: number;
    UNRELIABLE: number;
    UNKNOWN: number;
}

export interface CompassAccuracyResultType{
    STARTED: string;
    ACCURACY_CHANGED: string;
}
export interface CompassAccuracyStartMonitoringSuccessResult {
    type: CompassAccuracyResultType;
    currentAccuracy: CompassAccuracyAccuracy;
    requiredAccuracy: CompassAccuracyAccuracy;
}

declare namespace CompassAccuracyPlugin{
    interface CompassAccuracyPluginStatic{
        ACCURACY:CompassAccuracyAccuracy;
        RESULT_TYPE:CompassAccuracyResultType;
        startMonitoring (onSuccess: (result:CompassAccuracyStartMonitoringSuccessResult) => void, onError: (error: string) => void) : void;
        stopMonitoring(onSuccess: () => void, onError: (error:string) => void) : void;
        getCurrentAccuracy(onSuccess: (accuracy:CompassAccuracyAccuracy) => void, onError: (error:string) => void) : void;
        simulateAccuracyChange(accuracy:CompassAccuracyAccuracy, onSuccess: () => void, onError: (error:string) => void) : void;
    }
}

declare module "cordova-plugin-compass-accuracy" {
    export = CompassAccuracyPlugin;
}

interface Window {
    CompassAccuracy: CompassAccuracyPlugin.CompassAccuracyPluginStatic;
}
declare var CompassAccuracy: CompassAccuracyPlugin.CompassAccuracyPluginStatic;
