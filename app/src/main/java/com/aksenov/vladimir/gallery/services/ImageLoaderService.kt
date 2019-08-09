package com.aksenov.vladimir.gallery.services

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.ResultReceiver
import android.util.Log
import com.aksenov.vladimir.gallery.activities.NO_INTERNET_BROADCAST_RECEIVER_TAG
import com.aksenov.vladimir.gallery.activities.RECYCLER_BROADCAST_RECEIVER_TAG
import com.aksenov.vladimir.gallery.entities.Image
import com.aksenov.vladimir.gallery.INTERNAL_COVER_STORAGE
import com.aksenov.vladimir.gallery.services.ImageLoaderServiceReceiver.Companion.IMAGE_LOADER_RECEIVER_TAG
import com.aksenov.vladimir.gallery.utils.Utils
import com.aksenov.vladimir.gallery.utils.Utils.Companion.isOnline
import java.io.File
import java.io.Serializable
import java.net.URL
import java.util.concurrent.Executors

object LoadQueueStatus {
    const val START = 0
    const val DONE = 1
    const val RUNNING = 2
    const val FAILED = 3
}

data class LoadStatus(var loadedCount: Int, var status: Int) : Serializable

class ImageLoaderService : IntentService("ImageLoaderService") {

    companion object {
        val imageLoadQueue: MutableMap<Int, LoadStatus> = mutableMapOf()

        @JvmStatic
        fun startLoading(context: Context, list: List<Image>, page: Int,
                         receiver: ImageLoaderServiceReceiver?) {
            val intent = Intent(context, ImageLoaderService::class.java).apply {
                action = "LoadImages"
                putExtra("imagesList", list as Serializable)
                putExtra("page", page)
                putExtra(IMAGE_LOADER_RECEIVER_TAG, receiver)
            }
            context.startService(intent)
        }
    }

    var receiver: ResultReceiver? = null

    override fun onHandleIntent(intent: Intent?) {
        receiver = intent?.getParcelableExtra(IMAGE_LOADER_RECEIVER_TAG)

        when (intent?.action) {
            "LoadImages" -> {
                val images = intent.getSerializableExtra("imagesList") as List<Image>
                val page = intent.getIntExtra("page", -1)

                handler(images, page)
            }
        }
    }

    private fun handler(list: List<Image>, page: Int) {

        val executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2)

        for ((i, image) in list.withIndex()) {
            executor.submit {
                imageLoadQueue[page]?.status = LoadQueueStatus.RUNNING
                val file =
                        File(INTERNAL_COVER_STORAGE + image.id + ".jpg")
                if (!file.exists()) {
                    val url = URL(image.urls?.small)
                    val stream = url.openConnection().getInputStream()
                    val btm = BitmapFactory.decodeStream(stream)
                    stream.close()

                    if (btm == null) {
                        if (!isOnline(this)) {
                            val data = Bundle()
                            receiver?.send(NO_INTERNET_BROADCAST_RECEIVER_TAG, data)
                            imageLoadQueue[page]?.status = LoadQueueStatus.FAILED
                            return@submit

                        }
                        Log.i("loadingBTM", "BTM NULL!")
                    } else
                        Utils.saveToInternalStorage(this, "Covers", image.id + ".jpg", btm)
                }

                image.urls?.localCoverPath = INTERNAL_COVER_STORAGE +
                        image.id + ".jpg"

                val data = Bundle()
                data.putSerializable("image", image as Serializable)
                data.putInt("pos", i)
                receiver?.send(RECYCLER_BROADCAST_RECEIVER_TAG, data)

                imageLoadQueue[page]?.loadedCount = imageLoadQueue[page]?.loadedCount!! + 1

                if (imageLoadQueue[page]?.loadedCount == list.size)
                    imageLoadQueue[page]?.status = LoadQueueStatus.DONE

                Log.i("loading page" + page.toString(), imageLoadQueue[page].toString())
            }
        }

    }

}

class ImageLoaderServiceReceiver(handler: Handler) : ResultReceiver(handler) {

    companion object {
        const val IMAGE_LOADER_RECEIVER_TAG = "IMAGE_LOADER_RECEIVER_TAG"
    }

    private var mReceiver: Receiver? = null

    interface Receiver {
        fun onReceiveResult(resultCode: Int, data: Bundle)
    }

    fun setReceiver(receiver: Receiver?) {
        mReceiver = receiver
    }

    override fun onReceiveResult(resultCode: Int, resultData: Bundle) {
            mReceiver?.onReceiveResult(resultCode, resultData)
    }
}