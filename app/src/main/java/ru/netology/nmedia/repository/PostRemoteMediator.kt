package ru.netology.nmedia.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import ru.netology.nmedia.api.ApiService
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dao.PostRemoteKeyDao
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.entity.PostRemoteKeyEntity
import ru.netology.nmedia.error.ApiError
import javax.inject.Inject


@OptIn(ExperimentalPagingApi::class)
class PostRemoteMediator @Inject constructor(
    private val apiService: ApiService,
    private val dao: PostDao,
    private val remoteKeyDao: PostRemoteKeyDao,
    private val db: AppDb,
    ) : RemoteMediator<Int, PostEntity>() {

    override suspend fun load(loadType: LoadType, state: PagingState<Int, PostEntity>): MediatorResult {

        try {

            val response = when (loadType) { // имеет три ограниченных варианта: REFRESH, PREPEND,
                // APPEND,- действия, которые хочет произвести пользователь.

                // Обновление страницы
                LoadType.REFRESH -> {
                    if (dao.isEmpty()) apiService.getLatest(state.config.initialLoadSize) else {
                        val id = remoteKeyDao.max() ?: return MediatorResult.Success(false)
                        apiService.getAfter(id, state.config.pageSize)
                    }

                }

                // Пользователь "скроллит вниз".
                LoadType.APPEND -> {
                    val id = remoteKeyDao.min() ?: return MediatorResult.Success(false)
                    apiService.getBefore(id, state.config.pageSize)
                }

                // Пользователь "скроллит вверх".
                LoadType.PREPEND -> {
                    val id = remoteKeyDao.max() ?: return MediatorResult.Success(false)
                    apiService.getAfter(id, state.config.pageSize)
                }
            }


            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }

            val body = response.body() ?: throw ApiError(response.code(), response.message())

            db.withTransaction { // Транзакция. Если произойдёт ошибка в операции с одной БД,
                // то не будет выполнена вся операция. Нужно для того, чтобы при ошибке в выполнении,
                // данные в обоих базах оставались актуальными. Атомарное выполнение.
                when (loadType) {

                    LoadType.REFRESH -> {

                        if (dao.isEmpty()) {
                            remoteKeyDao.insert(
                                listOf(
                                    PostRemoteKeyEntity(
                                        PostRemoteKeyEntity.KeyType.AFTER,
                                        body.first().id,
                                    ),
                                    PostRemoteKeyEntity(
                                        PostRemoteKeyEntity.KeyType.BEFORE,
                                        body.last().id,
                                    ),
                                )
                            )
                        } else {
                            remoteKeyDao.insert(
                                PostRemoteKeyEntity(
                                    PostRemoteKeyEntity.KeyType.AFTER,
                                    body.first().id
                                ),
                            )
                        }

                    }


                    LoadType.APPEND -> {
                        remoteKeyDao.insert(
                            PostRemoteKeyEntity(
                                PostRemoteKeyEntity.KeyType.BEFORE,
                                body.last().id,
                            )
                        )
                    }

                    else -> {}

                }

                dao.insert(body.map(PostEntity::fromDto))
            }

            return MediatorResult.Success(body.isEmpty()
                    || body.size < state.config.pageSize)
        // Передаётся аргумент, достигнут ли конец страницы, или ответ меньше config.pageSize

        } catch (e: Exception) {
            return MediatorResult.Error(e)
        }
    }
}
