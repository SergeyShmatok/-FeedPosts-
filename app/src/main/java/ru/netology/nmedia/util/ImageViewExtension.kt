package ru.netology.nmedia.util

import android.widget.ImageView
import com.bumptech.glide.Glide
import ru.netology.nmedia.R


fun ImageView.loadAvatars(url: String) {
    Glide.with(this)
        .load(url)
        .error(R.drawable.ic_error_100dp)
        .placeholder(R.drawable.ic_loading_100dp)
        .circleCrop()
        .timeout(30_000)
        .into(this)
}

fun ImageView.loadAttachments(url: String) {
    Glide.with(this)
        .load(url)
        .error(R.drawable.ic_error_for_attachments_1450dp)
        .placeholder(R.drawable.ic_loading_for_attachments_1450dp)
        .dontAnimate()
        .dontTransform()
        .timeout(30_000)
        .into(this)
}


