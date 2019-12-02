/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.demo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import androidx.annotation.NonNull;
import androidx.fragment.app.ListFragment;

public class MainFragment extends ListFragment implements AdapterView.OnItemClickListener {
    private static final String KEY_DEMO_CASE = "demoCase";

    private DemoCase demoCase;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ArrayAdapter adapter = new DemoCasesAdapter(getContext(), android.R.layout.simple_list_item_1);
        setListAdapter(adapter);
        getListView().setOnItemClickListener(this);

        if (savedInstanceState != null) {
            demoCase = (DemoCase) savedInstanceState.getSerializable(KEY_DEMO_CASE);
            if (demoCase != null) {
                startDemoCase(demoCase);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        demoCase = null;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        demoCase = (DemoCase) getListAdapter().getItem(position);
        startDemoCase(demoCase);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable(KEY_DEMO_CASE, demoCase);
    }

    private void startDemoCase(@NonNull DemoCase demoCase) {
        getActivity().getSupportFragmentManager().beginTransaction()
                     .replace(R.id.fragment_container, demoCase.fragment, demoCase.fragmentTag)
                     .addToBackStack(null)
                     .commit();
    }
}
