package com.aksenov.vladimir.gallery.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.aksenov.vladimir.gallery.INTERNAL_COVER_STORAGE
import com.aksenov.vladimir.gallery.R
import com.aksenov.vladimir.gallery.fragments.EndlessListFragment
import com.aksenov.vladimir.gallery.fragments.FavouritesFragment
import kotlinx.android.synthetic.main.main_activity.favs

const val RECYCLER_BROADCAST_RECEIVER_TAG = 0
const val NO_INTERNET_BROADCAST_RECEIVER_TAG = 1

class MainActivity : AppCompatActivity() {

    private var isFavourite = false

    private val fragment = EndlessListFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        INTERNAL_COVER_STORAGE = filesDir.parent + "/app_Covers/"

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().replace(R.id.container,
                    fragment, "EndlessFragment").commitNow()
        }

        favs.setOnClickListener {
            if (!isFavourite) {
                supportFragmentManager.beginTransaction().replace(R.id.container,
                        FavouritesFragment(), "FavouritesFragment").commitNow()
            } else {
                supportFragmentManager.beginTransaction().replace(R.id.container,
                        fragment, "EndlessFragment").commitNow()
            }
            isFavourite = !isFavourite
        }
    }
}
