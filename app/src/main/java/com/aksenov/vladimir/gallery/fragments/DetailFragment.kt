package com.aksenov.vladimir.gallery.fragments

import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.transition.TransitionInflater
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.aksenov.vladimir.gallery.entities.Image
import com.aksenov.vladimir.gallery.R
import com.aksenov.vladimir.gallery.services.ImageLoaderService
import kotlinx.android.synthetic.main.detail_fragment.view.avatar
import kotlinx.android.synthetic.main.detail_fragment.view.bio
import kotlinx.android.synthetic.main.detail_fragment.view.description
import kotlinx.android.synthetic.main.detail_fragment.view.fullImageView
import kotlinx.android.synthetic.main.detail_fragment.view.imageView
import kotlinx.android.synthetic.main.detail_fragment.view.username
import kotlinx.android.synthetic.main.detail_fragment.view.username_block

class DetailFragment : androidx.fragment.app.Fragment() {
    private lateinit var image: Image

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = layoutInflater.inflate(R.layout.detail_fragment, null)

        val bundle = this.arguments
        if (bundle != null)
            if (bundle.containsKey("image"))
                image = bundle.getSerializable("image") as Image

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            view.imageView.transitionName = image.id
        }

        val btm = BitmapFactory.decodeFile(image.urls?.localCoverPath)
        view.imageView.setImageBitmap(btm)

        view.description.text = image.description ?: "No description provided"
        view.username.text = image.user?.name
        view.bio.text = image.user?.bio ?: "No bio provided"

        Glide.with(context!!)
                .load(image.urls?.regular)
                .into(view!!.fullImageView)

        Glide.with(context!!)
                .load(image.user?.profile_image?.large)
                .placeholder(R.drawable.account)
                .into(view.avatar)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.username_block.setOnClickListener {
            val fragment = AuthorImagesFragment()
            fragment.arguments = Bundle().apply {
                putSerializable("user", image.user)
            }

            activity?.supportFragmentManager?.beginTransaction()?.replace(R.id.container,
                    fragment, "AuthorListFragment")?.addToBackStack(null)?.commit()
        }
    }

    override fun onPause() {
        view?.imageView?.setImageBitmap(null)
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        ImageLoaderService.imageLoadQueue.clear()

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        postponeEnterTransition()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            sharedElementEnterTransition =
                    TransitionInflater.from(context).inflateTransition(android.R.transition.move)
        }
    }

}
