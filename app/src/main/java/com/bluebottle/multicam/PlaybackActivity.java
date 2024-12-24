package com.bluebottle.multicam;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

public class PlaybackActivity extends AppCompatActivity
{

    private VideoView videoView1, videoView2;
    private SeekBar seekBar;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playback);

        videoView1 = findViewById(R.id.videoView1);
        videoView2 = findViewById(R.id.videoView2);
        Button playButton = findViewById(R.id.playButton);
        Button pauseButton = findViewById(R.id.pauseButton);
        seekBar = findViewById(R.id.seekBar);

        // Set video URIs
        Uri videoUri1 = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.video1);
        Uri videoUri2 = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.video2);

        videoView1.setVideoURI(videoUri1);
        videoView2.setVideoURI(videoUri2);

        // Sync start of both videos
        videoView1.setOnPreparedListener(mp -> {
            videoView2.start();
            videoView1.start();
            syncSeekBar();
        });

        // Play button
        playButton.setOnClickListener(v -> {
            videoView1.start();
            videoView2.start();
        });

        // Pause button
        pauseButton.setOnClickListener(v -> {
            videoView1.pause();
            videoView2.pause();
        });

        // SeekBar listener
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                if (fromUser)
                {
                    videoView1.seekTo(progress);
                    videoView2.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar)
            {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {
            }
        });
    }

    private void syncSeekBar()
    {
        seekBar.setMax(videoView1.getDuration());

        Runnable updateSeekBar = new Runnable()
        {
            @Override
            public void run()
            {
                if (videoView1.isPlaying() || videoView2.isPlaying())
                {
                    seekBar.setProgress(videoView1.getCurrentPosition());
                }
                handler.postDelayed(this, 500);
            }
        };
        handler.post(updateSeekBar);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null); // Clean up handler
    }

}