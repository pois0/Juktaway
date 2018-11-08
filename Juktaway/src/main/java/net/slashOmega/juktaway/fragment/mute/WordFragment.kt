package net.slashOmega.juktaway.fragment.mute

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import net.slashOmega.juktaway.R
import net.slashOmega.juktaway.settings.MuteSettings
import net.slashOmega.juktaway.settings.mute.WordMute
import net.slashOmega.juktaway.util.KeyboardUtil
import net.slashOmega.juktaway.util.MessageUtil

/**
 * Created on 2018/08/31.
 */
internal class WordFragment: MuteFragmentBase<String>() {
    override val listId: Int = R.id.list
    override val currentMuteTargets: List<String> = WordMute.getAllItems()
    override fun getAdapter() = object: MuteTargetAdapter<String>(activity!!, R.layout.row_word) {
        override fun String.getDisplayText(): String = this

        override val onPositiveButtonClicked: (String) -> (DialogInterface, Int) -> Unit = { { _, _ ->
            remove(it)
            WordMute -= it
        }}
        override val onNegativeButtonClicked: (String) -> (DialogInterface, Int) -> Unit = {{_,_->}}
    }

    override val resource = R.layout.fragment_mute_word

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return super.onCreateView(inflater, container, savedInstanceState)?.apply {
            findViewById<TextView>(R.id.button_add).setOnClickListener { _ ->
                val editText = EditText(activity)
                KeyboardUtil.showKeyboard(editText)
                AlertDialog.Builder(activity)
                        .setTitle(R.string.title_create_mute_word)
                        .setView(editText)
                        .setPositiveButton(R.string.button_save) { _, _ ->
                            editText.text.toString().takeIf { it.isEmpty() }?.let { word ->
                                mAdapter.add(word)
                                WordMute += word
                                MessageUtil.showToast(R.string.toast_create_mute)
                            }
                        }
                        .setNegativeButton(R.string.button_cancel) {_,_->}
                        .show()
            }
        }
    }
}