package com.bluebottle.multicam;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class VideosAdapter extends RecyclerView.Adapter<VideosAdapter.ViewHolder>
{

    private List<VideoItem> itemList;

    public VideosAdapter(List<VideoItem> itemList)
    {
        this.itemList = itemList;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder
    {
        TextView nameBox, cameraOneBox, cameraTwoBox;

        public ViewHolder(@NonNull View itemView)
        {
            super(itemView);
            nameBox = itemView.findViewById(R.id.name_box);
            cameraOneBox = itemView.findViewById(R.id.camera_one_box);
            cameraTwoBox = itemView.findViewById(R.id.camera_two_box);
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
        holder.cameraOneBox.setText(item.pid1);
        holder.cameraTwoBox.setText(item.pid2);
    }

    @Override
    public int getItemCount()
    {
        return itemList.size();
    }
}