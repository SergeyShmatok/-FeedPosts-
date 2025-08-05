package ru.netology.nmedia.util

import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.dto.TimingSeparator
import ru.netology.nmedia.dto.TimingSeparatorType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject


class DateSeparator @Inject constructor() {

    private val today = LocalDate.now() // Получает текущую дату из системных часов в часовом поясе

    // по умолчанию. Это запросит системные часы в часовом поясе по умолчанию, чтобы получить
    // текущую дату. Использование этого метода предотвратит возможность использования
    // альтернативных часов для тестирования, поскольку часы жестко закодированы.
    // Возвращает: текущую дату с использованием системных часов и часового пояса по умолчанию,
    // а не null.
    private val yesterday = today.minusDays(1)

    // Возвращает копию этого LocalDate с указанным количеством вычтенных дней.
    private val thisWeek = today.minusDays(2)

    private val weekAgo = today.minusDays(7)

    fun create(previous: Post?, next: Post?): TimingSeparator? =

        when {
            !previous.isToday() && next.isToday() -> {
                TimingSeparatorType.TODAY
            }

            !previous.isYesterday() && next.isYesterday() -> {
                TimingSeparatorType.YESTERDAY
            }

            !previous.isThisWeek() && next.isThisWeek() -> {
                TimingSeparatorType.THIS_WEEK
            }

            !previous.isWeekAgo() && next.isWeekAgo() -> {
                TimingSeparatorType.WEEK_AGO
            }

            else -> {
                null
            }
        }
            ?.let(::TimingSeparator)


    private val Post.publishedDate: LocalDate
        get() = Instant.ofEpochSecond(published.toLong()) // получается Instant из "цифры"
            .atZone(ZoneId.systemDefault()) // располагается на временной зоне
            .toLocalDate() // обрезали всё лишнее - Г/М/Д


    private fun Post?.isToday(): Boolean = this?.publishedDate?.let { it == today } ?: false
    private fun Post?.isYesterday(): Boolean = this?.publishedDate?.let { it == yesterday } ?: false
    private fun Post?.isThisWeek(): Boolean = this?.publishedDate?.let { it <= thisWeek && it > weekAgo } ?: false
    private fun Post?.isWeekAgo(): Boolean = this?.publishedDate?.let { it <= weekAgo } ?: false


}