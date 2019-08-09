package com.aksenov.vladimir.gallery.adapters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat
import androidx.collection.LruCache
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.transition.TransitionInflater
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.aksenov.vladimir.gallery.activities.MainActivity
import com.aksenov.vladimir.gallery.database.LocalDatabaseAPI
import com.aksenov.vladimir.gallery.entities.Image
import com.aksenov.vladimir.gallery.fragments.DataProvider
import com.aksenov.vladimir.gallery.fragments.DetailFragment
import com.aksenov.vladimir.gallery.IMAGE_PER_PAGE
import com.aksenov.vladimir.gallery.R
import com.aksenov.vladimir.gallery.services.ImageLoaderService
import com.aksenov.vladimir.gallery.services.ImageLoaderServiceReceiver
import com.aksenov.vladimir.gallery.services.LoadQueueStatus
import kotlinx.android.synthetic.main.preview_item.view.image
import kotlinx.android.synthetic.main.preview_item.view.likeBtn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.Serializable

class PreviewAdapter(val parentFragment: androidx.fragment.app.Fragment, val dataProviderI: DataProvider,
                     val receiver: ImageLoaderServiceReceiver?, val db: LocalDatabaseAPI,
                     val limit: Int = -1) :
        androidx.recyclerview.widget.RecyclerView.Adapter<PreviewAdapter.previewHolder>() {

    var list: MutableList<Image?> = mutableListOf()

    private val previews: androidx.collection.LruCache<String, Bitmap?>

    init {
        val maxMemory = Runtime.getRuntime().maxMemory().toInt()
        val cacheSize = maxMemory / 3

        previews = object : androidx.collection.LruCache<String, Bitmap?>(cacheSize) {
            override fun sizeOf(key: String, value: Bitmap): Int {
                return value.byteCount
            }
        }
    }

    var page = 1

    lateinit var recyclerView: androidx.recyclerview.widget.RecyclerView

    override fun onAttachedToRecyclerView(recyclerView: androidx.recyclerview.widget.RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView

        if (page == 1)
            uploadMore()

        recyclerView.addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager!!
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val pastVisiblesItems =
                        (layoutManager as androidx.recyclerview.widget.GridLayoutManager).findFirstVisibleItemPosition()

                if ((visibleItemCount + pastVisiblesItems) >= totalItemCount) {
                    page++
                    uploadMore()
                }
            }
        })
    }

    fun uploadMore() {
        synchronized(list) {
            for (i in 0 until IMAGE_PER_PAGE) {
                if (list.size == limit)
                    return@synchronized
                list.add(null)
                recyclerView.post {
                    notifyItemInserted(list.size - 1)
                }
            }
        }
    }

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): previewHolder {

        val view = LayoutInflater.from(p0.context).inflate(R.layout.preview_item, null)
        return previewHolder(view)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: previewHolder, position: Int) {
        val image = list[position]

        val page = position / IMAGE_PER_PAGE + 1

        if (image == null) {

            holder.imageView.setBackgroundColor(Color.GRAY)
            holder.itemView.setOnClickListener {}
            holder.imageView.setImageBitmap(null)
            holder.likeBtn.visibility = View.INVISIBLE
            holder.likeBtn.setOnClickListener {}

            if (!ImageLoaderService.imageLoadQueue.contains(page)
                    || ImageLoaderService.imageLoadQueue[page]?.status == LoadQueueStatus.DONE
                    || ImageLoaderService.imageLoadQueue[page]?.status == LoadQueueStatus.FAILED) {
                dataProviderI.uploadNewImages(recyclerView.context, page, receiver)
            }
            return
        }

        ViewCompat.setTransitionName(holder.imageView, image.id)

        synchronized(previews) {
            if (previews.get(image.id) != null) {
                holder.imageView.setImageBitmap(previews.get(image.id))
                holder.likeBtn.visibility = View.VISIBLE
            } else {
                GlobalScope.launch(Dispatchers.IO) {
                    val options = BitmapFactory.Options()
                    options.inJustDecodeBounds = false
                    options.inPreferredConfig = Bitmap.Config.RGB_565

                    var btm: Bitmap? = null
                    try {
                        btm = BitmapFactory.decodeFile(image.urls?.localCoverPath, options)
                    } catch (e: OutOfMemoryError) {
                        Log.i("Service", "Too big image")
                    }

                    if (btm != null) {
                        previews.put(image.id, btm)
                        GlobalScope.launch(Dispatchers.Main) {
                            holder.imageView.setImageBitmap(btm)
                            holder.likeBtn.visibility = View.VISIBLE
                        }
                    }
                }
            }

        }

        initHolder(holder, image)
    }

    private fun initHolder(holder: previewHolder, image: Image) {
        GlobalScope.launch(Dispatchers.IO) {
            var isLiked = db.isImageExistInDatabase(image)
            GlobalScope.launch(Dispatchers.Main) {
                if (isLiked)
                    like(holder)
                else
                    unlike(holder)


                holder.itemView.setOnClickListener {
                    onAnimalItemClick(parentFragment.context!!,
                            image,
                            holder.imageView)
                }

                holder.likeBtn.setOnClickListener {
                    if (!isLiked) {
                        isLiked = true
                        GlobalScope.launch(Dispatchers.IO) {
                            db.insertImageInDatabase(image)
                            GlobalScope.launch(Dispatchers.Main) {
                                like(holder)
                            }
                        }
                    } else {
                        isLiked = false
                        GlobalScope.launch(Dispatchers.IO) {
                            db.removeImageFromDatabase(image)
                            GlobalScope.launch(Dispatchers.Main) {
                                unlike(holder)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun like(holder: previewHolder) {
        try {
            holder.likeBtn.setImageResource(R.drawable.like_fill)
            holder.likeBtn.setColorFilter(ContextCompat.getColor(parentFragment.context!!,
                    R.color.likeSelectColor),
                    android.graphics.PorterDuff.Mode.SRC_IN)
        } catch (e: Exception) {
        }
    }

    private fun unlike(holder: previewHolder) {
        try {

            holder.likeBtn.setImageResource(R.drawable.like_outline)
            holder.likeBtn.setColorFilter(ContextCompat.getColor(parentFragment.context!!,
                    R.color.likeOutlineColor),
                    android.graphics.PorterDuff.Mode.SRC_IN)
        } catch (e: Exception) {
        }
    }

    private fun onAnimalItemClick(context: Context, image: Image, sharedImageView: ImageView) {
        val bundle = Bundle()
        bundle.putSerializable("image", image as Serializable)

        val changeTransform =
                TransitionInflater.from(context)
                        .inflateTransition(R.transition.change_image_transform)

        val explodeTransform =
                TransitionInflater.from(context).inflateTransition(android.R.transition.fade)

        parentFragment.sharedElementReturnTransition = changeTransform
        parentFragment.exitTransition = explodeTransform

        val fragment = DetailFragment()
        fragment.arguments = bundle

        val explodeTransformNewFragment =
                TransitionInflater.from(context).inflateTransition(android.R.transition.fade)

        val changeTransformNewFragment =
                TransitionInflater.from(context)
                        .inflateTransition(R.transition.change_image_transform)
        fragment.enterTransition = explodeTransformNewFragment

        fragment.sharedElementEnterTransition = changeTransformNewFragment

        (context as MainActivity).supportFragmentManager
                .beginTransaction()
                .addSharedElement(sharedImageView, ViewCompat.getTransitionName(sharedImageView)!!)
                .addToBackStack("")
                .replace(R.id.container, fragment)
                .commit()
    }

    inner class previewHolder(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
        val imageView = itemView.image
        val likeBtn = itemView.likeBtn

    }
}