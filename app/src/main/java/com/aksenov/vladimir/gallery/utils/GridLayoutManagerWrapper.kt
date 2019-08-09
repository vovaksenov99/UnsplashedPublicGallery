package com.aksenov.vladimir.gallery.utils

import android.content.Context
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.util.Log

class WrapContentGridLayoutManager(context: Context, columnCount: Int) :
        androidx.recyclerview.widget.GridLayoutManager(context, columnCount) {

    override fun onLayoutChildren(recycler: androidx.recyclerview.widget.RecyclerView.Recycler?, state: androidx.recyclerview.widget.RecyclerView.State) {
        try {
            super.onLayoutChildren(recycler, state)
        } catch (e: IndexOutOfBoundsException) {
            Log.e("TAG", "meet a IOOBE in RecyclerView")
        }
    }
}
