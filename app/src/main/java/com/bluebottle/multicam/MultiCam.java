package com.bluebottle.multicam;

import static android.content.ContentValues.TAG;
import static android.content.Context.CAMERA_SERVICE;
import static androidx.core.content.ContextCompat.getSystemService;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.OutputConfiguration;
import android.hardware.camera2.params.SessionConfiguration;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.Arrays;

public class MultiCam {

    CameraDevice cameraDevice;
    CaptureRequest.Builder captureRequestBuilder;
    CameraCaptureSession cameraCaptureSession;

    Context context;
    CameraManager manager;

    String lid;
    String pid1, pid2;
    SurfaceView s1, s2;

    boolean flash = false;

    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;

    long exposureInNanos;
    int iso;
    float focusDistance;

    public MultiCam(Context context, String lid, String pid1, String pid2, SurfaceView s1, SurfaceView s2) {

        this.context = context;
        this.manager = (CameraManager) context.getSystemService(CAMERA_SERVICE);
        this.lid = lid;
        this.pid1 = pid1;
        this.pid2 = pid2;
        this.s1 = s1;
        this.s2 = s2;

        if (cameraCaptureSession != null)
        {
            cameraCaptureSession.close();
        }

        if (cameraDevice != null)
        {
            cameraDevice.close();
        }

        try {

            startBackgroundThread();

            CameraCharacteristics camera = manager.getCameraCharacteristics(lid);
            Size previewSize = camera.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.PRIVATE)[9]; // Get a suitable preview size


            float deviation = Float.MAX_VALUE;
            float ideal = (float) s1.getWidth() / s2.getHeight();
            Size best = new Size(500, 500);

            String dimension = "";

            for (Size size : camera.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.PRIVATE)) {
                float ratio = (float) size.getWidth() / size.getHeight();

                if (Math.abs(ratio - ideal) < deviation) {
                    deviation = Math.abs(ratio - ideal);
                    best = size;
                }

                System.out.println("width " + size.getWidth() + " height " + size.getHeight());
                dimension += "width " + size.getWidth() + " height " + size.getHeight() + "\n";
            }

            int x = camera.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.PRIVATE).length;
            best = camera.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.PRIVATE)[19];
            s1.getHolder().setFixedSize(best.getWidth(), best.getHeight());
            s1.getHolder().setFixedSize(best.getWidth(), best.getHeight());

            dimension += "chose " + best.getWidth() + " " + best.getHeight() + "\n";

            // showDialog(dimension);
            // Create an ImageReader to handle the preview frames
            // ImageReader imageReader = ImageReader.newInstance(previewSize.getWidth(), previewSize.getWidth(), ImageFormat.PRIVATE, 1);
            // reprocessSurface = imageReader.getSurface();

            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            Log.d(TAG, "Trying to open camera" + lid);
            manager.openCamera(lid, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    Log.d(TAG, "Opened " + lid);

                    // showDialog("Opened id " + lid);

                    cameraDevice = camera;
                    createCameraPreviewSession();
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void createCameraPreviewSession() {
        try {
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);

            captureRequestBuilder.addTarget(s1.getHolder().getSurface());
            captureRequestBuilder.addTarget(s2.getHolder().getSurface());

            OutputConfiguration config1 = new OutputConfiguration(s1.getHolder().getSurface());
            OutputConfiguration config2 = new OutputConfiguration(s2.getHolder().getSurface());

            ArrayList<OutputConfiguration> confs = new ArrayList<>();

            if (pid1.isEmpty() && pid2.isEmpty()) {

            } else {
                config1.setPhysicalCameraId(pid1);
                config2.setPhysicalCameraId(pid2);
            }

            confs.add(config1);
            confs.add(config2);

            cameraDevice.createCaptureSession(new SessionConfiguration(SessionConfiguration.SESSION_REGULAR, confs, context.getMainExecutor(), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    cameraCaptureSession = session;

                    startPreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Toast.makeText(context, "Configuration failed", Toast.LENGTH_SHORT).show();
                }
            }));
        } catch (Exception e) {
            // showDialog(Arrays.toString(e.getStackTrace()));
            e.printStackTrace();
        }
    }

    public void startPreview() {
////        showDialog("Success");
//        if (cameraDevice == null) {
////            showDialog("Null");
//            Log.e(TAG, "updatePreview: cameraDevices[id] is null");
//            return;
//        }


//        captureRequestBuilder2.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//             captureRequestBuilder.set(CaptureRequest.CONTROL_ZOOM_RATIO, 4f);
//             captureRequestBuilder2.set(CaptureRequest.CONTROL_ZOOM_RATIO, 2f);
        }

        // System.out.println(Arrays.toString(captureRequestBuilder.build().getKeys().toArray()));

        try {
            cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    // showDialog("working");

                    exposureInNanos = result.get(CaptureResult.SENSOR_EXPOSURE_TIME);
                    iso = result.get(CaptureResult.SENSOR_SENSITIVITY);
                    focusDistance = result.get(CaptureResult.LENS_FOCUS_DISTANCE);
                }

                @Override
                public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
                    super.onCaptureFailed(session, request, failure);

                    session.close();

                    // handler.postDelayed(r, 1000);

                    createCameraPreviewSession();

