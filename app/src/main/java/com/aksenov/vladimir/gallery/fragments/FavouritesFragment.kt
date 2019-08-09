package com.aksenov.vladimir.gallery.fragments

import android.accounts.NetworkErrorException
import android.content.Context
import com.aksenov.vladimir.gallery.R
import com.aksenov.vladimir.gallery.database.LocalDatabaseAPI
import com.aksenov.vladimir.gallery.entities.Image
import com.aksenov.vladimir.gallery.services.ImageLoaderService
import com.aksenov.vladimir.gallery.services.ImageLoaderServiceReceiver
import com.aksenov.vladimir.gallery.services.LoadQueueStatus
import com.aksenov.vladimir.gallery.services.LoadStatus
import kotlinx.android.synthetic.main.main_activity.favs
import kotlinx.android.synthetic.main.main_activity.toolbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class FavouritesFragment : ImagesPreviewFragment() {

    override var limit: Int = -1

    override fun onAttach(context: Context?) {
        db = LocalDatabaseAPI(context!!)
        limit = db.getImagesCount()
        super.onAttach(context)
    }

    override fun onResume() {
        super.onResume()
        initUI()
    }

    override fun onDestroyView() {
        onDestroyUI()
        super.onDestroyView()
    }

    private fun onDestroyUI() {
        ImageLoaderService.imageLoadQueue.clear()
        activity?.toolbar?.title = getString(R.string.app_name)
        activity?.favs?.setImageResource(R.drawable.like_outline)

    }

    private fun initUI() {
        ImageLoaderService.imageLoadQueue.clear()
        activity?.toolbar?.title = "Favourites"
        activity?.favs?.setImageResource(R.drawable.like_fill)
    }

    override fun uploadNewImages(context: Context, page: Int,
                                 receiver: ImageLoaderServiceReceiver?) {
        ImageLoaderService.imageLoadQueue[page] =
                LoadStatus(0,
                        LoadQueueStatus.START)

        GlobalScope.launch(Dispatchers.IO) {
            val list: List<Image>
            try {
                list = db.getFavouritesFromDatabase(page)
            } catch (e: NetworkErrorException) {
                return@launch
            }
            ImageLoaderService.startLoading(context,
                    list,
                    page,
                    receiver)
        }
    }

}
