package com.example.bluetoothcontroller.Dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;


import com.example.bluetoothcontroller.R;
import com.github.lany192.blurdialog.BlurDialogFragment;

import me.jfenn.colorpickerdialog.dialogs.ColorPickerDialog;
import me.jfenn.colorpickerdialog.views.picker.ImagePickerView;

public class BluetoothBlurDialog extends BlurDialogFragment {

    static AlertDialog alertDialog;

    public static BluetoothBlurDialog newInstance(AlertDialog dialog) {
        alertDialog = dialog;
        return new BluetoothBlurDialog();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return alertDialog;
    }

    @Override
    protected boolean isDimmingEnable() {
        return true;
    }

    @Override
    protected boolean isActionBarBlurred() {
        return true;
    }

    @Override
    protected float getDownScaleFactor() {
        return 4;
    }

    @Override
    protected int getBlurRadius() {
        return 2;
    }
}

