package com.cocoricostudio.cameraview;

import android.hardware.Camera;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Display;
import android.view.Surface;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

class CameraUtils {

    private static final String TAG = CameraUtils.class.getSimpleName();

    /**
     * @param screenRotation The rotation of the screen (see. {@link Display#getRotation()}).
     * @param cameraId       The id of the camera.
     * @return The angle that the camera preview should be rotated clockwise. Valid values are 0, 90, 180, and 270.
     */
    static int getDisplayOrientation(int screenRotation, int cameraId) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int degrees = 0;
        switch (screenRotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
    }

    @NonNull
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
        Log.v(TAG, "Optimal preview size for " + w + "x" + h + " is: " + optimalSize.width + "x" + optimalSize.height);
        return optimalSize;
    }

    @NonNull
    static Camera.Size getClosestPreviewSize(List<Camera.Size> sizes, int w, int h) {
        Collections.sort(sizes, new CameraSizeComparator(w, h));
        logSizes("Sorted to closest of " + w + "x" + h + ":", sizes);
        Camera.Size closest = sizes.get(0);
        Log.v(TAG, "Closest preview size of " + w + "x" + h + " is: " + closest.width + "x" + closest.height);
        return closest;
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

    public static String logScreenRotation(int screenRotation) {
        switch (screenRotation) {
            case Surface.ROTATION_0:
                return "0";
            case Surface.ROTATION_90:
                return "90";
            case Surface.ROTATION_180:
                return "180";
            case Surface.ROTATION_270:
                return "270";
        }
        return "Unknown";
    }

    static class CameraSizeComparator implements Comparator<Camera.Size> {

        private final int mTargetWidth;
        private final int mTargetHeight;
        private final double mArea;
        private final double mRatio;

        CameraSizeComparator(int width, int height) {
            mTargetWidth = width;
            mTargetHeight = height;
            mArea = computeArea(width, height);
            mRatio = computeRatio(width, height);
        }

        @Override
        public int compare(Camera.Size lhs, Camera.Size rhs) {
            if (lhs.width == rhs.width && lhs.height == rhs.height) return 0; // equals
            if (lhs.width == mTargetWidth && lhs.height == mTargetHeight) {
//                Log.v(TAG, lhs.width + "x" + lhs.height + " is exact match. So greater than " + rhs.width + "x" + rhs.height);
                return -1; // exact match => greater
            }
            if (rhs.width == mTargetWidth && rhs.height == mTargetHeight) {
//                Log.v(TAG, rhs.width + "x" + rhs.height + " is exact match. So greater than " + lhs.width + "x" + lhs.height);
                return 1; // exact match => greater
            }

            double lhsArea = getArea(lhs) / mArea;
            double lhsRatio = getRatio(lhs) / mRatio;
            // how far lhs is from target:
            double lhsTargetDistance = (Math.abs(1 - lhsArea) + Math.abs(1 - lhsRatio)) * 0.5;

            double rhsArea = getArea(rhs) / mArea;
            double rhsRatio = getRatio(rhs) / mRatio;
            double rhsTargetDistance = (Math.abs(1 - rhsArea) + Math.abs(1 - rhsRatio)) * 0.5;

//            Log.v(TAG, rhs.width + "x" + rhs.height + " is " + (rhsTargetDistance < lhsTargetDistance ? "better" : "lower") + " than " + lhs.width + "x" + lhs.height + " " +
//                    "(dArea: " + format(1 - rhsArea) + " vs " + format(1 - lhsArea) +
//                    ",dRatio: " + format(1 - rhsRatio) + " vs " + format(1 - lhsRatio) +
//                    " => distance: " + format(rhsTargetDistance) + " vs " + format(lhsTargetDistance) + ")");
            return rhsTargetDistance < lhsTargetDistance ? 1 : -1; // The closer to 1 (ie. to target), the greater
        }

//        private String format(double number) {
//            return String.format(Locale.US, "%.2f", number);
//        }

        private double computeArea(int width, int height) {
            return width * height;
        }

        private double computeRatio(int width, int height) {
            return (double) width / height;
        }

        private double getArea(Camera.Size size) {
            return computeArea(size.width, size.height);
        }

        private double getRatio(Camera.Size size) {
            return computeRatio(size.width, size.height);
        }
    }
}

