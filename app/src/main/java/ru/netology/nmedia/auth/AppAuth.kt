package ru.netology.nmedia.auth

import android.content.Context
import androidx.core.content.edit
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import ru.netology.nmedia.api.ApiService
import ru.netology.nmedia.dto.PushToken
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class AppAuth @Inject constructor(
    private val context: Context
) { // передаём контекст, потому что будем работать с преференсами
    // С помощью @Inject мы учим Hilt создавать/передавать данный объект (для ApiModule).
    private val tokenKey = "token"
    private val idKey = "id"
    private val pref =
        context.getSharedPreferences("auth", Context.MODE_PRIVATE) // название и "мод"
    // preferences можно получить из любого контекста

    private var _authState = MutableStateFlow<AuthState?>(null)

    init {
        val id = pref.getLong(idKey, 0L)
        val token = pref.getString(tokenKey, null)

        if (id == 0L || token == null) {
            pref.edit { clear() }
        } else {
            _authState.value = AuthState(id, token)
        }
        sendPushToken()
    }

    val authState: StateFlow<AuthState?> = _authState // преобразование в обычный Flow

    @InstallIn(SingletonComponent::class)
    @EntryPoint
    interface AppAuthEntryPoint {
        fun getApiService(): ApiService
    }


    fun sendPushToken(token: String? = null) {

        CoroutineScope(Dispatchers.IO).launch {

            try {

                val pushToken = PushToken(
                    token ?: FirebaseMessaging.getInstance().token.await()
                )
                // Возвращает интерфейс точки входа из приложения. Контекст может быть любым
                // контекстом, полученным из контекста приложения. Может использоваться только
                // с интерфейсами точки входа, установленными в SingletonComponent.
                val entryPoint = EntryPointAccessors
                    .fromApplication(context, AppAuthEntryPoint::class.java)
                entryPoint.getApiService().sendPushToken(pushToken)

            } catch (e: Exception) {

                e.printStackTrace()
            }
        }
    }

    @Synchronized // означает, что метод будет защищен от одновременного выполнения
    // несколькими потоками монитором экземпляра
    // (или, для статических методов, класса), на котором определен метод (??)
    fun setAuth(userId: Long, token: String) {
        _authState.value = AuthState(userId, token)
        pref.edit {
            putString(tokenKey, token)
            putLong(idKey, userId)
        }
        sendPushToken()
    }

    @Synchronized
    fun removeAuth() {
        _authState.value = null
        pref.edit {
            clear()
        }
        sendPushToken()
    }

}