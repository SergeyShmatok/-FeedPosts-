package ru.netology.nmedia.activity

import android.content.Intent
import android.os.Build
import android.os.Build.VERSION_CODES.TIRAMISU
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import ru.netology.nmedia.R
import ru.netology.nmedia.activity.ImageViewingFragment.Companion.textArg2
import ru.netology.nmedia.adapter.InteractionListener
import ru.netology.nmedia.adapter.OnInteractionListener
import ru.netology.nmedia.adapter.PostLoadingStateAdapter
import ru.netology.nmedia.adapter.PostsAdapter
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dao.PostRemoteKeyDao
import ru.netology.nmedia.databinding.FragmentFeedBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entity.PostRemoteKeyEntity
import ru.netology.nmedia.util.AndroidUtils.dialogBuilder
import ru.netology.nmedia.util.ItemAnimatorForAd
import ru.netology.nmedia.util.ScrollDebouncer
import ru.netology.nmedia.util.viewLifecycle
import ru.netology.nmedia.util.viewLifecycleScope
import ru.netology.nmedia.viewmodel.AuthViewModel
import ru.netology.nmedia.viewmodel.PostViewModel
import javax.inject.Inject

@AndroidEntryPoint
class FeedFragment : Fragment() {

    private companion object {

        const val SCROLL_DEBOUNCE = 500L
        const val SCROLL_DELAY = 5500L
        const val SCROLL_THRESHOLD = 10
        const val TOP_INSET_MARGIN = 240

    }

    @Inject
    lateinit var dao: PostDao

    @Inject
    lateinit var remoteKeyDao: PostRemoteKeyDao

    private val viewModel: PostViewModel by activityViewModels()

    private val authViewModel by viewModels<AuthViewModel>()

    lateinit var appActivity: AppActivity

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val binding = FragmentFeedBinding.inflate(inflater, container, false)

        if (Build.VERSION.SDK_INT > TIRAMISU) {

            binding.topInset.visibility = View.VISIBLE

            val fab = binding.newerPostsFab

            val marginParams = fab.layoutParams as ViewGroup.MarginLayoutParams

            marginParams.setMargins(0, TOP_INSET_MARGIN, 0, 0)

            fab.layoutParams = marginParams

        }

        val adapter = PostsAdapter(
            object : OnInteractionListener {

                override fun onEdit(post: Post) {
                    viewModel.edit(post)
                }

                override fun onLike(post: Post) {

                    if (authViewModel.isAuthenticated) {
                        if (!post.likedByMe) viewModel.likeById(post.id)
                        else viewModel.removeLike(post.id)
                    } else {
                        dialogBuilder(forPosts = false, true, requireContext(), this@FeedFragment)
                    }
                }


                override fun onRemove(post: Post) {
                    viewModel.removeById(post.id)
                }


                override fun onShare(post: Post) {
                    val intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, post.content)
                        type = "text/plain"
                    }

                    val shareIntent =
                        Intent.createChooser(intent, getString(R.string.chooser_share_post))
                    startActivity(shareIntent)
                }

