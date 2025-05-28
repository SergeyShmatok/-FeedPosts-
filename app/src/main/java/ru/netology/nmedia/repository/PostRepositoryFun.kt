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

//--------------------------------------------------------------------------------------------------
//                               - Прошлая версия (с коллбэками) -
//
// interface PostRepositoryFun {
//
//    // fun getAll(): List<Post>
//    suspend fun getAllAsync(callback: NMediaCallback<List<Post>>)
//    suspend fun save(post: Post, callback: NMediaCallback<Post>)
//
//    suspend fun likeById(id: Long, callback: NMediaCallback<Post>)
//    suspend fun removeById(id: Long, callback: NMediaCallback<Unit>)
//
//    suspend fun removeLike(id: Long, callback: NMediaCallback<Post>)
//
//interface NMediaCallback <T> {
//    fun onSuccess(data: T)
//    fun onError(e: Exception)
//  }
//
//  С корутинами коллбэки☝️ больше не нужны.
//}
//-------------------------------------- End




