/**
 *  Compass Accuracy plugin
 *
 *  Copyright (c) 2023 Dave Alden (Working Edge Ltd.)
 **/
var CompassAccuracy = function(){};

CompassAccuracy.prototype.RESULT_TYPE = {
	STARTED: "started",
	ACCURACY_CHANGED: "accuracy_changed"
};

CompassAccuracy.prototype.ACCURACY = {
	HIGH: "high",
	MEDIUM: "medium",
	LOW: "low",
	UNRELIABLE: "unreliable",
	UNKNOWN: "unknown"
};

var NATIVE_SENSOR_STATUS_ACCURACY = {
	UNRELIABLE: 0,
	LOW: 1,
	MEDIUM: 2,
	HIGH: 3
}

function jsAccuracyToNativeAccuracy(jsAccuracy){
	switch(jsAccuracy){
		case CompassAccuracy.prototype.ACCURACY.UNRELIABLE:
			return NATIVE_SENSOR_STATUS_ACCURACY.UNRELIABLE;
		case CompassAccuracy.prototype.ACCURACY.LOW:
			return NATIVE_SENSOR_STATUS_ACCURACY.LOW;
		case CompassAccuracy.prototype.ACCURACY.MEDIUM:
			return NATIVE_SENSOR_STATUS_ACCURACY.MEDIUM;
		case CompassAccuracy.prototype.ACCURACY.HIGH:
			return NATIVE_SENSOR_STATUS_ACCURACY.HIGH;
		default:
			return NATIVE_SENSOR_STATUS_ACCURACY.UNKNOWN;
	}
}

function nativeAccuracyToJsAccuracy(nativeAccuracy){
	switch(nativeAccuracy){
		case NATIVE_SENSOR_STATUS_ACCURACY.UNRELIABLE:
			return CompassAccuracy.prototype.ACCURACY.UNRELIABLE;
		case NATIVE_SENSOR_STATUS_ACCURACY.LOW:
			return CompassAccuracy.prototype.ACCURACY.LOW;
		case NATIVE_SENSOR_STATUS_ACCURACY.MEDIUM:
			return CompassAccuracy.prototype.ACCURACY.MEDIUM;
		case NATIVE_SENSOR_STATUS_ACCURACY.HIGH:
			return CompassAccuracy.prototype.ACCURACY.HIGH;
		default:
			return CompassAccuracy.prototype.ACCURACY.UNKNOWN;
	}
}


CompassAccuracy.prototype.startMonitoring = function(successCallback, errorCallback, requiredAccuracy) {
	var _requiredAccuracy;
	if(requiredAccuracy !== undefined){
		_requiredAccuracy = jsAccuracyToNativeAccuracy(requiredAccuracy);
	}
	return cordova.exec(function(result){
		if(result.requiredAccuracy !== undefined){
			result.requiredAccuracy = nativeAccuracyToJsAccuracy(result.requiredAccuracy);
		}
		if(result.currentAccuracy !== undefined){
			result.currentAccuracy = nativeAccuracyToJsAccuracy(result.currentAccuracy);
		}
		successCallback(result);
	}, errorCallback, 'CompassAccuracy', 'startMonitoring', [_requiredAccuracy]);
};
CompassAccuracy.prototype.stopMonitoring = function(successCallback, errorCallback) {
	return cordova.exec(successCallback, errorCallback, 'CompassAccuracy', 'stopMonitoring', []);
};
CompassAccuracy.prototype.getCurrentAccuracy = function(successCallback, errorCallback) {
	return cordova.exec(successCallback, errorCallback, 'CompassAccuracy', 'getCurrentAccuracy', []);
};
CompassAccuracy.prototype.simulateAccuracyChange = function(simulatedAccuracy, successCallback, errorCallback) {
	if(typeof simulatedAccuracy !== "string" || !CompassAccuracy.prototype.ACCURACY[simulatedAccuracy.toUpperCase()]){
		errorCallback("Invalid simulated accuracy: " + simulatedAccuracy);
		return;
	}
	return cordova.exec(successCallback, errorCallback, 'CompassAccuracy', 'simulateAccuracyChange', [jsAccuracyToNativeAccuracy(simulatedAccuracy)]);
};

module.exports = new CompassAccuracy();

