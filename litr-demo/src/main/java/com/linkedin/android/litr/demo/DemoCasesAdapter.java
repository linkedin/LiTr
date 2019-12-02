/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.demo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

class DemoCasesAdapter extends ArrayAdapter<DemoCase> {

    DemoCasesAdapter(@NonNull Context context, int resource) {
        super(context, resource);

        addAll(DemoCase.values());
    }

    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_list_item_1, null);
        }

        TextView textView = view.findViewById(android.R.id.text1);
        textView.setText(getItem(position).displayName);

        return textView;
    }
}
