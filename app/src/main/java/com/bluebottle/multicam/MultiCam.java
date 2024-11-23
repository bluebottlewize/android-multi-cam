package com.bluebottle.multicam;

import static android.content.ContentValues.TAG;
import static android.content.Context.CAMERA_SERVICE;

import android.Manifest;
import android.app.Activity;
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
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.view.SurfaceView;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public class MultiCam {

    MediaRecorder recorder_1;
    MediaRecorder recorder_2;

    static CameraDevice cameraDevice;
    CaptureRequest.Builder captureRequestBuilder;
    static CameraCaptureSession cameraCaptureSession;

    Context context;
    CameraManager manager;

    String lid;
    String pid1, pid2;
    SurfaceView s1, s2;

    Uri savedDirectory;

    boolean flash = false;
    boolean both = false;

    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;

    long exposureInNanos;
    int iso;
    float focusDistance;

    Size size;

    boolean isRecording = false;

    public MultiCam(Context context, String lid, String pid1, String pid2, SurfaceView s1, SurfaceView s2) {

        this.context = context;
        this.manager = (CameraManager) context.getSystemService(CAMERA_SERVICE);
        this.lid = lid;
        this.pid1 = pid1;
        this.pid2 = pid2;
        this.s1 = s1;
        this.s2 = s2;

        both = !(pid1.isEmpty() && pid2.isEmpty());

        if (cameraCaptureSession != null) {
            cameraCaptureSession.close();
        }

        if (cameraDevice != null) {
            cameraDevice.close();
        }

        try {

            startBackgroundThread();

            CameraCharacteristics camera = manager.getCameraCharacteristics(lid);
            Log.d(TAG, "Camera " + lid + " capabilities: " + Arrays.toString(camera.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)));
            Log.d(TAG, "Is logical multi-camera: " + camera.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES));

            for (int i : camera.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)) {
                if (i == CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA) {
                    System.out.println("Logical camera confirmed");
                }
            }


            CameraCharacteristics chars = manager.getCameraCharacteristics(lid);
            int[] capabilities = chars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
            for (int capability : capabilities) {
                Log.d(TAG, "Camera capability: " + capability);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                System.out.println(manager.getConcurrentCameraIds());
            }

//        (CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA));

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
            best = camera.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.PRIVATE)[3];
            size = best;
            s1.getHolder().setFixedSize(best.getWidth(), best.getHeight());
            s2.getHolder().setFixedSize(best.getWidth(), best.getHeight());

            dimension += "chose " + best.getWidth() + " " + best.getHeight() + "\n";

            // System.out.println(camera.get(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA));

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
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

//            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
//            captureRequestBuilder.addTarget(s1.getHolder().getSurface());
//            captureRequestBuilder.addTarget(s2.getHolder().getSurface());

            captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

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

                    // session.close();

                    // handler.postDelayed(r, 1000);

                    // createCameraPreviewSession;

                    startPreview();

//                    if (failure.wasImageCaptured())
//                        showDialog("Image was captured");
//                    else
//                        showDialog("image was not captured");

                    System.out.println(failure.getReason());
                    System.out.println("frame " + failure.getFrameNumber());
