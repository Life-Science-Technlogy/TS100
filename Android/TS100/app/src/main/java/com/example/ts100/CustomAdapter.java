package com.example.ts100;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.ts100.databinding.DeviceListItemBinding;

import java.util.List;

public class CustomAdapter extends ArrayAdapter<String> {

    private final Context context;
    private ButtonClickListener listener;

    public CustomAdapter(Context context, int resource, List<String> items, ButtonClickListener listener) {
        super(context, resource, items);
        this.context = context;
        this.listener = listener;
    }

    public interface ButtonClickListener {
        void onButtonClick(int position);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        DeviceListItemBinding binding;

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            binding = DeviceListItemBinding.inflate(inflater, parent, false);
        } else {
            return convertView;
        }

        binding.textDeviceName.setText(getItem(position));
        binding.buttonDeviceConnect.setOnClickListener(view -> listener.onButtonClick(position));

        return binding.getRoot();
    }

}
