package ru.netology.nmedia.db

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ru.netology.nmedia.dao.PostDao
import javax.inject.Singleton

//--------------------------------------------------------------------------------------------------

// Надо указать на каком уровне будет использоваться зависимость (есть разные уровни, приложения,
// активити, вью-модели..)
@InstallIn(SingletonComponent::class) // База данных идёт на всё приложение.
// SingletonComponent::class потому что в единственно числе.
@Module
class DbModule {

    @Singleton // Так же можно указать сколько должен жить данный объект. - База данных будет создана
    // один раз на всё приложение.
    @Provides // Если нужно создать экземпляр объекта нужно использовать аннотацию @Provides.
    fun provideDb( // Слева иконка указывает на связь объектов ("граф" зависимости).
        context: Context // Иконка слева говорит, что данный context уже кто-то предоставил:
    ): AppDb = Room.databaseBuilder(context, AppDb::class.java, "app.db") //
        .fallbackToDestructiveMigration(true) // Для миграции (**)
        // .allowMainThreadQueries() - чтобы можно было работать с Room
        //  на главном потоке (больше не нужно).
        .build()

//--------------------------------------------------------------------------------------------------

    @Provides
    fun providePostDao(
        appDb: AppDb
    ): PostDao = appDb.postDao() // Через это обращаемся к функциям прописанным в интерфейсе PostDao

//--------------------------------------------------------------------------------------------------

    @Provides
    fun provideContext (
        // Чтобы передать контекст в параметры.
        // Dagger Hilt уже всё сделал и теперь не надо забивать этим голову.- Ответственность за это
        // на библиотеке.
        @ApplicationContext
        applicationContext: Context) = applicationContext

}


// @Module как вы могли догадаться, помечает этот класс как модуль, то есть класс, который
// предоставляет зависимости другим классам в области действия своего компонента.

// @InstallIn(SingletonComponent::class) означает, что зависимости модуля будут храниться в
// SingletonComponent и станут доступны во всём коде приложения.

// SingletonComponent — компонент верхнего уровня, который позволяет модулю внедрять зависимости
// по всему приложению.