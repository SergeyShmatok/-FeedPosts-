package ru.netology.nmedia.error

import android.database.SQLException
import java.io.IOException

// sealed классы в явном виде перечисляют классы, которым разрешено быть
// их дочерними классами. Такой контроль дает нам больше уверенности в том,
// как именно тот или иной класс будет использоваться в нашем коде.


 sealed class AppError(var code: String) : RuntimeException() {
    companion object {
        fun from(e: Throwable): AppError = when (e) {
            is AppError -> e
            is SQLException -> DbError
            is IOException -> NetworkError
            else -> UnknownError
        }
    }
}

class ApiError(val status: Int, code: String) : AppError(code)

data object NetworkError : AppError("error_network")
data object DbError : AppError("error_db")
data object UnknownError : AppError("error_unknown")
