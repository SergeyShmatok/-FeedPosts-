package ru.netology.nmedia.util

import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import ru.netology.nmedia.adapter.AdViewHolder
import ru.netology.nmedia.adapter.TimingViewHolder

class ItemAnimatorForAd : DefaultItemAnimator() {

    // Решение убирающее анимацию у рекламы (мерцание) и разделителя дат

    override fun animateAdd(holder: RecyclerView.ViewHolder): Boolean {

        if (holder is AdViewHolder) {
//          dispatchAddStarting(holder)
            dispatchAddFinished(holder) // Обязательно оставить иначе будут артефакты на "timing holders"
            return false
        }

        if (holder is TimingViewHolder) {
//          dispatchAddStarting(holder)
            dispatchAddFinished(holder)
            return false
        }

        return super.animateAdd(holder)
    }


// Другие настройки анимации⬇️

//    override fun animateChange(
//        oldHolder: RecyclerView.ViewHolder,
//        newHolder: RecyclerView.ViewHolder,
//        preInfo: ItemHolderInfo,
//        postInfo: ItemHolderInfo
//    ): Boolean {
//        // Для рекламных блоков - немедленное обновление без анимации
//        if (oldHolder is AdViewHolder || newHolder is AdViewHolder) {
//            // Корректно завершаем анимации для обоих холдеров
//            dispatchChangeStarting(oldHolder, true)
//            dispatchChangeStarting(newHolder, false)
//
//            // Немедленно завершаем без анимации
//            dispatchChangeFinished(oldHolder, true)
//            dispatchChangeFinished(newHolder, false)
//            return false
//        }
//        return super.animateChange(oldHolder, newHolder, preInfo, postInfo)
//    }
//
//    override fun animatePersistence(
//        holder: RecyclerView.ViewHolder,
//        preInfo: ItemHolderInfo,
//        postInfo: ItemHolderInfo
//    ): Boolean {
//        // Отключаем анимацию "сохранения" для рекламы
//        if (holder is AdViewHolder) {
//            dispatchChangeStarting(holder, false)
//            dispatchChangeFinished(holder, false)
//            return false
//        }
//        return super.animatePersistence(holder, preInfo, postInfo)
//    }

    // Добавляем отключение других типов анимаций

//
//    override fun animateRemove(holder: RecyclerView.ViewHolder): Boolean {
//        if (holder is AdViewHolder) {
//            dispatchRemoveStarting(holder)
//            dispatchRemoveFinished(holder)
//            return false
//        }
//        return super.animateRemove(holder)
//    }

}
