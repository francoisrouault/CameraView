package com.cocoricostudio.cameraview;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import java.io.IOException;
import java.util.List;

/**
 * A simple wrapper around a Camera and a SurfaceView that renders a centered preview of the Camera
 * to the surface. We need to center the SurfaceView because not all devices have cameras that
 * support preview sizes at the same aspect ratio as the device's display.
 */
class CameraView extends ViewGroup implements SurfaceHolder.Callback {
    private final String TAG = CameraView.class.getSimpleName();

    SurfaceView mSurfaceView;
    SurfaceHolder mHolder;
    Camera.Size mPreviewSize;
    List<Camera.Size> mSupportedPreviewSizes;
    Camera mCamera;

    CameraView(Context context) {
        super(context);
        Log.v(TAG, "init");

        mSurfaceView = new SurfaceView(context);
        addView(mSurfaceView);

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void setCamera(Camera camera) {
        Log.v(TAG, "Set camera: " + camera);
        mCamera = camera;
        if (mCamera != null) {
            mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
            CameraUtils.logSizes("Supported Preview Sizes:", mSupportedPreviewSizes);
            requestLayout();
        }
    }

    public void switchCamera(Camera camera) {
        Log.v(TAG, "Switch camera.");
        setCamera(camera);
        try {
            camera.setPreviewDisplay(mHolder);
        } catch (IOException exception) {
            Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
        }
        Camera.Parameters parameters = camera.getParameters();
        parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
        requestLayout();

        camera.setParameters(parameters);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // We purposely disregard child measurements because act as a
        // wrapper to a SurfaceView that centers the camera preview instead
        // of stretching it.
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        setMeasuredDimension(width, height);

        if (mSupportedPreviewSizes != null) {
            mPreviewSize = CameraUtils.getOptimalPreviewSize(mSupportedPreviewSizes, width, height);
            Log.v(TAG, "onMeasure " + width + "x" + height + ". Preview Size : " + mPreviewSize.width + "x" + mPreviewSize.height);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int width = r - l;
        final int height = b - t;
        Log.v(TAG, "onLayout changed: " + changed + ", size: " + width + "x" + height);

        if (changed && getChildCount() > 0) {
            final View child = getChildAt(0);

            int previewWidth = width;
            int previewHeight = height;
            if (mPreviewSize != null) {
                previewWidth = mPreviewSize.width;
                previewHeight = mPreviewSize.height;
            }

            // Center the child SurfaceView within the parent.
            if (width * previewHeight > height * previewWidth) {
                final int scaledChildWidth = previewWidth * height / previewHeight;
                child.layout((width - scaledChildWidth) / 2, 0,
                        (width + scaledChildWidth) / 2, height);
            } else {
                final int scaledChildHeight = previewHeight * width / previewWidth;
                child.layout(0, (height - scaledChildHeight) / 2,
                        width, (height + scaledChildHeight) / 2);
            }
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        Log.v(TAG, "surfaceCreated.");
        // The Surface has been created, acquire the camera and tell it where
        // to draw.
        try {
            if (mCamera != null) {
                mCamera.setPreviewDisplay(holder);
            }
        } catch (IOException exception) {
            Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.v(TAG, "surfaceDestroyed.");
        // Surface will be destroyed when we return, so stop the preview.
        if (mCamera != null) {
            mCamera.stopPreview();
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        Log.v(TAG, "surfaceChanged.");

        // Now that the size is known, set up the camera parameters and begin
        // the preview.
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
        requestLayout();

        mCamera.setParameters(parameters);
        mCamera.startPreview();
    }

}
