package com.bluebottle.multicam;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialDialogs;

public class CameraSelectDialog extends Dialog implements android.view.View.OnClickListener {


    public CameraSelectDialog(@NonNull Context context) {
        super(context);
    }

    @Nullable
    // @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.camera_select_dialog, container, false);
        this.setTitle("Sample");

        return view;
    }


    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.done_button) {
            dismiss();
        }
        dismiss();
    }
}
