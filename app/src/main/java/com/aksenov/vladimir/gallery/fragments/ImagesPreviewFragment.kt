package com.aksenov.vladimir.gallery.fragments

import android.content.Context
import android.os.Bundle
import android.os.Handler
import androidx.fragment.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.aksenov.vladimir.gallery.IMAGE_PER_PAGE
import com.aksenov.vladimir.gallery.R
import com.aksenov.vladimir.gallery.activities.NO_INTERNET_BROADCAST_RECEIVER_TAG
import com.aksenov.vladimir.gallery.activities.RECYCLER_BROADCAST_RECEIVER_TAG
import com.aksenov.vladimir.gallery.adapters.PreviewAdapter
import com.aksenov.vladimir.gallery.database.LocalDatabaseAPI
import com.aksenov.vladimir.gallery.entities.Image
import com.aksenov.vladimir.gallery.services.ImageLoaderServiceReceiver
import com.aksenov.vladimir.gallery.utils.WrapContentGridLayoutManager
import kotlinx.android.synthetic.main.preview_fragment.recyclerView

interface DataProvider {
    fun uploadNewImages(context: Context, page: Int, receiver: ImageLoaderServiceReceiver?)
}

abstract class ImagesPreviewFragment : androidx.fragment.app.Fragment(), ImageLoaderServiceReceiver.Receiver, DataProvider {

    private lateinit var adapter: PreviewAdapter
    private var receiver: ImageLoaderServiceReceiver? = ImageLoaderServiceReceiver(Handler())

    abstract var limit: Int
    lateinit var db: LocalDatabaseAPI

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        db = LocalDatabaseAPI(context!!)
        receiver?.setReceiver(this)
        adapter = PreviewAdapter(this, this, receiver, db, limit)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return layoutInflater.inflate(R.layout.preview_fragment, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initPreviewGrid()
    }

    override fun onResume() {
        super.onResume()
        receiver?.setReceiver(this)
    }

    override fun onPause() {
        super.onPause()
        receiver?.setReceiver(null)
    }

    private fun initPreviewGrid() {
        view.apply {
            val layoutManager = WrapContentGridLayoutManager(context!!, 2)
            recyclerView.setHasFixedSize(true)
            recyclerView.layoutManager = layoutManager
            recyclerView.isNestedScrollingEnabled = true
            recyclerView.adapter = adapter
        }
    }

    override fun onReceiveResult(resultCode: Int, data: Bundle) {
        when (resultCode) {
            RECYCLER_BROADCAST_RECEIVER_TAG -> {
                adapter.apply {
                    synchronized(list) {
                        val image = data.getSerializable("image") as Image
                        val pos = data.getInt("pos", -1)
                        val notifyPos = IMAGE_PER_PAGE * (image.page!! - 1) + pos
                        if (list[notifyPos] != image) {
                            list[notifyPos] = image
                            notifyItemChanged(notifyPos)
                        }
                    }
                }

            }
            NO_INTERNET_BROADCAST_RECEIVER_TAG -> {
                Log.i("PreviewReceiver", "Need internet connection")
                Toast.makeText(context, "Need internet connection", Toast.LENGTH_SHORT).show()
            }
        }
    }

    abstract override fun uploadNewImages(context: Context, page: Int, receiver: ImageLoaderServiceReceiver?)

}
