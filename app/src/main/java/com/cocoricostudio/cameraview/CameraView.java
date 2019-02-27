package com.cocoricostudio.cameraview;

import android.content.Context;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import java.io.IOException;
import java.util.List;

/**
 * A simple wrapper around a Camera and a SurfaceView that renders a centered preview of the Camera
 * to the surface. We need to center the SurfaceView because not all devices have cameras that
 * support preview sizes at the same aspect ratio as the device's display.
 */
public class CameraView extends ViewGroup implements SurfaceHolder.Callback {
    private final String TAG = CameraView.class.getSimpleName();

    /**
     * The returned value may be {@link Surface#ROTATION_0} (no rotation),
     * {@link Surface#ROTATION_90},
     * {@link Surface#ROTATION_180},
     * or {@link Surface#ROTATION_270}.
     */
    private final int mScreenRotation;

    SurfaceView mSurfaceView;
    SurfaceHolder mHolder;
    Camera.Size mPreviewSize;
    HostLifecycle mHostLifeCycle = new HostLifecycle();

    @Nullable
    Camera mCamera;
    int mCameraId;

    private int mTargetPreviewSizeWidth = Integer.MIN_VALUE;
    private int mTargetPreviewSizeHeight = Integer.MIN_VALUE;
    private boolean mForceLayoutSurface;

