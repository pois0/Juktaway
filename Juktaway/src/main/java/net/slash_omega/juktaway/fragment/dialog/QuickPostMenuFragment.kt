package net.slash_omega.juktaway.fragment.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.widget.ListView
import net.slash_omega.juktaway.MainActivity
import net.slash_omega.juktaway.R
import net.slash_omega.juktaway.util.ThemeUtil
import org.jetbrains.anko.support.v4.toast

/**
 * Created on 2019/01/20.
 */
class QuickPostMenuFragment: DialogFragment() {
    private lateinit var mainActivity: MainActivity

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        mainActivity = activity!! as MainActivity
        ThemeUtil.setTheme(activity!!)
        return AlertDialog.Builder(mainActivity)
                .setView(ListView(mainActivity).apply {
                    val adapter = MenuAdapter(mainActivity, R.layout.row_menu).apply { createMenu() }
                    setAdapter(adapter)
                    setOnItemClickListener { _, _, i, _ ->
                        adapter.getItem(i)?.callback?.run()
                    }
                })
                .setTitle(getString(R.string.quick_posting_menu_title))
                .create()
    }

    private fun MenuAdapter.createMenu() {
        add(getString(R.string.quick_posting_menu_fix_initial_tweet)) {
            mainActivity.fixStatusInitialText()
            dismiss()
        }

        if(mainActivity.statusInitialText.isNotBlank()) {
            add("Unfix initial text") {
                mainActivity.unfixStatusInitialText()
                toast(getString(R.string.quick_posting_menu_unfixed_success))
                dismiss()
            }
        }
    }
}