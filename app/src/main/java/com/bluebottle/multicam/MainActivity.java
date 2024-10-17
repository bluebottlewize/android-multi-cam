package com.bluebottle.multicam;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.OutputConfiguration;
import android.hardware.camera2.params.SessionConfiguration;
import android.media.ImageReader;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "CameraActivity";
    private static final int CAMERA_REQUEST_CODE = 100;

    private SurfaceView[] previews;
    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSession;
    private CaptureRequest.Builder captureRequestBuilder;

    EditText logical_id_box;
    EditText physical_id_1_box;
    EditText physical_id_2_box;

    int lock = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previews = new SurfaceView[2];

        previews[0] = findViewById(R.id.preview1);
        SurfaceHolder holder1 = previews[0].getHolder();
        previews[1] = findViewById(R.id.preview2);
        SurfaceHolder holder2 = previews[1].getHolder();

        logical_id_box = findViewById(R.id.logical_id);
        physical_id_1_box = findViewById(R.id.physical_id_1);
        physical_id_2_box = findViewById(R.id.physical_id_2);

        holder1.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {

                ++lock;

                if (lock == 2) {
                    // openCamera(0, 0);
                }
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {

            }

        });


        holder2.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {

                ++lock;
                if (lock == 2) {
                    // openCamera(0, 0);
                }
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {

            }

        });


        // Request camera permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
        }
    }

    private void openCamera(String lid, String pid1, String pid2) {
        CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);


        // System.out.println("COncurrent ides " + manager.getConcurrentCameraIds().size());
        try {

            for (String ide : manager.getCameraIdList()) {
                CameraCharacteristics characteristicss = manager.getCameraCharacteristics(ide);
                for (String phyid : characteristicss.getPhysicalCameraIds()) {
                    System.out.print(phyid + " ");
                }
            }


            String cameraId = manager.getCameraIdList()[Integer.parseInt(lid)]; // Get the first camera
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            Size previewSize = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.PRIVATE)[9]; // Get a suitable preview size


            float deviation = Float.MAX_VALUE;
            float ideal = (float) previews[0].getWidth() / previews[0].getHeight();
            Size best = new Size(500, 500);

            for (Size size : characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.PRIVATE)) {
                float ratio = (float) size.getWidth() / size.getHeight();

                if (Math.abs(ratio - ideal) < deviation) {
                    deviation = Math.abs(ratio - ideal);
                    best = size;
                }

                System.out.println("width " + size.getWidth() + " height " + size.getHeight());
            }
            previews[0].getHolder().setFixedSize(best.getWidth(), best.getHeight());
            previews[1].getHolder().setFixedSize(best.getWidth(), best.getHeight());

            // Create an ImageReader to handle the preview frames
            // ImageReader imageReader = ImageReader.newInstance(previewSize.getWidth(), previewSize.getWidth(), ImageFormat.YUV_420_888, 2);

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            Log.d(TAG, "Trying to open camera" + lid);
            manager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    Log.d(TAG, "Opened " + lid);


                    cameraDevice = camera;
                    createCameraPreviewSession(pid1, pid2);
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    camera.close();
                    cameraDevice = null;
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    if (error == ERROR_MAX_CAMERAS_IN_USE) {
                        Log.d(TAG, "cant open camera" + lid);
                    }
                    camera.close();
                    cameraDevice = null;
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void createCameraPreviewSession(String pid1, String pid2) {
        try {
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(previews[0].getHolder().getSurface());
            captureRequestBuilder.addTarget(previews[1].getHolder().getSurface());

            OutputConfiguration config1 = new OutputConfiguration(previews[0].getHolder().getSurface());
            OutputConfiguration config2 = new OutputConfiguration(previews[1].getHolder().getSurface());
            config1.setPhysicalCameraId(pid1);
            config2.setPhysicalCameraId(pid2);

            ArrayList<OutputConfiguration> confs = new ArrayList<>();
            confs.add(config1);
            confs.add(config2);

            cameraDevice.createCaptureSession(
                    new SessionConfiguration(SessionConfiguration.SESSION_REGULAR, confs, this.getMainExecutor(), new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            cameraCaptureSession = session;
                            updatePreview();
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Toast.makeText(MainActivity.this, "Configuration failed", Toast.LENGTH_SHORT).show();
                        }
                    }));
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void updatePreview() {
        if (cameraDevice == null) {
            Log.e(TAG, "updatePreview: cameraDevices[id] is null");
            return;
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // openCamera(0, 0);
                // openCamera(1, 1);
            } else {
                Toast.makeText(this, "Camera permission is needed to use the camera", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void openCameras(View view) {

        try {
            cameraDevice.close();
            cameraDevice.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            String logical_id = logical_id_box.getText().toString();
            String physical_id_1 = physical_id_1_box.getText().toString();
            String physical_id_2 = physical_id_2_box.getText().toString();

            openCamera(logical_id, physical_id_1, physical_id_2);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void detectCameras() throws CameraAccessException {
        CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);

        String result = "";

        for (String lid : manager.getCameraIdList()) {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(lid);
            int direction = characteristics.get(CameraCharacteristics.LENS_FACING);
            if (!characteristics.getPhysicalCameraIds().isEmpty()) {
                result += "Available pids for lid " + lid;
                if (direction == CameraCharacteristics.LENS_FACING_FRONT)
                {
                    result += "(FRONT) ";
                }
                else if (direction == CameraCharacteristics.LENS_FACING_BACK)
                {
                    result += "(BACK) ";
                }
                result += ": ";
                for (String pid : characteristics.getPhysicalCameraIds()) {
                    result += " " + pid;
                }

                result += "\n";
            } else {
                result += "No physical ids for logical id " + lid + "\n";
            }
        }

        System.out.println(result);

        showDialog(result);
    }

    private void showDialog(String message) {
        // Create a TextView to display the long message
        TextView messageTextView = new TextView(this);

        messageTextView.setText(message);

        // Set the TextView properties to make it scrollable
        messageTextView.setPadding(40, 40, 40, 40); // Optional padding
        messageTextView.setTextSize(16); // Optional text size

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Long Message Dialog")
                .setView(messageTextView) // Set the custom TextView
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User clicked OK button
                    }
                });

        // Create and show the dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void detect(View view)
    {
        try {
            detectCameras();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
