package iloveopensource.com.torch;

/**
 * influenced by @see <a href="https://github.com/EddyVerbruggen/Flashlight-PhoneGap-Plugin">Flashlight-PhoneGap-Plugin</a>
 */


import android.app.Activity;
import android.content.pm.FeatureInfo;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.content.pm.PackageManager;

public class FlashLightActivity extends Activity {

    private static final String ACTION_AVAILABLE = "available";
    private static final String ACTION_SWITCH_ON = "switchOn";
    private static final String ACTION_SWITCH_OFF = "switchOff";


    private static final String LIGHT_IS_OFF="lightState";
    private static final String RELEASING="releasing";


    private static Boolean capable;
    private boolean releasing;
    private boolean lightIsOff = true;
    private static Camera mCamera;


    private Button button;


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.flash_light_activity);
        button = (Button) findViewById(R.id.buttonFlashlight);

        //for rotation
        if(savedInstanceState != null) {
            lightIsOff = savedInstanceState.getBoolean(LIGHT_IS_OFF);
            if(!lightIsOff){
                button.setText(R.string.disable);
            }
            releasing = savedInstanceState.getBoolean(RELEASING);
        }

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (lightIsOff & execute(ACTION_AVAILABLE)) {
                    lightIsOff = !execute(ACTION_SWITCH_ON);
                    button.setText(R.string.disable);

                } else if (!lightIsOff) {
                    lightIsOff = execute(ACTION_SWITCH_OFF);
                    button.setText(R.string.enable);

                }
            }
        });
    }
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        //on rotation,save the light state and if the camera is released
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBoolean(LIGHT_IS_OFF,lightIsOff);
        savedInstanceState.putBoolean(RELEASING,releasing);
    }

    public boolean execute(String action) {
        Log.d("Flashlight", "Plugin Called: " + action);
        try {
            if (action.equals(ACTION_SWITCH_ON)) {
                // When switching on immediately after checking for isAvailable,
                // the release method may still be running, so wait a bit.
                while (releasing) {
                    Thread.sleep(10);
                }
                mCamera = Camera.open();
                if (Build.VERSION.SDK_INT >= 11) { // honeycomb
                    // required for (at least) the Nexus 5
                    mCamera.setPreviewTexture(new SurfaceTexture(0));
                }
                toggleTorch(true);
                return true;
            } else if (action.equals(ACTION_SWITCH_OFF)) {
                toggleTorch(false);
                releaseCamera();
                return true;
            } else if (action.equals(ACTION_AVAILABLE)) {
                if (capable == null) {
                    mCamera = Camera.open();
                    capable = isCapable();
                    releaseCamera();
                }
                return (capable ? true : false);
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isCapable() {
        final PackageManager packageManager = this.getPackageManager();
        for (final FeatureInfo feature : packageManager.getSystemAvailableFeatures()) {
            if (PackageManager.FEATURE_CAMERA_FLASH.equalsIgnoreCase(feature.name)) {
                return true;
            }
        }
        return false;
    }

    private void toggleTorch(boolean switchOn) {
        final Camera.Parameters mParameters = mCamera.getParameters();
        if (isCapable()) {
            mParameters.setFlashMode(switchOn ? Camera.Parameters.FLASH_MODE_TORCH : Camera.Parameters.FLASH_MODE_OFF);
            mCamera.setParameters(mParameters);
            mCamera.startPreview();
        } else {
            //display short message to the user that flash doesn't work on his phone
            Toast.makeText(this, R.string.notCompatible,Toast.LENGTH_SHORT);
        }
    }

    private void releaseCamera() {
        releasing = true;
        // we need to release the camera, so other apps can use it
        new Thread(new Runnable() {
            public void run() {
                mCamera.setPreviewCallback(null);
                mCamera.stopPreview();
                mCamera.release();
                releasing = false;
            }
        }).start();
    }


}
