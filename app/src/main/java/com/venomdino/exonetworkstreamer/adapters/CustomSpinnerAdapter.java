package com.venomdino.exonetworkstreamer.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.venomdino.exonetworkstreamer.R;

public class CustomSpinnerAdapter extends ArrayAdapter<String> {

    private final String[] items;
    private final String placeholder;
    private boolean showPlaceholder = true;

    public CustomSpinnerAdapter(Context context, String[] items, String placeholder) {
        super(context, 0, items);
        this.items = items;
        this.placeholder = placeholder;
    }

    @Override
    public int getCount() {
        return showPlaceholder ? 1 : items.length;
    }

    @Override
    public String getItem(int position) {
        return showPlaceholder ? null : items[position];
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.custom_spinner_item, parent, false);
        }

        TextView textViewItem = convertView.findViewById(R.id.text_view_item);
        textViewItem.setText(showPlaceholder ? placeholder : items[position]);

        return convertView;
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.custom_spinner_item_dropdown, parent, false);
        }

        TextView textViewItem = convertView.findViewById(R.id.text_view_item);
        textViewItem.setText(items[position]);

        return convertView;
    }

    public void setShowPlaceholder(boolean showPlaceholder) {
        this.showPlaceholder = showPlaceholder;
    }
}
