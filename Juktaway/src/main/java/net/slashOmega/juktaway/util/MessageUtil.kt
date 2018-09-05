package net.slashOmega.juktaway.util

import android.app.ProgressDialog
import android.content.Context
import android.widget.Toast
import net.slashOmega.juktaway.JustawayApplication

object MessageUtil {
    private var sProgressDialog: ProgressDialog? = null

    fun showToast(text: String) {
        val application = JustawayApplication.app
        Toast.makeText(application, text, Toast.LENGTH_SHORT).show()
    }

    fun showToast(id: Int) {
        val application = JustawayApplication.app
        val text = application.getString(id)
        Toast.makeText(application, text, Toast.LENGTH_SHORT).show()
    }

    fun showToast(id: Int, description: String) {
        val application = JustawayApplication.app
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
