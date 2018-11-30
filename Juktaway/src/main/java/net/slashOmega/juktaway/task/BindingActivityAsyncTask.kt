package net.slashOmega.juktaway.task

import android.os.AsyncTask
import java.lang.ref.WeakReference

/**
 * Created on 2018/09/01.
 */
internal abstract class ReferenceAsyncTask<A, B, C, D>(obj: D): AsyncTask<A, B, C>() {
    internal val ref by lazy{ WeakReference(obj) }
}