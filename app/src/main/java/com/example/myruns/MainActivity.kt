package com.example.myruns

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {
    private lateinit var startFragment: StartFragment
    private lateinit var historyFragment: HistoryFragment
    private lateinit var settingsFragment: SettingsFragment
    private lateinit var fragments: ArrayList<Fragment>

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var tabFragmentStateAdapter: TabFragmentStateAdapter

    private val tabTitles = arrayOf("Start", "History", "Settings")
    private lateinit var tabConfigurationStrategy: TabLayoutMediator.TabConfigurationStrategy
    private lateinit var tabLayoutMediator: TabLayoutMediator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Util.checkPermissions(this)

        startFragment = StartFragment()
        historyFragment = HistoryFragment()
        settingsFragment = SettingsFragment()

        fragments = ArrayList()

        fragments.add(startFragment)
        fragments.add(historyFragment)
        fragments.add(settingsFragment)

        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)
        tabFragmentStateAdapter = TabFragmentStateAdapter(this, fragments)
        viewPager.adapter = tabFragmentStateAdapter

        // TabLayout for Start, History, and Settings tabs
        tabConfigurationStrategy = TabLayoutMediator.TabConfigurationStrategy() { tab: TabLayout.Tab, position: Int ->
            tab.text = tabTitles[position]
        }
        tabLayoutMediator = TabLayoutMediator(tabLayout, viewPager, tabConfigurationStrategy)
        tabLayoutMediator.attach()
    }

    override fun onDestroy() {
        super.onDestroy()
        tabLayoutMediator.detach()
    }
}