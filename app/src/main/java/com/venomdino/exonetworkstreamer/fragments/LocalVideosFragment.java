package com.venomdino.exonetworkstreamer.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.media3.common.util.UnstableApi;
import androidx.recyclerview.widget.GridLayoutManager;

import com.venomdino.exonetworkstreamer.adapters.LocalVideosRVAdapter;
import com.venomdino.exonetworkstreamer.databinding.FragmentLocalVideosBinding;
import com.venomdino.exonetworkstreamer.helpers.VideoUtil;
import com.venomdino.exonetworkstreamer.models.VideoInfoModel;

import java.util.List;

@UnstableApi
public class LocalVideosFragment extends Fragment{

    private FragmentLocalVideosBinding binding;
    @SuppressLint("StaticFieldLeak")
    public static LocalVideosRVAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentLocalVideosBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Activity activity = getActivity();

        if (activity != null) {

            List<VideoInfoModel> videos = VideoUtil.getAllVideos(activity);

            if (videos.size() > 0) {
                adapter = new LocalVideosRVAdapter(activity, videos);
                binding.recyclerView.setAdapter(adapter);

                GridLayoutManager layoutManager = new GridLayoutManager(activity,2);
                binding.recyclerView.setLayoutManager(layoutManager);

                binding.searchBarET.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        if (s.length() > 0) {
                            adapter.getFilter().filter(s);
                        } else {
                            adapter.getFilter().filter(null);
                        }
                    }

                    @Override
                    public void afterTextChanged(Editable s) {}
                });

            } else {
                Toast.makeText(activity, "No video found.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}