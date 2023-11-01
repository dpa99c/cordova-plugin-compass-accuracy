/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
*/
package cordova.plugin;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;


import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class CompassAccuracy extends CordovaPlugin implements SensorEventListener {
    public static final String TAG = "CompassAccuracy";

    protected static int NO_ACCURACY_RECEIVED = -1000;

    protected static int RELATIVE_LAYOUT_ID = 1000;

    protected static int DEFAULT_HORIZONTAL_MARGIN = 50;
    protected static int DEFAULT_VERTICAL_MARGIN = 100;
    protected static String CALIBRATION_IMAGE_NAME = "calibration";
    protected static String DIALOG_TITLE = "Compass calibration required";
    protected static String CALIBRATION_HINT = "Tilt and move your phone 3 times in a figure-of-eight motion like this.";

    protected CordovaInterface cordova = null;
    protected CallbackContext currentWatchContext = null;
    protected int requiredAccuracy = SensorManager.SENSOR_STATUS_ACCURACY_HIGH;
    protected int previousAccuracy = NO_ACCURACY_RECEIVED;

    protected AlertDialog dialog = null;
    protected TextView accuracyTextView = null;

    protected SpannableString accuracyValue = null;
    protected boolean hasShownDialog = false;

    private SensorManager mSensorManager;
    private Sensor mSensorMagneticField;

    /**
     * Constructor.
     */
    public CompassAccuracy() {}


    /**
     * Sets the context of the Command. This can then be used to do things like
     * get file paths associated with the Activity.
     *
     * @param cordova The context of the main Activity.
     * @param webView The CordovaWebView Cordova is running in.
     */
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        this.cordova = cordova;
        Log.d(TAG, "Initializing CompassAccuracy plugin");

        mSensorManager = cordova.getActivity().getSystemService(SensorManager.class);
        mSensorMagneticField = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        mSensorManager.registerListener(this, mSensorMagneticField, SensorManager.SENSOR_DELAY_NORMAL);
    }

    /**
     * Executes the request and returns PluginResult.
     *
     * @param action            The action to execute.
     * @param args              JSONArry of arguments for the plugin.
     * @param callbackContext   The callback id used when calling back into JavaScript.
     * @return                  True if the action was valid, false if not.
     */
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        boolean result = true;
        try {
            if(action.equals("startMonitoring")) {
                startMonitoring(args, callbackContext);
            }else if(action.equals("stopMonitoring")) {
                stopMonitoring(callbackContext);
            }else if(action.equals("getCurrentAccuracy")) {
                getCurrentAccuracy(callbackContext);
            }else if(action.equals("simulateAccuracyChange")) {
                simulateAccuracyChange(args, callbackContext);
            }else {
                handleError("Invalid action", callbackContext);
                result = false;
            }
        }catch(Exception e ) {
            handleError(e.getMessage(), callbackContext);
            result = false;
        }
        return result;
    }

    protected void startMonitoring(JSONArray args, CallbackContext callbackContext) throws JSONException {
        currentWatchContext = callbackContext;
        // if args array contains requiredAccuracy, set it
        if(!args.isNull(0)) {
            requiredAccuracy = args.getInt(0);
        }

        JSONObject resultObj = new JSONObject();
        resultObj.put("type", "started");
        resultObj.put("requiredAccuracy", requiredAccuracy);
        resultObj.put("currentAccuracy", previousAccuracy);
        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, resultObj);
        pluginResult.setKeepCallback(true);
        callbackContext.sendPluginResult(pluginResult);

        // If current accuracy has been received, prior to starting monitoring, evaluate it
        if(previousAccuracy != NO_ACCURACY_RECEIVED) {
            Log.d(TAG, "Current accuracy has been received prior to starting monitoring: evaluating it");
            evaluateChangedAccuracy(previousAccuracy);
        }
    }

    protected void stopMonitoring(CallbackContext callbackContext) throws JSONException {
        if(currentWatchContext != null){
            JSONObject resultObj = new JSONObject();
            resultObj.put("type", "stopped");
            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, resultObj);
            currentWatchContext.sendPluginResult(pluginResult);
        }
        currentWatchContext = null;
        hasShownDialog = false;
        callbackContext.success();
    }

    protected void getCurrentAccuracy(CallbackContext callbackContext) throws JSONException {
        JSONObject resultObj = new JSONObject();
        resultObj.put("currentAccuracy", previousAccuracy);
        callbackContext.success(resultObj);
    }

    protected void simulateAccuracyChange(JSONArray args, CallbackContext callbackContext) throws JSONException {
        hasShownDialog = false;
        evaluateChangedAccuracy(args.getInt(0));
        callbackContext.success();
    }


    protected void handleError(String errorMsg, CallbackContext context){
        Log.e(TAG, errorMsg);
        if(context != null){
            context.error(errorMsg);
        }
    }

    protected void evaluateChangedAccuracy(int currentAccuracy){
        boolean isInaccurate = false;
        try{
            switch(requiredAccuracy){
                case SensorManager.SENSOR_STATUS_ACCURACY_HIGH:
                    if(currentAccuracy < SensorManager.SENSOR_STATUS_ACCURACY_HIGH) {
                        isInaccurate = true;
                    }
                    break;
                case SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM:
                    if(currentAccuracy < SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM) {
                        isInaccurate = true;
                    }
                    break;
                case SensorManager.SENSOR_STATUS_ACCURACY_LOW:
                    if(currentAccuracy < SensorManager.SENSOR_STATUS_ACCURACY_LOW) {
                        isInaccurate = true;
                    }
                    break;
            }

            if(currentWatchContext != null){
                JSONObject resultObj = new JSONObject();
                resultObj.put("type", "accuracy_changed");
                resultObj.put("requiredAccuracy", requiredAccuracy);
                resultObj.put("currentAccuracy", currentAccuracy);
                PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, resultObj);
                pluginResult.setKeepCallback(true);
                currentWatchContext.sendPluginResult(pluginResult);

                if(isInaccurate) {
                    if(!hasShownDialog){
                        showDialog(currentAccuracy);
                        hasShownDialog = true; // only show dialog once
                    }
                }
                if(dialog != null){
                    setAccuracyText(currentAccuracy);
                }
            }
        }catch (Exception e) {
            handleError(e.getMessage(), currentWatchContext);
        }
    }

    protected void showDialog(int currentAccuracy) {
        if(dialog != null){
            return;
        }

        Activity activity = this.cordova.getActivity();
        Context context = this.cordova.getContext();
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);

                // Create a parent RelativeLayout for the dialog
                RelativeLayout dialogRelativeLayout = new RelativeLayout(context);
                dialogRelativeLayout.setId(RELATIVE_LAYOUT_ID);
                RelativeLayout.LayoutParams dialogRelativeLayoutParams = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT,
                        RelativeLayout.LayoutParams.MATCH_PARENT);
                dialogRelativeLayout.setLayoutParams(dialogRelativeLayoutParams);
                dialogRelativeLayout.setBackgroundColor(0xFFFFFFFF);


                // Create title TextView and align it to the top of the parent layout
                TextView titleTextView = new TextView(context);
                titleTextView.setId(RELATIVE_LAYOUT_ID + 1);
                RelativeLayout.LayoutParams titleTextParams = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT);
                titleTextParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
                titleTextParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                titleTextParams.setMargins(DEFAULT_HORIZONTAL_MARGIN, DEFAULT_HORIZONTAL_MARGIN, DEFAULT_HORIZONTAL_MARGIN, 0); // left, top, right, bottom in pixels
                titleTextView.setLayoutParams(titleTextParams);
                titleTextView.setText(DIALOG_TITLE);
                titleTextView.setTypeface(titleTextView.getTypeface(), android.graphics.Typeface.BOLD);
                titleTextView.setTextSize(22); // in sp
                titleTextView.setTextColor(0xFF000000);
                titleTextView.setGravity(android.view.Gravity.CENTER);
                dialogRelativeLayout.addView(titleTextView);

                // Create title TextView and align it below the title TextView
                TextView hintTextView = new TextView(context);
                hintTextView.setId(RELATIVE_LAYOUT_ID + 2);
                RelativeLayout.LayoutParams hintTextParams = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT);
                hintTextParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
                hintTextParams.addRule(RelativeLayout.BELOW, titleTextView.getId());
                hintTextParams.setMargins(DEFAULT_HORIZONTAL_MARGIN, DEFAULT_VERTICAL_MARGIN, DEFAULT_HORIZONTAL_MARGIN, 0); // left, top, right, bottom in pixels
                hintTextView.setLayoutParams(hintTextParams);
                hintTextView.setText(CALIBRATION_HINT);
                hintTextView.setTextSize(18); // in sp
                hintTextView.setTextColor(0xFF000000);
                hintTextView.setGravity(android.view.Gravity.CENTER);
                dialogRelativeLayout.addView(hintTextView);

                // Create an ImageView and align it to the center of the parent layout
                ImageView imageView = new ImageView(context);
                imageView.setId(RELATIVE_LAYOUT_ID + 3);
                RelativeLayout.LayoutParams imageParams = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT);
                imageParams.addRule(RelativeLayout.CENTER_IN_PARENT);
                imageParams.setMargins(DEFAULT_HORIZONTAL_MARGIN, DEFAULT_VERTICAL_MARGIN, DEFAULT_HORIZONTAL_MARGIN, DEFAULT_VERTICAL_MARGIN); // left, top, right, bottom in pixels
                imageView.setLayoutParams(imageParams);

                int defaultSmallIconResID = context.getResources().getIdentifier(CALIBRATION_IMAGE_NAME, "drawable", context.getPackageName());
                imageView.setImageResource(defaultSmallIconResID);
                dialogRelativeLayout.addView(imageView);

                // Create a Button and align it to the bottom of the parent layout
                Button button = new Button(context);
                button.setId(RELATIVE_LAYOUT_ID + 4);
                RelativeLayout.LayoutParams buttonParams = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT);
                buttonParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
                buttonParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                int buttonBottomOffset = 16;
                buttonParams.setMargins(DEFAULT_HORIZONTAL_MARGIN, DEFAULT_VERTICAL_MARGIN+buttonBottomOffset, DEFAULT_HORIZONTAL_MARGIN, DEFAULT_VERTICAL_MARGIN); // left, top, right, bottom in pixels

                button.setLayoutParams(buttonParams);
                button.setText("DONE");
                button.setBackgroundColor(Color.TRANSPARENT);
                button.setTextColor(0xFF007AFF);
                button.setTranslationY(-(buttonBottomOffset) * context.getResources().getDisplayMetrics().density);
                dialogRelativeLayout.addView(button);

                // Create the accuracy TextView and align it above the button
                accuracyTextView = new TextView(context);
                accuracyTextView.setId(RELATIVE_LAYOUT_ID + 5);
                RelativeLayout.LayoutParams accuracyTextParams = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT);
                accuracyTextParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
                accuracyTextParams.addRule(RelativeLayout.ABOVE, button.getId());
                accuracyTextParams.setMargins(DEFAULT_HORIZONTAL_MARGIN, DEFAULT_VERTICAL_MARGIN, DEFAULT_HORIZONTAL_MARGIN, 0); // left, top, right, bottom in pixels
                accuracyTextView.setLayoutParams(accuracyTextParams);
                accuracyTextView.setTextSize(18); // in sp
                accuracyTextView.setTextColor(0xFF000000);
                accuracyTextView.setGravity(android.view.Gravity.CENTER);

                setAccuracyText(currentAccuracy);
                dialogRelativeLayout.addView(accuracyTextView);

                // Set the parent layout as the view of the builder
                builder.setView(dialogRelativeLayout);

                // Create and show the dialog
                dialog = builder.create();
                dialog.show();

                // Set a click listener for the button to dismiss the dialog
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                        dialog = null;
                        accuracyTextView = null;
                    }
                });

                // Make the dialog modal by setting its cancelable properties to false
                dialog.setCancelable(false);
                dialog.setCanceledOnTouchOutside(false);
            }
        });
    }

    private String getAccuracyName(int accuracy){
        switch(accuracy){
            case SensorManager.SENSOR_STATUS_ACCURACY_HIGH:
                return "HIGH";
            case SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM:
                return "MEDIUM";
            case SensorManager.SENSOR_STATUS_ACCURACY_LOW:
                return "LOW";
            case SensorManager.SENSOR_STATUS_UNRELIABLE:
                return "UNRELIABLE";
            default:
                return "UNKNOWN";
        }
    }

    private int getAccuracyColor(int accuracy){
        switch(accuracy){
            case SensorManager.SENSOR_STATUS_ACCURACY_HIGH:
                return Color.GREEN;
            case SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM:
                // orange
                return Color.rgb(255, 165, 0);
            case SensorManager.SENSOR_STATUS_ACCURACY_LOW:
                return Color.RED;
            case SensorManager.SENSOR_STATUS_UNRELIABLE:
                return Color.GRAY;
            default:
                return Color.BLACK;
        }
    }

    private void setAccuracyText(int accuracy){
        if(accuracyTextView != null){
            String accuracyName = getAccuracyName(accuracy);
            int accuracyColor = getAccuracyColor(accuracy);

            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // Clear the text view
                    accuracyTextView.setText("");

                    SpannableString compassAccuracyPrefix = new SpannableString("Compass accuracy: ");
                    compassAccuracyPrefix.setSpan(new ForegroundColorSpan(Color.BLACK), 0, compassAccuracyPrefix.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    accuracyTextView.setText(compassAccuracyPrefix, TextView.BufferType.SPANNABLE);

                    accuracyValue = new SpannableString(accuracyName);
                    accuracyValue.setSpan(new StyleSpan(Typeface.BOLD), 0, accuracyValue.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    accuracyValue.setSpan(new ForegroundColorSpan(accuracyColor), 0, accuracyValue.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    accuracyTextView.append(accuracyValue);
                }
            });
        }
    }

    /***********
     * Overrides
     ***********/

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(previousAccuracy == NO_ACCURACY_RECEIVED || previousAccuracy != sensorEvent.accuracy){
            onAccuracyChanged(sensorEvent.sensor, sensorEvent.accuracy);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int currentAccuracy) {
        if(previousAccuracy == currentAccuracy) return;

        previousAccuracy = currentAccuracy;
        Log.i(TAG, "Magnetometer accuracy changed to " + getAccuracyName(currentAccuracy));

        evaluateChangedAccuracy(currentAccuracy);
    }
}
