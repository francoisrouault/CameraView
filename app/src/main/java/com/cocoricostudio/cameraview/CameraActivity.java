package com.cocoricostudio.cameraview;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

public class CameraActivity extends AppCompatActivity {

    private static final String TAG = CameraActivity.class.getSimpleName();

    private CameraView mPreview;
    private Spinner mSpinnerResolution;

    int numberOfCameras;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hide the window title.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            Log.e(TAG, getString(R.string.camera_permission_needed));
            Toast.makeText(this, R.string.camera_permission_needed, Toast.LENGTH_SHORT).show();
            Intent appSettings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + BuildConfig.APPLICATION_ID));
            startActivity(appSettings);
            finish();
            return;
        }

        // Create a RelativeLayout container that will hold a SurfaceView,
        // and set it as the content of our activity.
        setContentView(R.layout.activity_camera);
        mPreview = findViewById(R.id.preview);
        mSpinnerResolution = findViewById(R.id.previewSizesSpinner);
        ArrayAdapter<Resolution> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, Resolution.values());
        mSpinnerResolution.setAdapter(adapter);
        mSpinnerResolution.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            private int check = 0;

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (++check > 1) {
                    Resolution res = (Resolution) mSpinnerResolution.getSelectedItem();
                    Log.d(TAG, "onItemSelected: " + res);
                    mPreview.setTargetPreviewSize(res.width, res.height);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        // Find the total number of cameras available
        numberOfCameras = Camera.getNumberOfCameras();

        // Find the ID of the default camera
        mPreview.onRestoreEventually(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mPreview.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPreview.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate our menu which can gather user input for switching camera
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.camera_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.switch_cam:
                if (numberOfCameras <= 1) {
                    android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this);
                    builder.setMessage(this.getString(R.string.camera_alert, numberOfCameras))
                            .setNeutralButton(android.R.string.ok, null);
                    AlertDialog alert = builder.create();
                    alert.show();
                    return true;
                }
                mPreview.switchCamera();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        mPreview.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

}
