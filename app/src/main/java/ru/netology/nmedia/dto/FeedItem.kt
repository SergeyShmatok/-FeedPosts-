package ru.netology.nmedia.dto

// Иногда в названии указывают прямо, что это объект именно для передачи данных.
// (например, PostDto, а не просто Post)
data class Post(
    override val id: Long,
    val authorId: Long,
    val author: String,
    val content: String,
    val published: String,
    val likedByMe: Boolean,
    val likes: Int = 0,
    val authorAvatar: String,
    var attachment: Attachment? = null,
    val ownedByMe: Boolean = false,
    ) : FeedItem

data class Attachment(
    val url: String,
    val type: AttachmentType,
    // val description: String?,
    )

enum class AttachmentType {
    IMAGE
}

data class Ad( // Объект рекламы
override val id: Long,
    val image: String,
) : FeedItem


data class TimingSeparator(
     val type: TimingSeparatorType
) : FeedItem {
    override val id: Long = type.ordinal.toLong() // "свойство ordinal класса перечисления (enum)
    // в Kotlin возвращает порядковый номер константы в списке enum".
}

enum class TimingSeparatorType {
    TODAY,
    YESTERDAY,
    THIS_WEEK,
    WEEK_AGO,

}


sealed interface FeedItem { // для ограниченной реализации (sealed), будут наследоваться только некоторые объекты
    val id: Long
}