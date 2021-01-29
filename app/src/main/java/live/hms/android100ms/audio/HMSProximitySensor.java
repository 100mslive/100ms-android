package live.hms.android100ms.audio;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.Nullable;

import live.hms.android100ms.util.Utils;
import org.webrtc.ThreadUtils;

/**
 * AppRTCProximitySensor manages functions related to the proximity sensor in
 * the AppRTC demo.
 * On most device, the proximity sensor is implemented as a boolean-sensor.
 * It returns just two values "NEAR" or "FAR". Thresholding is done on the LUX
 * value i.e. the LUX value of the light sensor is compared with a threshold.
 * A LUX-value more than the threshold means the proximity sensor returns "FAR".
 * Anything less than the threshold value and the sensor  returns "NEAR".
 */
public class HMSProximitySensor implements SensorEventListener {
  private static final String TAG = "AppRTCProximitySensor";

  // This class should be created, started and stopped on one thread
  // (e.g. the main thread). We use |nonThreadSafe| to ensure that this is
  // the case. Only active when |DEBUG| is set to true.
  private final ThreadUtils.ThreadChecker threadChecker = new ThreadUtils.ThreadChecker();

  private final Runnable onSensorStateListener;
  private final SensorManager sensorManager;
  @Nullable
  private Sensor proximitySensor;
  private boolean lastStateReportIsNear;

  /** Construction */
  static HMSProximitySensor create(Context context, Runnable sensorStateListener) {
    return new HMSProximitySensor(context, sensorStateListener);
  }

  private HMSProximitySensor(Context context, Runnable sensorStateListener) {
    Log.d(TAG, "AppRTCProximitySensor" + live.hms.android100ms.util.ThreadUtils.getThreadInfo());
    onSensorStateListener = sensorStateListener;
    sensorManager = ((SensorManager) context.getSystemService(Context.SENSOR_SERVICE));
  }

  /**
   * Activate the proximity sensor. Also do initialization if called for the
   * first time.
   */
  public boolean start() {
    threadChecker.checkIsOnValidThread();
    Log.d(TAG, "start" + live.hms.android100ms.util.ThreadUtils.getThreadInfo());
    if (!initDefaultSensor()) {
      // Proximity sensor is not supported on this device.
      return false;
    }
    sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
    return true;
  }

  /** Deactivate the proximity sensor. */
  public void stop() {
    threadChecker.checkIsOnValidThread();
    Log.d(TAG, "stop" + live.hms.android100ms.util.ThreadUtils.getThreadInfo());
    if (proximitySensor == null) {
      return;
    }
    sensorManager.unregisterListener(this, proximitySensor);
  }

  /** Getter for last reported state. Set to true if "near" is reported. */
  public boolean sensorReportsNearState() {
    threadChecker.checkIsOnValidThread();
    return lastStateReportIsNear;
  }

  @Override
  public final void onAccuracyChanged(Sensor sensor, int accuracy) {
    threadChecker.checkIsOnValidThread();
    Utils.assertIsTrue(sensor.getType() == Sensor.TYPE_PROXIMITY);
    if (accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
      Log.e(TAG, "The values returned by this sensor cannot be trusted");
    }
  }

  @Override
  public final void onSensorChanged(SensorEvent event) {
    threadChecker.checkIsOnValidThread();
    Utils.assertIsTrue(event.sensor.getType() == Sensor.TYPE_PROXIMITY);
    // As a best practice; do as little as possible within this method and
    // avoid blocking.
    float distanceInCentimeters = event.values[0];
    if (distanceInCentimeters < proximitySensor.getMaximumRange()) {
      Log.d(TAG, "Proximity sensor => NEAR state");
      lastStateReportIsNear = true;
    } else {
      Log.d(TAG, "Proximity sensor => FAR state");
      lastStateReportIsNear = false;
    }

    // Report about new state to listening client. Client can then call
    // sensorReportsNearState() to query the current state (NEAR or FAR).
    if (onSensorStateListener != null) {
      onSensorStateListener.run();
    }

    Log.d(TAG, "onSensorChanged" + live.hms.android100ms.util.ThreadUtils.getThreadInfo() + ": "
            + "accuracy=" + event.accuracy + ", timestamp=" + event.timestamp + ", distance="
            + event.values[0]);
  }

  /**
   * Get default proximity sensor if it exists. Tablet devices (e.g. Nexus 7)
   * does not support this type of sensor and false will be returned in such
   * cases.
   */
  private boolean initDefaultSensor() {
    if (proximitySensor != null) {
      return true;
    }
    proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
    if (proximitySensor == null) {
      return false;
    }
    logProximitySensorInfo();
    return true;
  }

  /** Helper method for logging information about the proximity sensor. */
  private void logProximitySensorInfo() {
    if (proximitySensor == null) {
      return;
    }
    StringBuilder info = new StringBuilder("Proximity sensor: ");
    info.append("name=").append(proximitySensor.getName());
    info.append(", vendor: ").append(proximitySensor.getVendor());
    info.append(", power: ").append(proximitySensor.getPower());
    info.append(", resolution: ").append(proximitySensor.getResolution());
    info.append(", max range: ").append(proximitySensor.getMaximumRange());
    info.append(", min delay: ").append(proximitySensor.getMinDelay());
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
      // Added in API level 20.
      info.append(", type: ").append(proximitySensor.getStringType());
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      // Added in API level 21.
      info.append(", max delay: ").append(proximitySensor.getMaxDelay());
      info.append(", reporting mode: ").append(proximitySensor.getReportingMode());
      info.append(", isWakeUpSensor: ").append(proximitySensor.isWakeUpSensor());
    }
    Log.d(TAG, info.toString());
  }
}
