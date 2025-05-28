package ru.netology.nmedia.viewmodel

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.netology.nmedia.dto.FeedItem
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.error.AppError
import ru.netology.nmedia.error.NetworkError
import ru.netology.nmedia.model.FeedModelState
import ru.netology.nmedia.model.PhotoModel
import ru.netology.nmedia.repository.PostRepositoryFun
import java.io.File
import javax.inject.Inject


private val empty = Post(
    id = 0,
    content = "",
    author = "",
    likedByMe = false,
    likes = 0,
    published = "",
    authorAvatar = "",
    attachment = null,
    authorId = 0,
)

// @Deprecated(message = "Не использовать", replaceWith = ReplaceWith...) (Из вебинара)
@HiltViewModel
class PostViewModel @Inject constructor (
    private val repository: PostRepositoryFun,
    private val applicationContext: Context,
    ) : ViewModel() {

        private val _pagingDate: Flow<PagingData<FeedItem>> =
        repository.pagingDate
            .cachedIn(viewModelScope)
            .catch { e -> throw AppError.from(e)} // В этом
    // задании данные об авторстве поста (ownedByMe) рассчитываются на сервере.

    // Оператор cachedIn() делает поток данных общим и кэширует загруженные
    // данные с предоставленным CoroutineScope. При любом изменении конфигурации он
    // предоставит существующие данные вместо получения данных с нуля. Это также
    // предотвратит утечку памяти.

    val pagingData: Flow<PagingData<FeedItem>>
        get() = _pagingDate

    private val _dataState = MutableStateFlow(FeedModelState())
    val dataState: StateFlow<FeedModelState>
        get() = _dataState

    private val edited = MutableStateFlow(empty)

    private val _postCreated = MutableStateFlow<Unit?>(null)
    val postCreated: Flow<Unit>
        get() = _postCreated.asStateFlow().filterNotNull()

    private val newerData = repository.newerCountData

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _newerCount: StateFlow<Any> = newerData.flatMapLatest {
        repository.getNewerCount(it ?: 0L)
            .catch { e ->
                if (e is NetworkError) {
                    cleanNewPost(); println(e)
                    _dataState.value = FeedModelState(error = true)
                } else throw AppError.from(e)
            }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        false)

    val newerCount: StateFlow<Any>
        get() = _newerCount

    private var _newPostData = repository.newPost

    val newPostData: StateFlow<List<Post>?>
        get() = _newPostData

    private val _photo = MutableStateFlow<PhotoModel?>(null)
    val photo: StateFlow<PhotoModel?>
        get() = _photo

//--------------------------------------------------------------------------------------------------

    fun changePhoto (uri: Uri, file: File) {
        _photo.value = PhotoModel(uri, file)
    }

    fun removePhoto () {
        _photo.value = null
    }

//--------------------------------------------------------------------------------------------------

   fun newPostsIsVisible() = viewModelScope.launch {
       repository.addNewPostsToRoom()
   }

//--------------------------------------------------------------------------------------------------

    fun likeById(id: Long) = viewModelScope.launch {
        // viewModelScope автоматически отменится в случае закрытия активити

        try {
            repository.likeById(id)
            _dataState.value = FeedModelState(likeError = false)
        } catch (e: Exception) {
            _dataState.value = FeedModelState(likeError = true)

            }
        }


//--------------------------------------------------------------------------------------------------

    fun removeLike(id: Long) = viewModelScope.launch {


        try {
            repository.removeLike(id)
            _dataState.value = FeedModelState(likeError = false)
        } catch (e: Exception) {
            _dataState.value = FeedModelState(likeError = true)

        }
    }

//--------------------------------------------------------------------------------------------------

    fun cleanModel() {
        _dataState.value = FeedModelState()
    }

    fun cleanNewPost() {
        repository.cleanNewPostInRepo()
    }

    fun postCreatedIsNull() {
        _postCreated.value = null
    }

//--------------------------------------------------------------------------------------------------

    fun removeById(id: Long) = viewModelScope.launch {

        try {
            repository.removeById(id)
            _dataState.value = FeedModelState(postIsDeleted = true)

        } catch (e: Exception) {
            _dataState.value = FeedModelState(postIsDeleted = false)

        }
    }

//--------------------------------------------------------------------------------------------------

    fun save() = viewModelScope.launch {

        try {
            edited.value.let { post ->
                _postCreated.value = Unit
                 photo.value?.let {
                    repository.saveWithAttachment(post, it.file)
                } ?: repository.save(post)

                _dataState.value = FeedModelState(postIsAdded = true)
            }

        } catch (e: Exception) {
            _dataState.value = FeedModelState(postIsAdded = false)

        }
        edited.value = empty
    }

//--------------------------------------------------------------------------------------------------

    fun toastFun(refreshing: Boolean = false, pickError: Boolean = false) {
        val refreshedPhrase = "Data Refreshed"
        val pickErrorPhrase = "Photo pick error"
        val phrase = listOf(
            "Не удалось, попробуйте позже",
            "Ошибка :(",
            "Что-то пошло нет так..попробуйте снова",
            "Ошибка соединения",
        )

        val randomPhrase = phrase.random()
        val text = when {
                refreshing -> refreshedPhrase
                pickError -> pickErrorPhrase
                else -> randomPhrase
        }
        Toast.makeText(applicationContext, text, Toast.LENGTH_LONG).show()
    }

//--------------------------------------------------------------------------------------------------

    fun edit(post: Post) {
        edited.value = post
    }

    fun changeContent(content: String) {
        val text = content.trim()
        if (edited.value.content == text) {
            return
        }

        edited.value = edited.value.copy(content = text)
    }
}

//------------------------------------ End