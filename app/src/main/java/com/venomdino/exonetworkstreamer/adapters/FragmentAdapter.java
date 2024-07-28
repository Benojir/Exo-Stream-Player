package com.venomdino.exonetworkstreamer.adapters;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.media3.common.util.UnstableApi;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.venomdino.exonetworkstreamer.fragments.HomeFragment;
import com.venomdino.exonetworkstreamer.fragments.LocalVideosFragment;

public class FragmentAdapter extends FragmentStateAdapter {

    int totalTab;

    public FragmentAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle, int totalTab) {
        super(fragmentManager, lifecycle);
        this.totalTab = totalTab;
    }

    @OptIn(markerClass = UnstableApi.class) @NonNull
    @Override
    public Fragment createFragment(int position) {

        if (position == 1) {
            return new HomeFragment();
        } else if (position == 2) {
            return new LocalVideosFragment();
        } else {
            return new HomeFragment();
        }
    }

    @Override
    public int getItemCount() {
        return totalTab;
    }
}