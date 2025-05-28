package ru.netology.nmedia.model


data class FeedModelState(
    val loading: Boolean = false,
    val error: Boolean = false,
    val refreshing: Boolean = false,

    val postIsAdded: Boolean = true,
    val likeError: Boolean = false,
    val postIsDeleted: Boolean = true,

    )
