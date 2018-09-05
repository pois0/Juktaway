package net.slashOmega.juktaway.fragment.mute

import android.content.DialogInterface
import net.slashOmega.juktaway.R
import net.slashOmega.juktaway.settings.MuteSettings

/**
 * Created on 2018/08/31.
 */
internal class UserFragment: MuteFragmentBase<Pair<Long, String>>() {
    override val currentMuteTargets: List<Pair<Long, String>> = MuteSettings.getUserMap().toList()
    override fun getAdapter(): MuteTargetAdapter<Pair<Long, String>> = object: MuteTargetAdapter<Pair<Long, String>>(activity!!, R.layout.row_word) {
        override val onPositiveButtonClicked: (Pair<Long, String>) -> (DialogInterface, Int) -> Unit = {{ _, _ ->
            remove(it)
            MuteSettings.removeUser(it.first)
            MuteSettings.saveMuteSettings()
        }}

        override val onNegativeButtonClicked: (Pair<Long, String>) -> (DialogInterface, Int) -> Unit = {{ _, _ -> }}
        override fun Pair<Long, String>.getDisplayText(): String = "@" + this.second
    }

    override val resource = R.layout.list
    override val listId: Int = R.id.list_list
}

