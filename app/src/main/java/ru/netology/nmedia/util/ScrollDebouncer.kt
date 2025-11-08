package ru.netology.nmedia.util

import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ScrollDebouncer(
    private val scrollDebounce: Long,
    private val scrollThreshold: Int,
    private val postsFabIsVisible: () -> Boolean,
    private val newerPostsFab: ExtendedFloatingActionButton,
    private val fab: FloatingActionButton,
) : RecyclerView.OnScrollListener() {

    private var lastScrollTime = 0L

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {

        val currentTime = System.currentTimeMillis()

        if (currentTime - lastScrollTime < scrollDebounce) return

        lastScrollTime = currentTime

        when {
            dy > scrollThreshold -> {
                newerPostsFab.animate()
                    .translationY(-newerPostsFab.height * 1.35f)
                    .setDuration(250)
                    .start()

                fab.animate()
                    .translationY(fab.height * 2f)
                    .setDuration(300)
                    .start()

            }

            dy < -scrollThreshold -> {

                newerPostsFab.isVisible = postsFabIsVisible()

                newerPostsFab.animate()
                    .translationY(0f)
                    .setDuration(200)
                    .start()

                fab.animate()
                    .translationY(0f)
                    .setDuration(200)
                    .start()

            }
        }
    }
}
