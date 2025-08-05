package ru.netology.nmedia.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.core.net.toFile
import androidx.core.view.MenuProvider
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.navigation.fragment.findNavController
import com.github.dhaval2404.imagepicker.ImagePicker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.FragmentNewPostBinding
import ru.netology.nmedia.util.AndroidUtils
import ru.netology.nmedia.util.StringArg
import ru.netology.nmedia.util.viewLifecycle
import ru.netology.nmedia.util.viewLifecycleScope
import ru.netology.nmedia.viewmodel.PostViewModel

@AndroidEntryPoint
class NewPostFragment : Fragment() {

    companion object {
        var Bundle.textArg1: String? by StringArg
        const val MAX_PHOTO_SIZE_PX = 2048
    }

    private val viewModel: PostViewModel by activityViewModels()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val binding = FragmentNewPostBinding.inflate(
            inflater,
            container,
            false
        )

        arguments?.textArg1
            ?.let(binding.edit::setText)

//--------------------------------------------------------------------------------------------------

        requireActivity().addMenuProvider( // Принимает MenuProvider и LifecycleOwner
            object : MenuProvider {   // интерфейс, который включает всё необходимое для работы с menu
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.new_post, menu)
                // первым параметром передаёт XML меню, вторым объект меню
                // после вызова inflate заполнит данными объект "menu"
                    menu.setGroupVisible(R.id.authorized, false)
                    menu.setGroupVisible(R.id.unauthorized, false)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
                   if (menuItem.itemId == R.id.newPost) { // проверяем по id, что было 'нажато'
                       viewModel.changeContent(binding.edit.text.toString()) // обработка нажатия
                       viewModel.save() // -/-
                       AndroidUtils.hideKeyboard(requireView()) // -/-
                       true
                   } else
                       false

            }, viewLifecycleOwner) // - если не указать, то пункты меню,
            // добавленные в этом фрагменте, "перейдут" в другие.

//--------------------------------------------------------------------------------------------------

        viewModel.postCreated.flowWithLifecycle(viewLifecycle).onEach {

            findNavController().navigateUp()
            viewModel.postCreatedIsNull() // Чтобы отработал как лайв-ивент (надо "обнулить" значение)

        }.launchIn(viewLifecycleScope)



        viewModel.dataState.flowWithLifecycle(viewLifecycle).onEach { stateModel ->
            if (!stateModel.postIsAdded) {
                viewModel.toastFun()
                viewModel.cleanModel()
            }
        }.launchIn(viewLifecycleScope)



        viewModel.photo.flowWithLifecycle(viewLifecycle).onEach { photo ->

            if (photo == null) {
                binding.previewContainer.isGone = true
                return@onEach
            }

            binding.previewContainer.isVisible = true
            binding.preview.setImageURI(photo.uri)

        }.launchIn(viewLifecycleScope)

//--------------------------------------------------------------------------------------------------

        val photoLauncher = registerForActivityResult(StartActivityForResult()) {result ->
            if (result.resultCode == ImagePicker.RESULT_ERROR) {
                viewModel.toastFun(pickError = true)
                return@registerForActivityResult
            }

         val uri = result.data?.data ?: return@registerForActivityResult // если код ответа не ошибка,
             // то (всё равно) что-то должно сохраниться.

             // Метод getData() позволяет получить данные, с которыми работает намерение. Возвращает URI
             // (универсальный идентификатор ресурса) данных, на которые направлено намерение,
             // или значение null.

            viewModel.changePhoto(uri, uri.toFile())
                // toFile() создаёт файл из указанного Uri. Обратите внимание, что это вызовет исключение
                // IllegalArgumentException при вызове на Uri, в котором отсутствует схема файла.
        }

        binding.takePhoto.setOnClickListener {
            ImagePicker.Builder(this)
                .crop()
                .cameraOnly()
                .maxResultSize(MAX_PHOTO_SIZE_PX, MAX_PHOTO_SIZE_PX)
                .createIntent(photoLauncher::launch)
                        // photoLauncher.launch(it)
        }

        binding.pickPhoto.setOnClickListener {
            ImagePicker.Builder(this)
                .crop()
                .galleryOnly()
                .maxResultSize(MAX_PHOTO_SIZE_PX, MAX_PHOTO_SIZE_PX)
                .createIntent(photoLauncher::launch)
        }

        binding.deleteButton.setOnClickListener {
            viewModel.removePhoto()
        }


        return binding.root
    }
}
