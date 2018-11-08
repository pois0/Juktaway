package net.slashOmega.juktaway.fragment.mute

import android.content.DialogInterface
import net.slashOmega.juktaway.R
import net.slashOmega.juktaway.settings.MuteSettings
import net.slashOmega.juktaway.settings.mute.SourceMute

/**
 * Created on 2018/08/31.
 */
internal class SourceFragment: MuteFragmentBase<String>() {
    override val listId: Int = R.id.list_list
    override val currentMuteTargets: List<String> = SourceMute.getAllItems()
    override fun getAdapter() = object: MuteTargetAdapter<String>(activity!!, R.layout.row_word) {
        override fun String.getDisplayText(): String = this
        override val onPositiveButtonClicked: (String) -> (DialogInterface, Int) -> Unit = { { _, _ ->
            remove(it)
            SourceMute -= it
        }}
        override val onNegativeButtonClicked: (String) -> (DialogInterface, Int) -> Unit = {{_,_->}}
    }

    override val resource: Int = R.layout.list
}