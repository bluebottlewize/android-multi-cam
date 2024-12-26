package com.bluebottle.multicam;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.RecyclerView;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.List;

public class VideosAdapter extends RecyclerView.Adapter<VideosAdapter.ViewHolder>
{

    private List<VideoItem> itemList;
    Context context;

    public VideosAdapter(List<VideoItem> itemList, Context context)
    {
        this.itemList = itemList;
        this.context = context;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder
    {
        TextView nameBox, lidBox, cameraOneBox, cameraTwoBox;
        ImageButton optionButton;

        public ViewHolder(@NonNull View itemView)
        {
            super(itemView);
            nameBox = itemView.findViewById(R.id.name_box);
            lidBox = itemView.findViewById(R.id.lid_box);
            cameraOneBox = itemView.findViewById(R.id.camera_one_box);
            cameraTwoBox = itemView.findViewById(R.id.camera_two_box);
            optionButton = itemView.findViewById(R.id.video_options_button);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.videos_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position)
    {
        VideoItem item = itemList.get(position);
        holder.nameBox.setText(item.name);
        holder.lidBox.setText(item.lid);
        holder.cameraOneBox.setText(item.pid1);
        holder.cameraTwoBox.setText(item.pid2);

        holder.itemView.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(context, PlaybackActivity.class);
                intent.putExtra("video_id", item.name);
                context.startActivity(intent);
            }
        });

        holder.optionButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                PopupMenu popupMenu = new PopupMenu(context, view);
                popupMenu.getMenuInflater().inflate(R.menu.video_options, popupMenu.getMenu());

                // Set a listener for menu item clicks
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener()
                {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem)
                    {
                        if (menuItem.getItemId() == R.id.delete_video_option)
                        {
                            if (position != RecyclerView.NO_POSITION)
                            {
                                try
                                {
                                    File dir = new File(context.getFilesDir(), "videos/" + item.name);
                                    FileUtils.deleteDirectory(dir);
                                    Toast.makeText(context, "Deleted Successfully", Toast.LENGTH_SHORT).show();
                                }
                                catch (Exception e)
                                {
                                    e.printStackTrace();
                                }

                                itemList.remove(position);
                                notifyItemRemoved(position);
                                notifyItemRangeChanged(position, itemList.size());
                            }

                            return true;
                        }
                        else if (menuItem.getItemId() == R.id.export_video_option)
                        {
                            String exportPath = SettingsActivity.getExportPath(context);
                            DocumentFile savedFolder = DocumentFile.fromTreeUri(context, Uri.parse(exportPath));

                            FileInputStream inputStream = null;
                            OutputStream outputStream = null;

                            try
                            {
                                DocumentFile destFolder = savedFolder.createDirectory(item.name);

                                File srcFolder = new File(context.getFilesDir(), "videos/" + item.name);

                                for (File file : srcFolder.listFiles())
                                {
                                    DocumentFile outputFile = destFolder.createFile("video/mp4", file.getName());

                                    try
                                    {
                                        outputStream = context.getContentResolver().openOutputStream(outputFile.getUri());

                                        FileUtils.copyFile(file, outputStream);

                                        inputStream.close();
                                        outputStream.close();
                                    }
                                    catch (Exception e)
                                    {
                                        e.printStackTrace();
                                    }
                                }

                                inputStream.close();
                                outputStream.close();
                            }
                            catch (Exception e)
                            {
                                e.printStackTrace();
                            }

                            return true;
                        }
                        else
                        {
                            // Default case
                            return false;
                        }
                    }
                });

                // Show the menu
                popupMenu.show();

            }
        });
    }

    @Override
    public int getItemCount()
    {
        return itemList.size();
    }
}