package com.micronet_inc.smarthubsampleapp.activities;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.micronet_inc.smarthubsampleapp.R;
import com.micronet_inc.smarthubsampleapp.fragments.AboutFragment;
import com.micronet_inc.smarthubsampleapp.fragments.Can1OverviewFragment;
import com.micronet_inc.smarthubsampleapp.fragments.Can2OverviewFragment;
import com.micronet_inc.smarthubsampleapp.fragments.CanBusFragment;
import com.micronet_inc.smarthubsampleapp.fragments.CanbusFramesFragment;
import com.micronet_inc.smarthubsampleapp.fragments.HWFragment;
import com.micronet_inc.smarthubsampleapp.fragments.InputOutputsFragment;
import com.micronet_inc.smarthubsampleapp.fragments.J1708Fragment;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "SMARTHUB";
    private TabLayout tabLayout;
    private ViewPager viewPager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewPager = (ViewPager) findViewById(R.id.viewpager);
        setupViewPager(viewPager);

        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.e(TAG, "Configuration changed: " + newConfig.toString());
    }

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new InputOutputsFragment(), "GPIOs");
//        adapter.addFragment(new HWFragment(), "Misc");
        adapter.addFragment(new Can1OverviewFragment(), "Can1");
        adapter.addFragment(new Can2OverviewFragment(), "Can2");
        adapter.addFragment(new CanbusFramesFragment(), "CanFrames");
        adapter.addFragment(new J1708Fragment(), "J1708");
        adapter.addFragment(new AboutFragment(), "Info");
        viewPager.setAdapter(adapter);
    }

    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }
}
