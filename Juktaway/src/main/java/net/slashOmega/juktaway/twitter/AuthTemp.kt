package net.slashOmega.juktaway.twitter

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
        ckTemp = ""
        csTemp = ""
        rtTemp = ""
        rtsTemp = ""
    }
}