package net.slash_omega.juktaway

import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlin.coroutines.CoroutineContext

/**
 * Created on 2019/02/03.
 */
abstract class ScopedAppCompatActivity: AppCompatActivity(), CoroutineScope {
    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onDestroy() {
        job.cancelChildren()
        super.onDestroy()
    }
}
