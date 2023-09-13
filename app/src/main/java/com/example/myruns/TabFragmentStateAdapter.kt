package com.example.myruns

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class TabFragmentStateAdapter(activity: FragmentActivity, var inputList: ArrayList<Fragment>) : FragmentStateAdapter(activity) {

    override fun createFragment(position: Int): Fragment {
        return inputList[position]
    }

    override fun getItemCount(): Int {
        return inputList.size
    }
}