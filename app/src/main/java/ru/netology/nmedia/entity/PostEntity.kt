package ru.netology.nmedia.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.netology.nmedia.dto.Post

@Entity
data class PostEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val author: String,
    val authorId: Long,
    val content: String,
    val published: String,
    val likedByMe: Boolean,
    val likes: Int = 0,
    val authorAvatar: String,
    @Embedded // Вместо Embedded раньше писали конвертер для встроенного класса.
    // Так же, можно было просто добавить новые поля из встраиваемого класса, но так компактнее.
    var attachment: AttachmentEmbedded? = null, // Для Attachment (всего, что связанно с базой данных)
    val ownedByMe: Boolean = false,
    // принято создавать отдельные сущности, поэтому появился "AttachmentEmbedded".
) {

    fun toDto() = Post(
        id = id,
        author = author,
        authorId = authorId,
        content = content,
        published = published,
        likedByMe = likedByMe,
        likes = likes,
        authorAvatar = authorAvatar,
        attachment = attachment?.toDto(),
        ownedByMe = ownedByMe,
    ) // По сути, функция просто возвращает инстанс Post
    // и инициализирует его поля полями своего класса (экземпляра).
    // Хорошая практика в связке с функциями (**) расширения👇

    companion object {
        fun fromDto(dto: Post) =
            PostEntity(
                id = dto.id,
                author = dto.author,
                authorId = dto.authorId,
                content = dto.content,
                published = dto.published,
                likedByMe = dto.likedByMe,
                likes = dto.likes,
                authorAvatar = dto.authorAvatar,
                attachment = dto.attachment
                    ?.let(AttachmentEmbedded::fromDto),
                // ?.let(AttachmentEmbedded.fromDto(it))
                ownedByMe = dto.ownedByMe,
            )
    }

}

fun List<PostEntity>.toDto(): List<Post> = map(PostEntity::toDto) // (**)
// fun List<PostEntity>.toDto(): List<Post> = map { it.toDto() } - или так

fun List<Post>.toEntity(): List<PostEntity> = map(PostEntity::fromDto)