    public CameraView(Context context) {
        this(context, null);
    }

    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);

        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mScreenRotation = windowManager != null ? windowManager.getDefaultDisplay().getRotation() : Surface.ROTATION_0;
        Log.v(TAG, "init - with screen rotation: " + CameraUtils.logScreenRotation(mScreenRotation));

        mSurfaceView = new SurfaceView(context);
        addView(mSurfaceView);

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    private void setCamera(@NonNull Camera camera, int cameraId) {
        Log.v(TAG, "setCamera");
        Camera.Parameters parameters = camera.getParameters();

        List<Camera.Size> supportedPreviewSizes = camera.getParameters().getSupportedPreviewSizes();
        CameraUtils.logSizes("Supported Preview Sizes:", supportedPreviewSizes);
        Camera.Size previewSize = mTargetPreviewSizeWidth == Integer.MIN_VALUE || mTargetPreviewSizeHeight == Integer.MIN_VALUE ?
                supportedPreviewSizes.get(0) :
                CameraUtils.getClosestPreviewSize(supportedPreviewSizes, mTargetPreviewSizeWidth, mTargetPreviewSizeHeight);
        boolean invalidateLayout = mPreviewSize == null || (previewSize.width * mPreviewSize.height != previewSize.height * mPreviewSize.width); // ratio did change
        mForceLayoutSurface = invalidateLayout;
        parameters.setPreviewSize(previewSize.width, previewSize.height);

        mPreviewSize = previewSize;

        camera.setDisplayOrientation(CameraUtils.getDisplayOrientation(mScreenRotation, cameraId));
        camera.setParameters(parameters);

        if (invalidateLayout) {
            Log.v(TAG, "Invalidate layout.");
            requestLayout(); // start preview will be done in onSurfaceChange
        } else {
            setPreviewDisplay(camera, mHolder);
            camera.startPreview();
        }
    }

    public void switchCamera() {
        // check for availability of multiple cameras
        if (Camera.getNumberOfCameras() <= 1) {
            return;
        }
        if (mCamera == null) {
            throw new IllegalStateException("onResume() must be called first, before any other camera interaction.");
        }

        Log.v(TAG, "Switch camera.");

        // OK, we have multiple cameras.
        // Release this camera
        mCamera.stopPreview();
        mCamera.release();

        // Acquire the next camera and request Preview to reconfigure parameters.
        int nextCameraId = (mCameraId + 1) % Camera.getNumberOfCameras();
        mCamera = Camera.open(nextCameraId);
        mCameraId = nextCameraId;

        setCamera(mCamera, mCameraId);

        mHostLifeCycle.handleSwitch(mCameraId);
    }

    private void setPreviewDisplay(@NonNull Camera camera, @NonNull SurfaceHolder holder) {
        try {
            camera.setPreviewDisplay(holder);
        } catch (IOException exception) {
            Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
        }
    }

    public void setTargetPreviewSize(int width, int height) {
        Log.v(TAG, "setTargetPreviewSize(" + width + "x" + height + ")," +
                " current target: " + mTargetPreviewSizeWidth + "x" + mTargetPreviewSizeHeight + "," +
                " current camera: " + mCamera);

        if (mTargetPreviewSizeWidth == width && mTargetPreviewSizeHeight == height) {
            // Target already set.
            return;
        }

        mTargetPreviewSizeWidth = width;
        mTargetPreviewSizeHeight = height;

        if (mCamera == null) {
            // The camera is not opened yet.
            return;
        }

        // SetPreviewSize: "if the preview has already started, applications should stop the preview first before changing preview size."
        mCamera.stopPreview();
        setCamera(mCamera, mCameraId);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // We purposely disregard child measurements because act as a
        // wrapper to a SurfaceView that centers the camera preview instead
        // of stretching it.
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        setMeasuredDimension(width, height);

        Log.v(TAG, "onMeasure " + width + "x" + height);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int width = r - l;
        final int height = b - t;
        Log.v(TAG, "onLayout changed: " + changed + ", size: " + width + "x" + height + ", force: " + mForceLayoutSurface);

        if ((mForceLayoutSurface || changed) && getChildCount() > 0) {
            final View child = getChildAt(0);
            if (mForceLayoutSurface) mForceLayoutSurface = false;

            int previewWidth = width;
            int previewHeight = height;
            if (mPreviewSize != null) {
                boolean isCameraLandscape = mScreenRotation == Surface.ROTATION_90 || mScreenRotation == Surface.ROTATION_270;
                previewWidth = isCameraLandscape ? mPreviewSize.width : mPreviewSize.height;
                previewHeight = isCameraLandscape ? mPreviewSize.height : mPreviewSize.width;
            }

            // Center the child SurfaceView within the parent.
            if (width * previewHeight > height * previewWidth) {
                Log.v(TAG, "onLayout case L1. Putting preview " + previewWidth + "x" + previewHeight + " (r" + CameraUtils.logRatio(previewWidth, previewHeight) + ") inside " + width + "x" + height + " (r" + CameraUtils.logRatio(width, height) + ")");
                final int scaledChildWidth = previewWidth * height / previewHeight;
                child.layout((width - scaledChildWidth) / 2, 0,
                        (width + scaledChildWidth) / 2, height);
            } else {
                Log.v(TAG, "onLayout case L2. Putting preview " + previewWidth + "x" + previewHeight + " (r" + CameraUtils.logRatio(previewWidth, previewHeight) + ") inside " + width + "x" + height + " (r" + CameraUtils.logRatio(width, height) + ")");
                final int scaledChildHeight = previewHeight * width / previewWidth;
                child.layout(0, (height - scaledChildHeight) / 2,
                        width, (height + scaledChildHeight) / 2);
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.v(TAG, "surfaceCreated.");
        // The Surface has been created, acquire the camera and tell it where
        // to draw.
        if (mCamera != null) {
            setPreviewDisplay(mCamera, holder);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.v(TAG, "surfaceDestroyed.");
        // Surface will be destroyed when we return, so stop the preview.
        if (mCamera != null) {
            mCamera.stopPreview();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        Log.v(TAG, "surfaceChanged.");

        if (mCamera != null) {
            // Now that surface is the good size, begin the preview.
            setPreviewDisplay(mCamera, holder);
            mCamera.startPreview();
        }
    }

    /**
     * Starts the default camera or restore the previous one opened.
     * <p/>
     * This call is required prior any other interaction with the camera.
     * <p/>
     * To be called in {@link android.app.Activity#onResume() Activity.onResume()}
     * or {@link android.app.Fragment#onResume() Fragment.onResume()}.
     */
    public void onResume() {
        mHostLifeCycle.onResume();
    }

    /**
     * Releases the opened camera. This is <strong>absolutely mandatory</strong>.
     * <p/>
     * Because the Camera object is a shared resource, it's very important to release it when the activity is paused.
     * <p/>
     * <strong>Must</strong> be called in {@link android.app.Activity#onPause() Activity.onPause()}
     * or {@link android.app.Fragment#onPause() Fragment.onPause()}.
     */
    public void onPause() {
        mHostLifeCycle.onPause();
    }

    /**
     * To be called in {@link android.app.Activity#onCreate(Bundle)}
     * or {@link android.app.Activity#onRestoreInstanceState(Bundle)}
     * to restore the previously opened camera.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut
     *                           down then this Bundle contains the data it most recently supplied in
     *                           onSaveInstanceState. Note: Otherwise it is null.
     */
    public void onRestoreEventually(@Nullable Bundle savedInstanceState) {
        mHostLifeCycle.onRestore(savedInstanceState);
    }

    /**
     * To be called in {@link android.app.Activity}
     *
     * @param outState Bundle in which to place your saved state.
     */
    public void onSaveInstanceState(Bundle outState) {
        mHostLifeCycle.onSaveInstanceState(outState);
    }

    class HostLifecycle {

        private static final String CAMERA_ID = "firekast_restore-camera-id";
        private static final String CAMERA_PREVIEW_SIZE_WIDTH = "firekast_restore-width";
        private static final String CAMERA_PREVIEW_SIZE_HEIGHT = "firekast_restore-height";
        private int cameraCurrentlyLocked;

        HostLifecycle() {
            cameraCurrentlyLocked = 0;
        }

        /**
         * This is the entry point, the only way to open the camera for the first time.
         * <p/>
         * The other {@link Camera#open()} is called in {@link #switchCamera()} which throws
         * {@link IllegalStateException} in case {@link #mCamera} is {@code null}.
         */
        void onResume() {
            if (Camera.getNumberOfCameras() == 0) {
                Log.e(TAG, "Current device has no camera.");
                return;
            }
            if (mCamera != null) {
                Log.w(TAG, "onResume should be called once, in your host corresponding lifecycle.");
                return;
            }
            // Open the default camera.
            mCamera = Camera.open(cameraCurrentlyLocked);
            mCameraId = cameraCurrentlyLocked;
            setCamera(mCamera, mCameraId);
            // Start preview will be done in onSurfaceChanged.
        }

        void onPause() {
            // Because the Camera object is a shared resource, it's very
            // important to release it when the activity is paused.
            if (mCamera != null) {
                mCamera.release();
                mCamera = null;
                mCameraId = Integer.MIN_VALUE;
            }
        }

        void handleSwitch(int cameraId) {
            cameraCurrentlyLocked = cameraId;
        }

        void onRestore(@Nullable Bundle savedInstanceState) {
            if (savedInstanceState != null) {
                if (savedInstanceState.containsKey(CAMERA_ID)) {
                    cameraCurrentlyLocked = savedInstanceState.getInt(CAMERA_ID);
                }
                if (savedInstanceState.containsKey(CAMERA_PREVIEW_SIZE_WIDTH) && savedInstanceState.containsKey(CAMERA_PREVIEW_SIZE_HEIGHT)) {
                    mTargetPreviewSizeWidth = savedInstanceState.getInt(CAMERA_PREVIEW_SIZE_WIDTH);
                    mTargetPreviewSizeHeight = savedInstanceState.getInt(CAMERA_PREVIEW_SIZE_HEIGHT);
                }
            }
        }

        void onSaveInstanceState(Bundle outState) {
            outState.putInt(CAMERA_ID, cameraCurrentlyLocked);
            outState.putInt(CAMERA_PREVIEW_SIZE_WIDTH, mTargetPreviewSizeWidth != Integer.MIN_VALUE ? mTargetPreviewSizeWidth : mPreviewSize.width);
            outState.putInt(CAMERA_PREVIEW_SIZE_HEIGHT, mTargetPreviewSizeHeight != Integer.MIN_VALUE ? mTargetPreviewSizeHeight : mPreviewSize.height);
        }
    }
}
