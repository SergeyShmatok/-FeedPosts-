package ru.netology.nmedia.util

import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import kotlinx.coroutines.CoroutineScope


val Fragment.viewLifecycleScope: CoroutineScope
    get() = viewLifecycle.coroutineScope


val Fragment.viewLifecycle: Lifecycle
    get() = viewLifecycleOwner.lifecycle // Если во фрагменте будет просто lifecycle
    // без viewLifecycleOwner, то ссылки останутся висеть со всеми вытекающими (утечкой памяти и пр.)

