package com.bluebottle.multicam;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Range;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.material.slider.Slider;

public class MainActivity extends AppCompatActivity
{

    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int REQUEST_CODE_OPEN_DIRECTORY = 200;

    private SurfaceView[] previews;

    int lock = 0;

    String lid;
    String pid1, pid2;

    MultiCam multiCam;

    Runnable runnable;

    boolean isSliderVisible = false;

    Uri savedDirectory;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getSupportActionBar() != null)
        {
            getSupportActionBar().hide();
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        lid = MultiCam.availableCameras(this).get(0);
        pid1 = "";
        pid2 = "";

        initializePreviews();

        if (SettingsActivity.getExportPath(this) == null)
        {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            startActivityForResult(intent, REQUEST_CODE_OPEN_DIRECTORY);
        }
        else
        {
            savedDirectory = Uri.parse(SettingsActivity.getExportPath(this));
        }

        System.out.println("Saved dir" + savedDirectory);

        // Request camera permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
        }

        openCamera();
    }

    private void initializePreviews()
    {
        previews = new SurfaceView[2];

        previews[0] = findViewById(R.id.preview1);
        SurfaceHolder holder1 = previews[0].getHolder();
        previews[1] = findViewById(R.id.preview2);
        SurfaceHolder holder2 = previews[1].getHolder();

        holder1.addCallback(new SurfaceHolder.Callback()
        {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder)
            {

                ++lock;

                if (lock == 2)
                {
                    lock = 0;
                    openCamera();
                }
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height)
            {
            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder)
            {
                lock = 0;
                System.out.println("surface destroyed");
                stopCamera();
            }

        });

        holder2.addCallback(new SurfaceHolder.Callback()
        {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder)
            {

                ++lock;
                if (lock == 2)
                {
                    lock = 0;
                    openCamera();
                }
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height)
            {
            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder)
            {
                lock = 0;
                System.out.println("surface destroyed");

                stopCamera();
            }

        });
    }

    private void stopCamera()
    {
        try
        {
            multiCam.stopPreview();
            multiCam.stopCamera();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void openCamera()
    {
        try
        {
            if (multiCam != null)
            {
                multiCam.stopPreview();
                multiCam.stopCamera();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        multiCam = new MultiCam(this, lid, pid1, pid2, previews[0], previews[1]);
        multiCam.savedDirectory = savedDirectory;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_OPEN_DIRECTORY && resultCode == Activity.RESULT_OK)
        {
            Uri directoryUri = data.getData();
            takePersistableUriPermission(directoryUri);
            if (directoryUri != null)
            {
                SettingsActivity.setExportPath(this, directoryUri);
                savedDirectory = directoryUri;
            }
        }
    }

    private void takePersistableUriPermission(Uri uri)
    {
        getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQUEST_CODE)
        {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                // openCamera(0, 0);
                // openCamera(1, 1);
            }
            else
            {
                Toast.makeText(this, "Camera permission is needed to use the camera", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showDialog(String message)
    {
        // Create a TextView to display the long message
        TextView messageTextView = new TextView(this);

        messageTextView.setText(message);

        // Set the TextView properties to make it scrollable
        messageTextView.setPadding(40, 40, 40, 40); // Optional padding
        messageTextView.setTextSize(16); // Optional text size

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Long Message Dialog")
                .setView(messageTextView) // Set the custom TextView
                .setPositiveButton("OK", new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        // User clicked OK button
                    }
                });

        // Create and show the dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void selectCamera(View view)
    {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.camera_select_dialog);

        // Initialize views from the dialog layout
        Button submitButton = dialog.findViewById(R.id.done_button);
        AutoCompleteTextView lid_box = dialog.findViewById(R.id.lid_box);
        AutoCompleteTextView pid_one_box = dialog.findViewById(R.id.pid_one_box);
        AutoCompleteTextView pid_two_box = dialog.findViewById(R.id.pid_two_box);

        ArrayAdapter<String> adapter_lid = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, MultiCam.availableCameras(this));
        lid_box.setAdapter(adapter_lid);

        lid_box.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                ArrayAdapter<String> adapter_pid = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_dropdown_item_1line, MultiCam.availablePhysicalIds(MainActivity.this, lid_box.getText().toString()));
                System.out.println(lid_box.getText().toString());
                pid_one_box.setText("", false);
                pid_two_box.setText("", false);
                pid_one_box.setAdapter(adapter_pid);
                pid_two_box.setAdapter(adapter_pid);
            }
        });

        if (multiCam != null)
        {
            lid_box.setText(multiCam.lid, false);
        }

        ArrayAdapter<String> adapter_pid = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, MultiCam.availablePhysicalIds(this, lid_box.getText().toString()));
        pid_one_box.setAdapter(adapter_pid);
        pid_two_box.setAdapter(adapter_pid);

        if (multiCam != null)
        {
            pid_one_box.setText(multiCam.pid1, false);
            pid_two_box.setText(multiCam.pid2, false);
        }

        submitButton.setOnClickListener(mview -> {
            lid = lid_box.getText().toString();
            pid1 = pid_one_box.getText().toString();
            pid2 = pid_two_box.getText().toString();

            openCamera();

            dialog.dismiss();
        });

        dialog.show();

        Window window = dialog.getWindow();
        window.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    public void toggleFlash(View view)
    {
        boolean flash = multiCam.toggleFlash();

        ImageButton flashButton = findViewById(R.id.flash_button);

        if (flash)
        {
            flashButton.setImageResource(R.drawable.baseline_flash_on_40);
        }
        else
        {
            flashButton.setImageResource(R.drawable.baseline_flash_off_40);
        }
    }

    public void setExposure(View view)
    {
        if (isSliderVisible)
        {
            return;
        }

        LinearLayout sliderLayout = findViewById(R.id.slider_layout);

        fadeInAnimation(sliderLayout);

        Slider slider = findViewById(R.id.slider);


        Range<Long> exposureRange = new Range<>(0L, 1000L);

        try
        {
            exposureRange = multiCam.getExposureRange();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        try
        {

            slider.setValueFrom(exposureRange.getLower() / 100000);
            slider.setValueTo(exposureRange.getUpper() / 100000);
            slider.setValue(multiCam.getExposureInNanos() / 100000);
        }
        catch (Exception e)
        {
            showDialog("" + exposureRange.getLower() / 100000 + " " + exposureRange.getUpper() / 100000 + " " + multiCam.getExposureInNanos() / 100000);
        }

        Handler handler = new Handler();

        Slider.OnSliderTouchListener touchListener = new Slider.OnSliderTouchListener()
        {
            @Override
            public void onStartTrackingTouch(Slider slider)
            {
                // Cancel any existing callbacks
                handler.removeCallbacks(runnable);
            }

            @Override
            public void onStopTrackingTouch(Slider slider)
            {
                handler.postDelayed(runnable, 700);
            }
        };

        Slider.OnChangeListener changeListener = new Slider.OnChangeListener()
        {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser)
            {
                handler.removeCallbacks(runnable);
                multiCam.setExposureISO((long) value * 100000, multiCam.getIso());
                System.out.println(multiCam.getIso());
            }
        };

        runnable = new Runnable()
        {
            @Override
            public void run()
            {
                long value = (long) slider.getValue();
                System.out.println("Value: " + value);
                fadeOutAnimation(sliderLayout);
                slider.removeOnChangeListener(changeListener);
                slider.removeOnSliderTouchListener(touchListener);

                multiCam.setExposureISO(value * 100000, multiCam.getIso());
            }
        };

        handler.postDelayed(runnable, 1000);


        slider.addOnChangeListener(changeListener);


        slider.addOnSliderTouchListener(touchListener);

    }

    public void setIso(View view)
    {
        if (isSliderVisible)
        {
            return;
        }

        LinearLayout sliderLayout = findViewById(R.id.slider_layout);

        fadeInAnimation(sliderLayout);

        Slider slider = findViewById(R.id.slider);


        Range<Integer> isoRange = new Range<>(0, 2000);

        try
        {
            isoRange = multiCam.getIsoRange();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        try
        {
            slider.setValueFrom(isoRange.getLower());
            slider.setValueTo(isoRange.getUpper());
            slider.setValue(multiCam.getIso());
        }
        catch (Exception e)
        {
            slider.setValueFrom(0.0f);
            slider.setValueTo(multiCam.getIso() + 3000);
            slider.setValue(multiCam.getIso());
        }

        Handler handler = new Handler();

        Slider.OnChangeListener onChangeListener = new Slider.OnChangeListener()
        {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser)
            {
                handler.removeCallbacks(runnable);
                System.out.println(multiCam.getExposureInNanos());
                multiCam.setExposureISO(multiCam.getExposureInNanos(), (int) value);
            }
        };

        Slider.OnSliderTouchListener onSliderTouchListener = new Slider.OnSliderTouchListener()
        {
            @Override
            public void onStartTrackingTouch(Slider slider)
            {
                // Cancel any existing callbacks
                handler.removeCallbacks(runnable);
            }

            @Override
            public void onStopTrackingTouch(Slider slider)
            {
                handler.postDelayed(runnable, 700);
            }
        };


        runnable = new Runnable()
        {
            @Override
            public void run()
            {
                int value = (int) slider.getValue();
                System.out.println("Value: " + value);
                fadeOutAnimation(sliderLayout);
                slider.removeOnSliderTouchListener(onSliderTouchListener);
                slider.removeOnChangeListener(onChangeListener);

                multiCam.setExposureISO(multiCam.getExposureInNanos(), value);
            }
        };

        handler.postDelayed(runnable, 1000);

        slider.addOnChangeListener(onChangeListener);

        slider.addOnSliderTouchListener(onSliderTouchListener);

    }

    public void setFocusDistance(View view)
    {
        if (isSliderVisible)
        {
            return;
        }

        LinearLayout sliderLayout = findViewById(R.id.slider_layout);

        fadeInAnimation(sliderLayout);

        Slider slider = findViewById(R.id.slider);

        float focusDistance = 0.0f;

        try
        {
            focusDistance = multiCam.getFocusDistanceMin();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        slider.setValueFrom(0.0f);
        slider.setValueTo(focusDistance);

        try
        {
            slider.setValue(multiCam.getFocusDistance());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        Handler handler = new Handler();

        Slider.OnChangeListener onChangeListener = new Slider.OnChangeListener()
        {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser)
            {
                handler.removeCallbacks(runnable);
                multiCam.setFocus(value);
            }
        };

        Slider.OnSliderTouchListener onSliderTouchListener = new Slider.OnSliderTouchListener()
        {
            @Override
            public void onStartTrackingTouch(Slider slider)
            {
                // Cancel any existing callbacks
                handler.removeCallbacks(runnable);
            }

            @Override
            public void onStopTrackingTouch(Slider slider)
            {
                handler.postDelayed(runnable, 700);
            }
        };

        runnable = new Runnable()
        {
            @Override
            public void run()
            {
                float value = (float) slider.getValue();
                System.out.println("Value: " + value);
                fadeOutAnimation(sliderLayout);

                slider.removeOnSliderTouchListener(onSliderTouchListener);
                slider.removeOnChangeListener(onChangeListener);

                multiCam.setFocus(value);
            }
        };

        handler.postDelayed(runnable, 1000);


        slider.addOnChangeListener(onChangeListener);

        slider.addOnSliderTouchListener(onSliderTouchListener);

    }

    public void record(View view)
    {
        if (multiCam.isRecording)
        {
            multiCam.stopRecording();
        }
        else
        {
            multiCam.startRecording();
        }
    }

    private void hideSystemUI()
    {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    void fadeInAnimation(View view)
    {
        view.setAlpha(0f);
        view.setVisibility(View.VISIBLE);

        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);

        fadeIn.start();

        isSliderVisible = true;
    }

    void fadeOutAnimation(View view)
    {
        view.setAlpha(1f);
        view.setVisibility(View.VISIBLE);

        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f);

        fadeOut.addListener(new AnimatorListenerAdapter()
        {
            @Override
            public void onAnimationEnd(Animator animation)
            {
                super.onAnimationEnd(animation);

                view.setVisibility(View.INVISIBLE);

                isSliderVisible = false;
            }
        });

        fadeOut.start();
    }

    public void openPlaybackActivity(View view)
    {
        Intent intent = new Intent(this, PlaybackActivity.class);
        startActivity(intent);
    }

    public void openVideosActivity(View view)
    {
        Intent intent = new Intent(this, VideosActivity.class);
        startActivity(intent);
    }

    public void openSettingsActivity(View view)
    {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }
}