//                    Log.e(TAG, "Session configuration: " + session.getDeviceStateCallback().toString()); }


                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        System.out.println("failed " + failure.getReason() + " ON " + failure.getPhysicalCameraId());
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

    public void stopPreview() {
        cameraCaptureSession.close();
    }

    public void stopCamera() {
        cameraDevice.close();
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

        if (flash) {
            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON);
            captureRequestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);
        } else {
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
        } catch (Exception e) {
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

    public float getFocusDistanceMin() {
        try {
            return manager.getCameraCharacteristics(lid).get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);
        } catch (Exception e) {
            return 0.0f;
        }
    }

    public long getExposureInNanos() {
        return exposureInNanos;
    }

    public int getIso() {
        return iso;
    }

    public float getFocusDistance() {
        return focusDistance;
    }

    private void setUpMediaRecorder() throws IOException {

        System.out.println(savedDirectory);
        DocumentFile savedFolder = DocumentFile.fromTreeUri(context, savedDirectory);
        String filename = getNextVideoName();

        DocumentFile outputFile_1 = savedFolder.createFile("video/mp4", filename + "_" + lid + "_" + pid1 + ".mp4");
        ParcelFileDescriptor pfd_1 = context.getContentResolver().openFileDescriptor(outputFile_1.getUri(), "w");
        recorder_1 = new MediaRecorder();

        Toast.makeText(context, "Recording Started", Toast.LENGTH_SHORT);

        recorder_1.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder_1.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        recorder_1.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder_1.setOutputFile(pfd_1.getFileDescriptor());
        recorder_1.setVideoEncodingBitRate(10000000);
        recorder_1.setVideoFrameRate(30);
        recorder_1.setVideoSize(size.getWidth(), size.getHeight());
        recorder_1.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        recorder_1.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        Toast.makeText(context, "Recording Started", Toast.LENGTH_SHORT);
        recorder_1.prepare();

        Toast.makeText(context, "Recording Started", Toast.LENGTH_SHORT);
        System.out.println("mediarecorder 1 ready");

        if (both) {
            DocumentFile outputFile_2 = savedFolder.createFile("video/mp4", filename + "_" + lid + "_" + pid2 + ".mp4");
            ParcelFileDescriptor pfd_2 = context.getContentResolver().openFileDescriptor(outputFile_2.getUri(), "w");

            recorder_2 = new MediaRecorder();

            recorder_2.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder_2.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            recorder_2.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder_2.setOutputFile(pfd_2.getFileDescriptor());
            recorder_2.setVideoEncodingBitRate(10000000);
            recorder_2.setVideoFrameRate(30);
            recorder_2.setVideoSize(size.getWidth(), size.getHeight());
            recorder_2.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            recorder_2.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder_2.prepare();

            System.out.println("mediarecorder 2 ready");
        }

        Toast.makeText(context, "Recording Started", Toast.LENGTH_SHORT);
    }

    public void startRecording() {
        try {

            System.out.println("recording starting");

            stopPreview();

            setUpMediaRecorder();

            System.out.println("mediarecorder ready");

            Toast.makeText(context, "Recording Started", Toast.LENGTH_SHORT);

            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);

            captureRequestBuilder.addTarget(s1.getHolder().getSurface());
            captureRequestBuilder.addTarget(s2.getHolder().getSurface());
            captureRequestBuilder.addTarget(recorder_1.getSurface());

            OutputConfiguration config1 = new OutputConfiguration(s1.getHolder().getSurface());
            OutputConfiguration config2 = new OutputConfiguration(s2.getHolder().getSurface());
            OutputConfiguration config3 = new OutputConfiguration(recorder_1.getSurface());

            ArrayList<OutputConfiguration> confs = new ArrayList<>();

            if (!both) {
                System.out.println("Capturing only logical camera");

                confs.add(config1);
                confs.add(config2);
                confs.add(config3);
            } else {
                config1.setPhysicalCameraId(pid1);
                config2.setPhysicalCameraId(pid2);
                config3.setPhysicalCameraId(pid1);


                confs.add(config1);
                confs.add(config2);
                confs.add(config3);

                captureRequestBuilder.addTarget(recorder_2.getSurface());
                OutputConfiguration config4 = new OutputConfiguration(recorder_2.getSurface());
                config4.setPhysicalCameraId(pid2);
                confs.add(config4);
            }


            Toast.makeText(context, "Recording Started", Toast.LENGTH_SHORT);

            cameraDevice.createCaptureSession(new SessionConfiguration(SessionConfiguration.SESSION_REGULAR, confs, context.getMainExecutor(), new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    cameraCaptureSession = session;
                    startPreview();
                    isRecording = true;

                    System.out.println("recording started");

                    Toast.makeText(context, "Recording Started", Toast.LENGTH_SHORT);

                    recorder_1.start();
                    if (both) {
                        recorder_2.start();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    System.out.println("Failed configuration");
                    Toast.makeText(context, "Failed", Toast.LENGTH_SHORT).show();
                }
            }));

            ImageButton record_button = ((Activity) context).findViewById(R.id.record_button);

            record_button.setImageResource(R.drawable.outline_stop_circle_90);

        } catch (CameraAccessException | IOException e) {
            e.printStackTrace();
        }
    }

    public static String getNextVideoName() {
        LocalDateTime currentDate = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").withLocale(Locale.getDefault()).withZone(ZoneId.systemDefault());
        String formattedDate = currentDate.format(formatter);

        String name = "VID_" + formattedDate;

        return name;
    }

    public void stopRecording() {
        isRecording = false;

        try {
            recorder_1.stop();
            recorder_1.reset();
            recorder_1.release();
            recorder_1 = null;

            if (both) {
                recorder_2.stop();
                recorder_2.reset();
                recorder_2.release();
                recorder_2 = null;
            }

            Toast.makeText(context, "Video saved", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(context, "Error in saving video", Toast.LENGTH_SHORT).show();
        }

        createCameraPreviewSession();

        ImageButton record_button = ((Activity) context).findViewById(R.id.record_button);

        record_button.setImageResource(R.drawable.baseline_fiber_manual_record_90);
    }
}
