package ru.netology.nmedia.util

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat.getString
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment.Companion.findNavController
import ru.netology.nmedia.R

object AndroidUtils {

    fun hideKeyboard(view: View) {
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun dialogBuilder(forPosts: Boolean = false, forLikes: Boolean = false, context: Context, fragment: Fragment) {

        val appName = getString(context, R.string.app_name)

        val writePosts = "Чтобы иметь возможность писать посты, войдите в $appName."

        val putLikes = "Чтобы иметь возможность ставить лайки, войдите в $appName."

        val standardPhrase =
            "Чтобы иметь возможность пользоваться всеми функциями, войдите в $appName."

        val phrase = when {
            forPosts -> writePosts
            forLikes -> putLikes
            else -> standardPhrase
        }

        val dialogBuilder = AlertDialog.Builder(context)

        dialogBuilder.setMessage(phrase)
            .setCancelable(false) // Если установить значение false, то пользователь
//          не сможет закрыть диалоговое окно, например, нажатием в любой точке экрана.
//          В таком случае пользователь должен нажать одну из предоставленных опций.

            .setPositiveButton(getString(context, R.string.entry)) { dialog, _ ->
                findNavController(fragment)
                    .navigate(R.id.action_feedFragment_to_application_login_fragment)
                dialog.cancel()
            }
            .setNegativeButton(getString(context, R.string.cancel)) { dialog, _ ->
                dialog.cancel()
            }
        val alert = dialogBuilder.create()

        alert.setTitle("Вход в -FeedPosts-")

        alert.setIcon(R.drawable.ic_netology_48dp)

        alert.show()
    }

}