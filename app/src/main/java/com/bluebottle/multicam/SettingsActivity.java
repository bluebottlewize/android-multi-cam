package com.bluebottle.multicam;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SettingsActivity extends AppCompatActivity
{
    private static final int REQUEST_CODE_OPEN_DIRECTORY = 200;

    TextView exportPathBox;
    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeUI();
    }

    void initializeUI()
    {
        exportPathBox = findViewById(R.id.export_path_box);
        String exportPath = getExportPath(this);

        if (exportPath != null)
        {
            exportPathBox.setText(exportPath);
        }
    }

    public static String getExportPath(Context context)
    {
        String path;
        try
        {
            SharedPreferences sharedPreferences = context.getSharedPreferences("Settings", MODE_PRIVATE);
            path = sharedPreferences.getString("export_path", null);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }

        return path;
    }

    public static void setExportPath(Context context, Uri uri)
    {
        try
        {
            SharedPreferences sharedPreferences = context.getSharedPreferences("Settings", MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("export_path", uri.toString());
            editor.apply();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void takePersistableUriPermission(Uri uri)
    {
        getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
    }

    public void changeExportPath(View view)
    {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(intent, REQUEST_CODE_OPEN_DIRECTORY);
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
                String exportPath = getExportPath(this);
                exportPathBox.setText(exportPath);
            }
        }
    }
}