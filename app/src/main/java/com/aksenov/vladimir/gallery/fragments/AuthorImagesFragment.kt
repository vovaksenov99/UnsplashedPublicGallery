package com.aksenov.vladimir.gallery.fragments

import android.accounts.NetworkErrorException
import android.content.Context
import android.widget.Toast
import com.aksenov.vladimir.gallery.R
import com.aksenov.vladimir.gallery.entities.Image
import com.aksenov.vladimir.gallery.entities.SplashUser
import com.aksenov.vladimir.gallery.services.ImageLoaderService
import com.aksenov.vladimir.gallery.services.ImageLoaderService.Companion.imageLoadQueue
import com.aksenov.vladimir.gallery.services.ImageLoaderServiceReceiver
import com.aksenov.vladimir.gallery.services.LoadQueueStatus
import com.aksenov.vladimir.gallery.services.LoadStatus
import com.aksenov.vladimir.gallery.utils.Utils
import kotlinx.android.synthetic.main.main_activity.toolbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class AuthorImagesFragment : ImagesPreviewFragment() {

    override var limit: Int = -1
    lateinit var user: SplashUser

    override fun onAttach(context: Context?) {
        arguments?.let { arguments ->
            if (arguments.containsKey("user"))
                user = arguments.getSerializable("user") as SplashUser

            limit = user.total_photos!!
        }
        super.onAttach(context)
    }

    override fun onResume() {
        super.onResume()
        initUI()
    }

    private fun initUI() {
        activity?.toolbar?.title = user.name
    }

    override fun onDestroyView() {
        imageLoadQueue.clear()
        super.onDestroyView()
    }

    override fun uploadNewImages(context: Context, page: Int,
                                 receiver: ImageLoaderServiceReceiver?) {
        imageLoadQueue[page] = LoadStatus(0, LoadQueueStatus.START)

        GlobalScope.launch(Dispatchers.IO) {
            val list: List<Image>
            try {
                list = Utils.getUserImagesList(context, page, user)
            } catch (e: NetworkErrorException) {
                GlobalScope.launch(Dispatchers.Main) {
                    Toast.makeText(context,
                            context.getString(R.string.no_internet),
                            Toast.LENGTH_SHORT).show()
                    imageLoadQueue[page]?.status = LoadQueueStatus.FAILED
                }
                return@launch
            } catch (e: IllegalStateException) {
                GlobalScope.launch(Dispatchers.Main) {
                    Toast.makeText(context,
                            e.message,
                            Toast.LENGTH_SHORT).show()
                    imageLoadQueue[page]?.status = LoadQueueStatus.FAILED
                }
                return@launch
            }
            ImageLoaderService.startLoading(context, list, page, receiver)
        }
    }

}
