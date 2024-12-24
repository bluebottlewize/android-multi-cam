package com.bluebottle.multicam;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.slider.Slider;

import java.io.File;

public class PlaybackActivity extends AppCompatActivity
{

    private VideoView videoView1, videoView2;
    private Slider seekBar;
    private ImageButton playPauseButton;

    private Handler handler = new Handler();

    boolean isPlaying = false;
    boolean isUiVisible = true;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playback);

        Intent intent = getIntent();
        String video_id = intent.getStringExtra("video_id");

        videoView1 = findViewById(R.id.videoView1);
        videoView2 = findViewById(R.id.videoView2);
        playPauseButton = findViewById(R.id.play_pause_button);
        seekBar = findViewById(R.id.playback_seekbar);

        String path_one, path_two;
        File video = new File(this.getFilesDir(), "videos/" + video_id);
        path_one = video.listFiles()[0].getPath();
        videoView1.setVideoPath(path_one);

        if (video.listFiles().length == 2)
        {
            path_two = video.listFiles()[1].getPath();
            videoView2.setVideoPath(path_two);
        }
        else
        {
            videoView2.setVideoPath(path_one);
        }

        // Sync start of both videos
        videoView1.setOnPreparedListener(mp -> {
            videoView1.pause();
            videoView2.pause();
            syncSeekBar();
        });

        playPauseButton.setOnClickListener(v -> {
            if (isPlaying)
            {
                videoView1.pause();
                videoView2.pause();
            }
            else
            {
                videoView1.start();
                videoView2.start();
            }

            isPlaying = !isPlaying;
        });

        // SeekBar listener
        seekBar.addOnChangeListener(new Slider.OnChangeListener()
        {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser)
            {
                if (fromUser)
                {
                    System.out.println(value);
                    videoView1.seekTo((int) value);
                    videoView2.seekTo((int) value);
                }
            }
        });

    }

    private void syncSeekBar()
    {
        seekBar.setValueFrom(0f);
        seekBar.setValueTo(videoView1.getDuration());

        Runnable updateSeekBar = new Runnable()
        {
            @Override
            public void run()
            {
                if (videoView1.isPlaying() || videoView2.isPlaying())
                {
                    seekBar.setValue(videoView1.getCurrentPosition());
                }
                handler.postDelayed(this, 100);
            }
        };
        handler.post(updateSeekBar);
    }

    void fadeInAnimation(View view)
    {
        view.setAlpha(0f);
        view.setVisibility(View.VISIBLE);

        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);

        fadeIn.start();

        isUiVisible = true;
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

                isUiVisible = false;
            }
        });

        fadeOut.start();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null); // Clean up handler
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        if (event.getAction() == MotionEvent.ACTION_UP)
        {
            if (!isUiVisible)
            {
                fadeInAnimation(seekBar);
                fadeInAnimation(playPauseButton);
            }
            else
            {
                fadeOutAnimation(seekBar);
                fadeOutAnimation(playPauseButton);
            }
        }
        return super.onTouchEvent(event);
    }
}