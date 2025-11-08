package ru.netology.nmedia.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import ru.netology.nmedia.BuildConfig
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.CardAdBinding
import ru.netology.nmedia.databinding.CardPostBinding
import ru.netology.nmedia.databinding.CardTimingSeparatorBinding
import ru.netology.nmedia.dto.Ad
import ru.netology.nmedia.dto.FeedItem
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.dto.TimingSeparator
import ru.netology.nmedia.dto.TimingSeparatorType.THIS_WEEK
import ru.netology.nmedia.dto.TimingSeparatorType.TODAY
import ru.netology.nmedia.dto.TimingSeparatorType.WEEK_AGO
import ru.netology.nmedia.dto.TimingSeparatorType.YESTERDAY
import ru.netology.nmedia.util.loadAttachments
import ru.netology.nmedia.util.loadAvatars

interface OnInteractionListener {
    fun onLike(post: Post) {}
    fun onEdit(post: Post) {}
    fun onRemove(post: Post) {}
    fun onShare(post: Post) {}
    fun openPhoto(post: Post) {}
}

typealias ViewHolder = RecyclerView.ViewHolder
typealias Diff = DiffUtil.ItemCallback<FeedItem>

class PostsAdapter(
    private val onInteractionListener: OnInteractionListener,
) : PagingDataAdapter<FeedItem, ViewHolder>(PostDiffCallback()) {

    private val typeData = 0
    private val typePost = 1
    private val typeAd = 2


    // Метод getItemViewType() в Android позволяет определить разные типы представлений (view types)
    // для одного элемента данных.
    override fun getItemViewType(position: Int): Int =
        // Для чего нужен этот переход от "типов" к цифрам🤔(?)
        when (getItem(position)) {
            is TimingSeparator -> typeData
            is Post -> typePost
            is Ad -> typeAd
            null -> error("unknown item type")

        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =

        when (viewType) {

            typePost -> {
                val binding =
                    CardPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                PostViewHolder(binding, onInteractionListener)
            }

            typeAd -> {
                val binding =
                    CardAdBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                AdViewHolder(binding)
            }

            typeData -> {
                val binding =
                    CardTimingSeparatorBinding.inflate(
                        LayoutInflater.from(parent.context), parent, false
                    )
                TimingViewHolder(binding)

            }

            else -> error("unknown item type: $viewType")
        }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Метод для обычной привязки💁‍♂️
        when (val item = getItem(position)) {
            is Ad -> (holder as? AdViewHolder)?.bind(item)
            is Post -> (holder as? PostViewHolder)?.bind(item)
            is TimingSeparator -> (holder as? TimingViewHolder)?.bind(item)
            null -> error("unknown item type")
        }
    }

}


class AdViewHolder(
    private val binding: CardAdBinding,
) : ViewHolder(binding.root) {

//    init {
//        // Отключаем анимации для этого элемента
//        itemView.setHasTransientState(true)
//    }

    fun bind(ad: Ad) {
        binding.image.loadAttachments("${BuildConfig.BASE_URL}/media/${ad.image}")
    }
}


class TimingViewHolder(
    private val binding: CardTimingSeparatorBinding,
) : ViewHolder(binding.root) {

    fun bind(timingSeparator: TimingSeparator) {
        binding.root.setText(
            when (timingSeparator.type) {
                TODAY -> R.string.today
                YESTERDAY -> R.string.yesterday
                THIS_WEEK -> R.string.this_week
                WEEK_AGO -> R.string.week_ago
            }
        )
    }
}

class PostViewHolder(
    private val binding: CardPostBinding,
    private val onInteractionListener: OnInteractionListener,
) : ViewHolder(binding.root) {

    fun bind(post: Post) {
        binding.apply {
            author.text = post.author
            published.text = post.published
            content.text = post.content

            like.isChecked = post.likedByMe
            like.text = "${post.likes}"

            avatar.loadAvatars("${BuildConfig.BASE_URL}/avatars/${post.authorAvatar}")
            if (post.attachment != null) attachment.apply {
                loadAttachments("${BuildConfig.BASE_URL}/media/${post.attachment?.url}")
                // contentDescription = post.attachment?.description
                visibility = View.VISIBLE
            }
            else attachment.visibility = View.GONE

            menu.visibility = if (post.ownedByMe) View.VISIBLE else View.INVISIBLE

//            menu.isVisible = post.ownedByMe

            menu.setOnClickListener {
                PopupMenu(it.context, it).apply {
                    inflate(R.menu.options_post)
                    setOnMenuItemClickListener { item ->
                        when (item.itemId) {

                            R.id.remove -> {
                                onInteractionListener.onRemove(post)
                                true
                            }

                            R.id.edit -> {
                                onInteractionListener.onEdit(post)
                                true
                            }

                            else -> false
                        }
                    }
                }.show()
            }


            like.setOnClickListener {
                onInteractionListener.onLike(post)
            }


            share.setOnClickListener {
                onInteractionListener.onShare(post)
            }

            attachment.setOnClickListener {
                onInteractionListener.openPhoto(post)
            }

        }
    }

}

//data class Payload(
//    val likedByMe: Boolean? = null,
////    val content: String? = null,
//)


class PostDiffCallback : Diff() {
    override fun areItemsTheSame(oldItem: FeedItem, newItem: FeedItem): Boolean {
        if (oldItem::class != newItem::class) return false

        return oldItem.id == newItem.id

    }

    override fun areContentsTheSame(oldItem: FeedItem, newItem: FeedItem): Boolean {

       return oldItem == newItem

    }

    override fun getChangePayload(oldItem: FeedItem, newItem: FeedItem): Any? =
        // крч, метод нужен только для оптимизации ("производительность") и чтобы анимация
        // использовалась в нужном месте,- чтобы не 'мелькал' весь UI ("пользовательский опыт")

        when {

            newItem is Post && oldItem is Post -> true
//            {
//                Payload(
//                    likedByMe = newItem.likedByMe.takeIf { it != oldItem.likedByMe }, // возьмём его,
//                    // только если он не равен лайку предыдущего элемента.
//                )
//
//            }
            newItem is Ad && oldItem is Ad -> true

            else -> null

        }

}

//    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: List<Any>) {
//
//        if (payloads.isEmpty()) onBindViewHolder(holder, position) else {
//
//            (getItem(position) as? Post)?.let { (holder as? PostViewHolder)?.bind(it) }
//
//        }
//
//    // (getItem(position) as? TimingSeparator)?.let { (holder as? TimingViewHolder)?.bind(it) }
//        }