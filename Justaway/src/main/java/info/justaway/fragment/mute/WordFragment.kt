//package info.justaway.fragment.mute
//
//import android.app.AlertDialog
//import android.content.DialogInterface
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.Button
//import android.widget.EditText
//import android.widget.TextView
//import info.justaway.R
//import info.justaway.settings.MuteSettings
//import info.justaway.util.KeyboardUtil
//import info.justaway.util.MessageUtil
//
///**
// * Created on 2018/08/31.
// */
//internal class WordFragment: MuteFragmentBase<String>() {
//    override val currentMuteTargets: List<String> = MuteSettings.getWords()
//    override fun getAdapter() = object: MuteTargetAdapter<String>(activity!!, R.layout.row_word) {
//        override fun String.getDisplayText(): String = this
//
//        override val onPositiveButtonClicked: (String) -> (DialogInterface, Int) -> Unit = { { _, _ ->
//            remove(it)
//            MuteSettings.removeWord(it)
//            MuteSettings.saveMuteSettings()
//        }}
//        override val onNegativeButtonClicked: (String) -> (DialogInterface, Int) -> Unit = {{_,_->}}
//    }
//
//    override val resource = R.layout.fragment_mute_word
//
//    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
//        return super.onCreateView(inflater, container, savedInstanceState)?.apply {
//            findViewById<TextView>(R.id.button_add).setOnClickListener { _ ->
//                val editText = EditText(activity)
//                KeyboardUtil.showKeyboard(editText)
//                AlertDialog.Builder(activity)
//                        .setTitle(R.string.title_create_mute_word)
//                        .setView(editText)
//                        .setPositiveButton(R.string.button_save) { _, _ ->
//                            editText.text.toString().takeIf { it.isEmpty() }?.let { word ->
//                                mAdapter.add(word)
//                                MuteSettings.addWord(word)
//                                MuteSettings.saveMuteSettings()
//                                MessageUtil.showToast(R.string.toast_create_mute)
//                            }
//                        }
//                        .setNegativeButton(R.string.button_cancel) {_,_->}
//                        .show()
//            }
//        }
//    }
//}