//                    if (failure.wasImageCaptured())
//                        showDialog("Image was captured");
//                    else
//                        showDialog("image was not captured");

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                        showDialog("failed " + failure.getReason() + " ON " + failure.getPhysicalCameraId());
                    } else {
//                        showDialog("failed " + failure.getReason());
                    }
                }

                @Override
                public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber) {
                    super.onCaptureStarted(session, request, timestamp, frameNumber);

                    // showDialog("started at " + timestamp);
                }

            }, mBackgroundHandler);
        } catch (Exception e) {
            // showDialog(Arrays.toString(e.getStackTrace()));
            e.printStackTrace();
        }
    }

    public void setAutoMode() {
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON);
        captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_AUTO);
        captureRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CameraMetadata.CONTROL_AWB_MODE_AUTO);
        startPreview();
    }

    public void setExposureISO(long exposureInNanos, int ISO) {
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_OFF);
        captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF);
        captureRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, exposureInNanos);
        captureRequestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, ISO);
        startPreview();
    }

    public void setFocus(float focalDistanceInDioptres) {
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_OFF);
        captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_OFF);
        captureRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, focalDistanceInDioptres);
        startPreview();
    }

    // returns current flash status
    public boolean toggleFlash() {

        flash = !flash;

        if (flash)
        {
            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON);
            captureRequestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);
        }
        else
        {
            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON);
            captureRequestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);
        }

        startPreview();

        return flash;
    }

    public static ArrayList<String> availableCameras(Context context) {
        CameraManager manager = (CameraManager) context.getSystemService(CAMERA_SERVICE);

        ArrayList<String> ids = new ArrayList<>();

        for (int i = 0; i <= 511; ++i) {
            try {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(Integer.toString(i));

                ids.add(Integer.toString(i));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return ids;
    }

    public static ArrayList<String> availablePhysicalIds(Context context, String lid) {
        CameraManager manager = (CameraManager) context.getSystemService(CAMERA_SERVICE);

        ArrayList<String> ids = new ArrayList<>();

        try {

            CameraCharacteristics characteristics = manager.getCameraCharacteristics(lid);

            for (String pid : characteristics.getPhysicalCameraIds()) {
                ids.add(pid);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return ids;
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground1");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    public Range<Long> getExposureRange() throws CameraAccessException {
        return manager.getCameraCharacteristics(lid).get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE);
    }

    public Range<Integer> getIsoRange() throws CameraAccessException {
        return manager.getCameraCharacteristics(lid).get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);
    }

    public float getFocusDistanceMin()  {
        try {
            return manager.getCameraCharacteristics(lid).get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);
        }
        catch (Exception e)
        {
            return 0.0f;
        }
    }

    public long getExposureInNanos()
    {
        return exposureInNanos;
    }

    public int getIso()
    {
        return iso;
    }

    public float getFocusDistance() {
        return focusDistance;
    }
}
