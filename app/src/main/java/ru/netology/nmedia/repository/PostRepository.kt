package ru.netology.nmedia.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.TerminalSeparatorType
import androidx.paging.insertSeparators
import androidx.paging.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import ru.netology.nmedia.api.ApiService
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dao.PostRemoteKeyDao
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.dto.Ad
import ru.netology.nmedia.dto.Attachment
import ru.netology.nmedia.dto.AttachmentType
import ru.netology.nmedia.dto.FeedItem
import ru.netology.nmedia.dto.Media
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.entity.toEntity
import ru.netology.nmedia.error.ApiError
import ru.netology.nmedia.error.AppError
import ru.netology.nmedia.error.NetworkError
import ru.netology.nmedia.error.UnknownError
import ru.netology.nmedia.util.DateSeparator
import java.io.File
import java.io.IOException
import javax.inject.Inject
import kotlin.random.Random


class PostRepository @Inject constructor(
    private val dao: PostDao,
    private val apiService: ApiService,
    private val dateSeparator: DateSeparator,
    remoteKeyDao: PostRemoteKeyDao,
    abbDb: AppDb,

) : PostRepositoryFun {

    @Inject
    lateinit var appAuth: AppAuth

//--------------------------------------------------------------------------------------------------

    override var newPost = MutableStateFlow<List<Post>>(emptyList())

    override var newerCountData: Flow<Long?> = dao.getLastId().flowOn(Dispatchers.Default)

//    val pagingSource: () -> PagingSource<Int, PostEntity> = fun () = dao.getPagingSource()
//    - pagingSourceFactory имеет функциональный тип

    @OptIn(ExperimentalPagingApi::class)
    override val pagingDate: Flow<PagingData<FeedItem>> = Pager(
        config = PagingConfig(pageSize = 25, enablePlaceholders = false),
        pagingSourceFactory = dao::getPagingSource,
        remoteMediator = PostRemoteMediator(
            apiService, dao,
            remoteKeyDao = remoteKeyDao,
            abbDb = abbDb,
        ),
    ).flow
        .map {
            it.map(PostEntity::toDto)
                .insertSeparators (
                    terminalSeparatorType = TerminalSeparatorType.SOURCE_COMPLETE, // не появится Today сверху, если не установить.
                    generator = { previous, next -> dateSeparator.create(previous, next) }
            ).insertSeparators { previous, _ ->
                    // пример динамической генерации рекламы, через 5 элементов
                    if (previous?.id?.rem(5) == 0L) {
                        Ad(Random.nextLong(), "figma.jpg")
                    } else {
                        null
                    }

                }
            // "previous" вначале списка = null,
            // "next" в конце списка = null
        }

//--------------------------------------------------------------------------------------------------

    override suspend fun getAll() {

        try {

            val response = apiService.getAll()

            if (!response.isSuccessful) throw ApiError(response.code(), response.message())

            val posts = response.body() ?: throw UnknownError
            dao.insert(posts.toEntity())

        } catch (e: IOException) {
            throw NetworkError
        } catch (e: ApiError) {
            throw e
        } catch (e: Exception) {
            throw UnknownError
        }
    }

//--------------------------------------------------------------------------------------------------

    override fun getNewerCount(id: Long) = flow {

        while (true) {  // Цикл прерывается вызовом - CancellationException -
            delay(15_000L)

            val response = apiService.getNewer(id)

//            println(response.code())
//            println(response.message())

            if (!response.isSuccessful) throw ApiError(response.code(), response.message())

            val body = response.body() ?: throw ApiError(response.code(), response.message())
            newPost.value += body

            emit(body.size)
        }
    }
        .catch { e -> throw AppError.from(e) }
        .flowOn(Dispatchers.Default)

//--------------------------------------------------------------------------------------------------

    override suspend fun addNewPostsToRoom() {
        newPost.value.toEntity().let { dao.insert(it) }

//            mutex.withLock {        (**)
//            newPost.value = null
//        }
        newPost.value = emptyList()
    }

//--------------------------------------------------------------------------------------------------

    override fun cleanNewPostInRepo() {
        newPost.value = emptyList()
    }

//--------------------------------------------------------------------------------------------------

    override suspend fun likeById(id: Long) {
        dao.likeById(id)
        try {

            val response = apiService.likeById(id)
            if (!response.isSuccessful) throw ApiError(response.code(), response.message())

            response.body() ?: throw UnknownError

        } catch (e: IOException) {
            dao.removeLike(id)
            throw NetworkError
        } catch (e: ApiError) {
            dao.removeLike(id)
            throw e
        } catch (e: Exception) {
            dao.removeLike(id)
            throw UnknownError
        }

    }

//--------------------------------------------------------------------------------------------------

    override suspend fun removeLike(id: Long) {
        dao.removeLike(id)
        try {

            val response = apiService.removeLike(id)
            if (!response.isSuccessful) throw ApiError(response.code(), response.message())

            response.body() ?: throw UnknownError

        } catch (e: IOException) {
            dao.likeById(id)
            throw NetworkError
        } catch (e: ApiError) {
            dao.likeById(id)
            throw e
        } catch (e: Exception) {
            dao.likeById(id)
            throw UnknownError
        }

    }


//--------------------------------------------------------------------------------------------------

    override suspend fun removeById(id: Long) {

        val currentList = dao.getSimpleList()

        dao.removeById(id)

        try {

            val response = apiService.deletePost(id)
            if (!response.isSuccessful) throw ApiError(response.code(), response.message())

            response.body() ?: throw UnknownError

        } catch (e: IOException) {
            dao.insert(currentList)
            throw NetworkError
        } catch (e: ApiError) {
            dao.insert(currentList)
            throw e
        } catch (e: Exception) {
            dao.insert(currentList)
            throw UnknownError
        }
    }

//--------------------------------------------------------------------------------------------------

    override suspend fun save(post: Post) {

        // val currentList = dao.getSimpleList()
        // dao.insert(PostEntity.fromDto(post))

        try {

            val response = apiService.save(post)

            if (!response.isSuccessful) throw ApiError(response.code(), response.message())

            val body = response.body() ?: throw UnknownError

            dao.insert(PostEntity.fromDto(body))

        } catch (e: IOException) {
            throw NetworkError
        } catch (e: ApiError) {
            throw e
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun saveWithAttachment(post: Post, file: File) {

        try {

            val media = upload(file)

            val response =
                apiService.save(post.copy(attachment = Attachment(media.id, AttachmentType.IMAGE)))

            if (!response.isSuccessful) throw ApiError(response.code(), response.message())

            val body = response.body() ?: throw UnknownError

            dao.insert(PostEntity.fromDto(body))

        } catch (e: IOException) {
            throw NetworkError
        } catch (e: ApiError) {
            throw e
        } catch (e: Exception) {
            throw UnknownError
        }

    }

    private suspend fun upload(file: File): Media =
        apiService.upload(
            MultipartBody.Part.createFormData(
                "file",
                file.name,
                file.asRequestBody()
            )
        )
    // имя сервер будет поставлять своё👆
    // MultipartBody.Part.createFormData — метод, который создаёт экземпляр MultipartBody.Part
    // из библиотеки okhttp3. При использовании этого метода нужно указать имя части (обычно «файл»)
    // и созданный RequestBody. Метод используется для работы с форматом Multipart/Form-Data,
    // который позволяет отправлять двоичные данные и несколько типов данных за один запрос.


    override suspend fun updateUser(login: String, pass: String) {

        try {

            val response = apiService.updateUser(login, pass)

            if (!response.isSuccessful) throw ApiError(response.code(), response.message())

            val body = response.body() ?: throw UnknownError

//             isInitialized()

            appAuth.setAuth(body.get("id").asLong, body.get("token").asString)

        } catch (e: IOException) {
            throw NetworkError
        } catch (e: ApiError) {
            throw e
        } catch (e: Exception) {
            throw UnknownError
        }

    }


}

//------------------------------------ End