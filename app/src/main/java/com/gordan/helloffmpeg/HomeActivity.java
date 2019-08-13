package com.gordan.helloffmpeg;

import android.Manifest;
import android.graphics.drawable.Drawable;
import android.os.Message;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.gordan.baselibrary.BaseActivity;
import com.gordan.helloffmpeg.adapter.MainPagerAdapter;
import com.gordan.helloffmpeg.fragment.AudioFragment;
import com.gordan.helloffmpeg.fragment.ImageFragment;
import com.gordan.helloffmpeg.fragment.MeFragment;
import com.gordan.helloffmpeg.fragment.VideoFragment;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.OnClick;

public class HomeActivity extends BaseActivity implements ViewPager.OnPageChangeListener {

    final static String TAG = HomeActivity.class.getSimpleName();

    @Bind(R.id.tv_title)
    TextView tvTitle;

    @Bind(R.id.tv_menu_image)
    TextView tvImage;
    @Bind(R.id.tv_menu_audio)
    TextView tvAudio;
    @Bind(R.id.tv_menu_video)
    TextView tvVideo;
    @Bind(R.id.tv_menu_me)
    TextView tvAbout;

    @Bind(R.id.vp_home)
    ViewPager mViewPager;

    MainPagerAdapter mPagerAdapter;

    List<Fragment> mFragmentList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO}, 10000);

        mFragmentList = new ArrayList<>();

        ImageFragment imageFragment = new ImageFragment();
        mFragmentList.add(imageFragment);

        AudioFragment audioFragment = new AudioFragment();
        mFragmentList.add(audioFragment);

        VideoFragment videoFragment = new VideoFragment();
        mFragmentList.add(videoFragment);

        MeFragment meFragment = new MeFragment();
        mFragmentList.add(meFragment);

        mPagerAdapter = new MainPagerAdapter(getSupportFragmentManager(), mFragmentList);
        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.addOnPageChangeListener(this);
        mViewPager.setOffscreenPageLimit(3);
        //默认选中视频模块
        mViewPager.setCurrentItem(2);
    }

    @Override
    protected int inflateResId() {
        return R.layout.activity_home;
    }

    @Override
    protected void handleBaseMessage(Message message) {

    }

    @OnClick({R.id.tv_menu_image, R.id.tv_menu_audio, R.id.tv_menu_video, R.id.tv_menu_me})
    public void onViewClick(View view) {
        switch (view.getId()) {
            case R.id.tv_menu_image:
                setHomeMenuStyle(0);
                break;

            case R.id.tv_menu_audio:
                setHomeMenuStyle(1);
                break;

            case R.id.tv_menu_video:
                setHomeMenuStyle(2);
                break;

            case R.id.tv_menu_me:
                setHomeMenuStyle(3);
                break;
        }
    }

    public void setHomeMenuStyle(int index) {
        switch (index) {
            case 0:
                tvTitle.setText("图像");
                mViewPager.setCurrentItem(0);
                setMenuStyle(tvImage, R.drawable.image_selected, R.color.menu_selected);
                setMenuStyle(tvAudio, R.drawable.audio_default, R.color.menu_default);
                setMenuStyle(tvVideo, R.drawable.video_default, R.color.menu_default);
                setMenuStyle(tvAbout, R.drawable.me_default, R.color.menu_default);
                break;
            case 1:
                tvTitle.setText("音频");
                mViewPager.setCurrentItem(1);
                setMenuStyle(tvImage, R.drawable.image_default, R.color.menu_default);
                setMenuStyle(tvAudio, R.drawable.audio_selected, R.color.menu_selected);
                setMenuStyle(tvVideo, R.drawable.video_default, R.color.menu_default);
                setMenuStyle(tvAbout, R.drawable.me_default, R.color.menu_default);
                break;

            case 2:
                tvTitle.setText("视频");
                mViewPager.setCurrentItem(2);
                setMenuStyle(tvImage, R.drawable.image_default, R.color.menu_default);
                setMenuStyle(tvAudio, R.drawable.audio_default, R.color.menu_default);
                setMenuStyle(tvVideo, R.drawable.video_selected, R.color.menu_selected);
                setMenuStyle(tvAbout, R.drawable.me_default, R.color.menu_default);
                break;

            case 3:
                tvTitle.setText("关于");
                mViewPager.setCurrentItem(3);
                setMenuStyle(tvImage, R.drawable.image_default, R.color.menu_default);
                setMenuStyle(tvAudio, R.drawable.audio_default, R.color.menu_default);
                setMenuStyle(tvVideo, R.drawable.video_default, R.color.menu_default);
                setMenuStyle(tvAbout, R.drawable.me_selected, R.color.menu_selected);
                break;
            default:
                break;
        }
    }

    public void setMenuStyle(TextView textView, int drawableId, int textColorId) {
        Drawable drawable = ContextCompat.getDrawable(this, drawableId);
        textView.setCompoundDrawablesWithIntrinsicBounds(null, drawable, null, null);
        textView.setTextColor(ContextCompat.getColor(this, textColorId));
    }

    @Override
    public void onPageScrolled(int i, float v, int i1) {

    }

    @Override
    public void onPageSelected(int i) {

        Log.i(TAG, "====onPageSelected()=====" + i);

        setHomeMenuStyle(i);

    }

    @Override
    public void onPageScrollStateChanged(int i) {

    }
}
