package com.cocoricostudio.cameraview;

import android.hardware.Camera;
import android.util.Log;

import java.util.List;
import java.util.Locale;

class CameraUtils {

    private static final String TAG = CameraUtils.class.getSimpleName();

    static Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    static void logSizes(String message, List<Camera.Size> sizes) {
        Log.v(TAG, message);
        for (Camera.Size size : sizes) {
            Log.v(TAG, "- " + size.width + "x" + size.height + ", r: " + logRatio(size.width, size.height));
        }
    }

    public static String logRatio(int width, int height) {
        return String.format(Locale.US, "%.2f", (double) width / height);
    }

}
