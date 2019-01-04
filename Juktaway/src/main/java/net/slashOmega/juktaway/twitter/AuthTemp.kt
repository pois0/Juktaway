package net.slashOmega.juktaway.twitter

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.slashOmega.juktaway.util.SharedPreference

/**
 * Created on 2018/12/23.
 */

internal var ckTemp by SharedPreference("twitter", "cktemp", "")
internal var csTemp by SharedPreference("twitter", "cstemp", "")
internal var rtTemp by SharedPreference("twitter", "rtTemp", "")
internal var rtsTemp by SharedPreference("twitter", "rtTemp", "")
object AuthTemp {
    suspend fun clearTemps() {
        withContext(Dispatchers.Default) {
            ckTemp = ""
            csTemp = ""
            rtTemp = ""
            rtsTemp = ""
        }
    }
}