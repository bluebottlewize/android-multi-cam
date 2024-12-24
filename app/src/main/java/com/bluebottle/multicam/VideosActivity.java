package com.bluebottle.multicam;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class VideosActivity extends AppCompatActivity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_videos);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeVideosList();
    }

    public void initializeVideosList()
    {
        RecyclerView recyclerView = findViewById(R.id.videos_list);

        // Initialize the data
        List<VideoItem> itemList = new ArrayList<>();

        File videos = new File(getApplicationContext().getFilesDir().getPath(), "videos");

        if (!videos.exists())
        {
            return;
        }

        for (File file : videos.listFiles())
        {
            if (file.isDirectory())
            {
                if (file.listFiles().length == 1)
                {
                    VideoItem item = new VideoItem(file.getName(), file.listFiles()[0].getName(), "");
                    itemList.add(item);
                }
                else if (file.listFiles().length == 2)
                {
                    VideoItem item = new VideoItem(file.getName(), file.listFiles()[0].getName(), file.listFiles()[1].getName());
                    itemList.add(item);
                }
            }
        }

        System.out.println(itemList.size());

        // Set up RecyclerView
        VideosAdapter adapter = new VideosAdapter(itemList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }
}