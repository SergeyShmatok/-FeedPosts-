package ru.netology.nmedia.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Build.VERSION_CODES.TIRAMISU
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.flowWithLifecycle
import androidx.navigation.findNavController
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import ru.netology.nmedia.R
import ru.netology.nmedia.activity.NewPostFragment.Companion.textArg1
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.viewmodel.AuthViewModel
import javax.inject.Inject


@AndroidEntryPoint // Из вебинара...dagger сделает так, что мы будем наследоваться не от
// AppCompatActivity, а от DaggerAppComp... У которого в super в onCreate будет вызов Inject,
// который доставит все зависимости.
class AppActivity: AppCompatActivity(R.layout.activity_app) {


    @Inject
    lateinit var appAuth: AppAuth

    @Inject
    lateinit var googleApiAvailability: GoogleApiAvailability

    @Inject
    lateinit var firebaseMessaging: FirebaseMessaging

    val authViewModel by viewModels<AuthViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestNotificationsPermission()

        checkGoogleApiAvailability()

//        if (Build.VERSION.SDK_INT > TIRAMISU) {
//            findViewById<View>(R.id.top_inset).visibility = View.VISIBLE
////            binding.topInset.visibility = View.VISIBLE
//        }
//        WindowCompat.setDecorFitsSystemWindows(window, false)

                intent?.let {
            if (it.action != Intent.ACTION_SEND) {
                return@let
            }

            val text = it.getStringExtra(Intent.EXTRA_TEXT)

            if (text?.isNotBlank() != true) {
                return@let
            }

            intent.removeExtra(Intent.EXTRA_TEXT)
            findNavController(R.id.nav_host_fragment)
                .navigate(
                    R.id.action_feedFragment_to_newPostFragment,
                    Bundle().apply {
                        textArg1 = text
                    }
                )

        }


//----------------------------------------- MenuProvider -------------------------------------------

        addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_auth, menu)
                authViewModel.state.flowWithLifecycle(this@AppActivity.lifecycle).onEach {


                menu.setGroupVisible(R.id.authorized, authViewModel.isAuthenticated)
                    menu.setGroupVisible(R.id.unauthorized, !authViewModel.isAuthenticated)

                }.launchIn(this@AppActivity.lifecycle.coroutineScope)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
                when (menuItem.itemId){
                    R.id.signIn -> {
//                        версия findNavController'а для Activity (не Fragment).
                        findNavController(R.id.nav_host_fragment)
                            .navigate(R.id.application_login_fragment)

                        true
                    }

                    R.id.logout -> {
                        appAuth.removeAuth()
                        true
                    }

                    else -> false

                }
        })
    }

//------------------------------------- Системные панели и пр. -----------------------------------

  private fun windowInsetsController() = WindowCompat
        .getInsetsController(window, window.decorView)

    fun hideStatusBar(hide: Boolean) {

        if (hide) {
            windowInsetsController().hide(WindowInsetsCompat.Type.statusBars())
            } else {
            windowInsetsController().show(WindowInsetsCompat.Type.statusBars())
            }
        }

//--------------------------------------------------------------------------------------------------


    private fun requestNotificationsPermission() {
        if (Build.VERSION.SDK_INT < TIRAMISU) {
            return
        }

        val permission = Manifest.permission.POST_NOTIFICATIONS

        if (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
            return
        }

        requestPermissions(arrayOf(permission), 1)
    }     // запрос доступа к уведомлениям


    private fun checkGoogleApiAvailability() {
        with(googleApiAvailability) {
            val code = isGooglePlayServicesAvailable(this@AppActivity)
            if (code == ConnectionResult.SUCCESS) {
                return@with
            }
            if (isUserResolvableError(code)) {
                getErrorDialog(this@AppActivity, code, 9000)?.show()
                return
            }
            Toast.makeText(this@AppActivity, R.string.google_play_unavailable, Toast.LENGTH_LONG)
                .show()
        }

        firebaseMessaging.token.addOnSuccessListener {
            println(it)
        }
    }
}

//--------------------------------------------------------------------------------------------------

