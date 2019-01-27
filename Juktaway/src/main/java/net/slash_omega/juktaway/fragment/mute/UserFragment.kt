package net.slash_omega.juktaway.fragment.mute

import android.content.DialogInterface
import net.slash_omega.juktaway.R
import net.slash_omega.juktaway.settings.mute.UserMute

/**
 * Created on 2018/08/31.
 */
internal class UserFragment: MuteFragmentBase<Pair<Long, String>>() {
    override val currentMuteTargets: List<Pair<Long, String>> = UserMute.getAllItems()
    override fun getAdapter(): MuteTargetAdapter<Pair<Long, String>> = object: MuteTargetAdapter<Pair<Long, String>>(activity!!, R.layout.row_word) {
        override val onPositiveButtonClicked: (Pair<Long, String>) -> (DialogInterface, Int) -> Unit = {{ _, _ ->
            remove(it)
            UserMute -= it.first
        }}

        override val onNegativeButtonClicked: (Pair<Long, String>) -> (DialogInterface, Int) -> Unit = {{ _, _ -> }}
        override fun Pair<Long, String>.getDisplayText(): String = "@" + this.second
    }

    override val resource = R.layout.list
    override val listId: Int = R.id.list_list
}

