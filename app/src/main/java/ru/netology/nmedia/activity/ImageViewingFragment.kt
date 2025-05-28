package ru.netology.nmedia.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nmedia.BuildConfig
import ru.netology.nmedia.databinding.FragmentPictureViewingBinding
import ru.netology.nmedia.util.StringArg
import ru.netology.nmedia.util.loadAttachments

@AndroidEntryPoint
class ImageViewingFragment : Fragment() {

    companion object {
        var Bundle.textArg2: String? by StringArg
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner) {
            (requireActivity() as AppActivity).hideStatusBar(false)
            findNavController().navigateUp()
        }


        val binding = FragmentPictureViewingBinding.inflate(
            inflater,
            container,
            false
        )

        val url = arguments?.textArg2

        binding.viewImage.loadAttachments("${BuildConfig.BASE_URL}/media/${url}")

        binding.backForPicture.setOnClickListener {
            (requireActivity() as AppActivity).hideStatusBar(false)
            findNavController().navigateUp()
        }


        // Настройка анимации при клике
        binding.apply {

                binding.root.setOnClickListener {

                    if (backForPicture.isVisible) {
                        backForPicture.visibility = View.INVISIBLE
                        oneOfOne.visibility = View.INVISIBLE
                    } else if (backForPicture.isInvisible) {
                        backForPicture.visibility = View.VISIBLE
                        oneOfOne.visibility = View.VISIBLE

                    }
                }

            return binding.root
        }

    }
}






