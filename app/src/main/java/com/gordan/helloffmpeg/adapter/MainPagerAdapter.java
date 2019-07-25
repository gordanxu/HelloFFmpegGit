package com.gordan.helloffmpeg.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.List;

public class MainPagerAdapter extends FragmentPagerAdapter {

    List<Fragment> mListFragment;

    public MainPagerAdapter(FragmentManager fm, List<Fragment> list) {

        super(fm);
        this.mListFragment = list;
    }


    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public Fragment getItem(int i) {
        return mListFragment.get(i);
    }

    @Override
    public int getCount() {
        return mListFragment.size();
    }
}
