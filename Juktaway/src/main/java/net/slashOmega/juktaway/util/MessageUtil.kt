package net.slashOmega.juktaway.util

import android.app.ProgressDialog
import android.content.Context
import android.widget.Toast
import net.slashOmega.juktaway.JuktawayApplication
import org.jetbrains.anko.toast

object MessageUtil {
    private var sProgressDialog: ProgressDialog? = null

    fun showToast(text: String) {
        val application = JuktawayApplication.app
        Toast.makeText(application, text, Toast.LENGTH_SHORT).show()
    }

    fun showToast(id: Int) {
        JuktawayApplication.app.toast(id)
    }

    fun showToast(id: Int, description: String) {
        val application = JuktawayApplication.app
        val text = application.getString(id) + "\n" + description
        Toast.makeText(application, text, Toast.LENGTH_SHORT).show()
    }

    fun showProgressDialog(context: Context, message: String) {
        sProgressDialog = ProgressDialog(context)
        sProgressDialog!!.setMessage(message)
        sProgressDialog!!.show()
    }

    fun dismissProgressDialog() {
        if (sProgressDialog != null)
            try {
                sProgressDialog!!.dismiss()
            } finally {
                sProgressDialog = null
            }
    }
}
