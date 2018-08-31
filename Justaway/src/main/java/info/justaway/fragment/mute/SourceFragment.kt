package info.justaway.fragment.mute

import android.content.DialogInterface
import info.justaway.R
import info.justaway.settings.MuteSettings

/**
 * Created on 2018/08/31.
 */
internal class SourceFragment: MuteFragmentBase<String>() {
    override val listId: Int = R.id.list_list
    override val currentMuteTargets: List<String> = MuteSettings.getSources()
    override fun getAdapter() = object: MuteTargetAdapter<String>(activity!!, R.layout.row_word) {
        override fun String.getDisplayText(): String = this
        override val onPositiveButtonClicked: (String) -> (DialogInterface, Int) -> Unit = { { _, _ ->
            remove(it)
            MuteSettings.removeSource(it)
            MuteSettings.saveMuteSettings()
        }}
        override val onNegativeButtonClicked: (String) -> (DialogInterface, Int) -> Unit = {{_,_->}}
    }

    override val resource: Int = R.layout.list
}