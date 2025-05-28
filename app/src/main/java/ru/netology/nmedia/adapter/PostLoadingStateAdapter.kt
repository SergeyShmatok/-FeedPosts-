package ru.netology.nmedia.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.netology.nmedia.databinding.LoadStateBinding

interface InteractionListener {
    fun onRetry() {}
}

class PostLoadingStateAdapter(
    private val onInteractionListener: InteractionListener,
) : LoadStateAdapter<LoadingHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): LoadingHolder =
        LoadingHolder(
            LoadStateBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            onInteractionListener
        )

    override fun onBindViewHolder(holder: LoadingHolder, loadState: LoadState) {
        // Данные в "holder" приходят в виде loadState, хранящем информацию о состоянии загрузки.
        holder.bind(loadState)
    }

}


class LoadingHolder(
    // Описываем класс целевого холдера
    private val loadStateBinding: LoadStateBinding,
    private val onInteractionListener: InteractionListener,
) : RecyclerView.ViewHolder(loadStateBinding.root) {

    fun bind(loadState: LoadState) {

        loadStateBinding.apply {

            progress.isVisible = loadState is LoadState.Loading
            // Показываем элемент, если происходит загрузка
            retry.isVisible = loadState is LoadState.Error // Если ошибка
            retry.setOnClickListener {

                onInteractionListener.onRetry()

            }

        }

    }
}