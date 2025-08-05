package ru.netology.nmedia.repository

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import ru.netology.nmedia.dto.FeedItem
import ru.netology.nmedia.dto.Post
import java.io.File

interface PostRepositoryFun {
    val pagingDate: Flow<PagingData<FeedItem>>
    var newPost: MutableStateFlow<List<Post>>
    var newerCountData: Flow<Long?>

    suspend fun getAll()
    suspend fun save(post: Post)
    suspend fun saveWithAttachment (post: Post, file: File)
    suspend fun likeById(id: Long)
    suspend fun removeById(id: Long)
    suspend fun removeLike(id: Long)
    suspend fun updateUser(login: String, pass: String)
    suspend fun addNewPostsToRoom()
    fun cleanNewPostInRepo()

    fun getNewerCount(id: Long): Flow<Int>
}