                override fun openPhoto(post: Post) {

                    findNavController().navigate(
                        R.id.action_feedFragment_to_imageViewingFragment,
                        Bundle().apply { textArg2 = post.attachment?.url })
                }

            },
        )

        binding.list.itemAnimator = ItemAnimatorForAd()


        binding.list.adapter = adapter.withLoadStateHeaderAndFooter(
            header = PostLoadingStateAdapter(object : InteractionListener {
                override fun onRetry() {
                    adapter.retry()
                }

            }),
            footer = PostLoadingStateAdapter(object : InteractionListener {
                override fun onRetry() {
                    adapter.retry()
                }
            }),
        )

        viewModel.pagingData.flowWithLifecycle(viewLifecycle).onEach { pagingData ->

            adapter.submitData(pagingData)

        }.launchIn(viewLifecycleScope)

        viewModel.dataState.flowWithLifecycle(viewLifecycle, Lifecycle.State.STARTED).onEach { stateModel ->
            binding.progress.isVisible = stateModel.loading
            binding.swiperefresh.isRefreshing = stateModel.refreshing

            if (stateModel.error) { // Модель состояния: - ошибка -
                Snackbar.make(binding.root, R.string.error_loading, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.retry_loading) {
                        adapter.refresh()
                    }.show()
            }

            if (stateModel.likeError) { // Лайк не поставился
                viewModel.toastFun()
                viewModel.cleanModel()
            }

            if (!stateModel.postIsDeleted) { // Пост не удалился
                viewModel.toastFun()
                viewModel.cleanModel()
            }

        }.launchIn(viewLifecycleScope)



        viewLifecycleOwner.lifecycleScope.launch { // Объединение 'связанных' Flow
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.newerCount.collectLatest {
                        println("Новые посты: $it")
                        binding.newerPostsFab.text = getString(R.string.extended_fab_text)
                            .format("$it")

                        launch {
                            viewModel.newPostData.collectLatest { posts ->
                                binding.newerPostsFab.isVisible =
                                    !posts.isNullOrEmpty() && it.toString() != "0"
                            }
                        }

                    }
                }


            }
        }



        binding.newerPostsFab.setOnClickListener {
            viewModel.newPostsIsVisible()
            binding.list.smoothScrollToPosition(0)

            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                remoteKeyDao.insert(
                    PostRemoteKeyEntity(
                        PostRemoteKeyEntity.KeyType.AFTER,
                        dao.takeLastId(),
                    )
                )

            }
        }


        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                adapter.loadStateFlow.collectLatest { state ->
                    binding.swiperefresh.isRefreshing =
                        state.refresh is LoadState.Loading
                }
            }
        }


        authViewModel.refreshEvents.flowWithLifecycle(viewLifecycle, Lifecycle.State.STARTED)
            .onEach { // Обновление списка при раз/авторизации
                viewModel.cleanNewPost()
                adapter.refresh()
                authViewModel.onRefresh()

            }.launchIn(viewLifecycleScope)


        binding.swiperefresh.setOnRefreshListener {
            viewModel.toastFun(true)
            viewModel.cleanNewPost()
            adapter.refresh()
            viewLifecycleOwner.lifecycleScope.launch {
                delay(SCROLL_DELAY)
                binding.list.smoothScrollToPosition(0)
            }

        }


        binding.fab.setOnClickListener {
            if (authViewModel.isAuthenticated) findNavController()
                .navigate(R.id.action_feedFragment_to_newPostFragment)
            else {
                dialogBuilder(forPosts = true, false, requireContext(), this)
            }

        }

            binding.list.addOnScrollListener(ScrollDebouncer(
            scrollDebounce = SCROLL_DEBOUNCE,
            scrollThreshold = SCROLL_THRESHOLD,
            postsFabIsVisible = { viewModel.newPostData.value?.isNotEmpty() ?: false },
            newerPostsFab = binding.newerPostsFab,
            fab = binding.fab,
        ))

        return binding.root
    }


    override fun onCreate(savedInstanceState: Bundle?) {

        appActivity = requireActivity() as AppActivity

        fun colorSetter(resId: Int) = AppCompatResources.getDrawable(requireContext(), resId)
        // "Слушатель" навигации
        findNavController().addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.imageViewingFragment -> {
                    appActivity.apply {
                        supportActionBar?.setBackgroundDrawable(colorSetter(R.color.black))
                        supportActionBar?.hide()
                        hideStatusBar(true)
                    }
                }

                R.id.application_login_fragment -> {
                    appActivity.apply {
                        supportActionBar?.hide()
                    }

                }

                else -> {
                    appActivity.apply {
                        supportActionBar?.setBackgroundDrawable(colorSetter(R.color.colorPrimary))
                        supportActionBar?.show()
                    }
                }
            }
        }

        super.onCreate(savedInstanceState)

    }

}
