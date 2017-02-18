package com.avaa.surfforecast.views;

import android.content.Context;
import android.graphics.PointF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.view.View;

/**
 * Created by Alan on 1 Feb 2017.
 */

public class ParallaxHelper {
    private static final String TAG = "ParallaxHelper";

    private static final double NS2S = 1.0f / 1000000000.0f;

    private final View v;

    private final SensorManager sensorManager;
    private final Sensor sensor;

    private double phoneDistance = 6000;
    private double goBackV = 0;
    private double angleX = 0, angleY = 0;
    private double userX = 0, userY = 0, userZ = 0;
    private double w, h;

//    private final double[] deltaRotationVector = new double[4];
    private double timestamp = 0;

    private final SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (setDeviceAngles(event)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) v.postInvalidateOnAnimation();
                else v.postInvalidate();
            }
        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int i) { }
    };


//    private static final double NS2S = 1.0f / 1000000000.0f;
//    private final SensorEventListener sensorEventListener = new SensorEventListener() {
//        @Override
//        public void onSensorChanged(SensorEvent event) {
//            if (timestamp != 0) {
//                final double dT = (event.timestamp - timestamp) * NS2S;
//                double dAX = event.values[0];
//                double dAY = event.values[1];
//                angleX += dAX * dT;
//                angleY += dAY * dT;
//            }
//            timestamp = event.timestamp;
//        }
//        @Override
//        public void onAccuracyChanged(Sensor sensor, int i) { }
//    };


    public ParallaxHelper(View v) {
        this.v = v;
        this.w = v.getWidth();
        this.h = v.getHeight();

        angleX = 0;
        angleY = 0;

        userX = w/2;
        userY = h/2;

        sensorManager = (SensorManager)v.getContext().getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        v.addOnLayoutChangeListener((v1, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            this.w = right-left;
            this.h = bottom-top;
        });
    }


    public void resume() {
        timestamp = 0;
        sensorManager.registerListener(sensorEventListener, sensor, SensorManager.SENSOR_DELAY_FASTEST);
    }
    public void stop() {
        sensorManager.unregisterListener(sensorEventListener);
    }


    public boolean setDeviceAngles(SensorEvent event) {
        // This time step's delta rotation to be multiplied by the current rotation
        // after computing it from the gyro sample data.
        if (timestamp != 0) {
            final double dT = (event.timestamp - timestamp) * NS2S;
            // Axis of the rotation sample, not normalized yet.
            double axisX = event.values[0];
            double axisY = event.values[1];
//            double axisZ = event.values[2];

            // Calculate the angular speed of the sample
//            double omegaMagnitude = Math.sqrt(axisX*axisX + axisY*axisY + axisZ*axisZ);

            // Normalize the rotation vector if it's big enough to get the axis
//            if (omegaMagnitude > 80) {
//                axisX /= omegaMagnitude;
//                axisY /= omegaMagnitude;
////                axisZ /= omegaMagnitude;
//            }

            // Integrate around this axis with the angular speed by the time step
            // in order to get a delta rotation from this sample over the time step
            // We will convert this axis-angle representation of the delta rotation
            // into a quaternion before turning it into the rotation matrix.
//            double thetaOverTwo = omegaMagnitude * dT / 2.0f;
//            double sinThetaOverTwo = Math.sin(thetaOverTwo);
//            double cosThetaOverTwo = Math.cos(thetaOverTwo);
//            deltaRotationVector[0] = sinThetaOverTwo * axisX;
//            deltaRotationVector[1] = sinThetaOverTwo * axisY;
//            deltaRotationVector[2] = sinThetaOverTwo * axisZ;
//            deltaRotationVector[3] = cosThetaOverTwo;

            //Log.i(TAG, "Deltas: " + axisX + " " + axisY + ", " + deltaRotationVector[0] + " " + deltaRotationVector[1]);

            double maxDA = Math.PI / 8.0;

            angleX += Math.max(-maxDA, Math.min(maxDA, axisX * dT));
            angleY += Math.max(-maxDA, Math.min(maxDA, axisY * dT));

            double angleRange = Math.PI / 4.0;
            angleX = Math.max(-angleRange, Math.min(angleRange, angleX));
            angleY = Math.max(-angleRange, Math.min(angleRange, angleY));

            if (axisX*dT < 0.0015 && axisY*dT < 0.0015) {
                double d = dT;
                if (goBackV >= d) goBackV -= d;
                else goBackV = 0;

                double k = Math.min(1, goBackV);
                k*=k;
                angleX *= 0.995 + k*0.005;
                angleY *= 0.995 + k*0.005;
            }
            else {
                goBackV = 1.5;
            }

//            Log.i(TAG, "Angles: " + angleX*180/Math.PI + ", " + angleY*180/Math.PI + " dT:" + dT);

            userX = Math.sin(-angleY) * phoneDistance + w/2.0;
            userY = Math.sin(-angleX) * phoneDistance + h/2.0;
            userZ = Math.cos(-(Math.sqrt(angleX*angleX + angleY*angleY))) * phoneDistance;

            timestamp = event.timestamp;
//            double[] deltaRotationMatrix = new double[9];
//            //SensorManager.getRotationMatrixFromVector(deltaRotationMatrix, deltaRotationVector);

            return true;
        }
        timestamp = event.timestamp;
//        double[] deltaRotationMatrix = new double[9];
//        //SensorManager.getRotationMatrixFromVector(deltaRotationMatrix, deltaRotationVector);

        return false;
    }


//    public double getK(double z) {
//        return userZ / (userZ - z);
//    }
    public PointF applyParallax(double x, double y, double z) {
        if (userZ == 0) return new PointF((float)x, (float)y);
        double k = userZ / (userZ - z);
        return new PointF((float)(userX + (x-userX) * k), (float)(userY + (y-userY) * k));
    }
}
