package net.slash_omega.juktaway

import android.app.Activity
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import java.io.BufferedReader
import java.io.FileReader
import java.io.PrintWriter

/**
 * Created on 2018/08/29.
 */
class MyUncaughtExceptionHandler(private val context: Context): Thread.UncaughtExceptionHandler {
    companion object {
        private const val BUG_FILE = "BUG"
        private const val MAIL_TO = "mailto:s.aska.org@gmail.com,teshi04@gmail.com"

        private var sPackageInfo: PackageInfo? = null
        private val sMemoryInfo = ActivityManager.MemoryInfo()

        fun showBugReportDialogIfExist(activity: Activity) {
            val bugFile = activity.getFileStreamPath(BUG_FILE)
            if (!bugFile.exists()) return

            val writeFile = activity.getFileStreamPath("$BUG_FILE.txt")
            if (!bugFile.renameTo(writeFile)) return

            val body = StringBuilder()
            var firstLine: String? = null
            try {
                BufferedReader(FileReader(writeFile)).use { r ->
                    r.lineSequence().forEach {
                        if (firstLine == null)
                            firstLine = it
                        else
                            body.append(it).append("\n")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            AlertDialog.Builder(activity).setTitle("バグレポート").setMessage("バグ発生状況を開発者に送信しますか？")
                    .setPositiveButton("送信") { _, _ ->
                        activity.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse(MAIL_TO))
                                .putExtra(Intent.EXTRA_SUBJECT, firstLine)
                                .putExtra(Intent.EXTRA_TEXT, body.toString()))
                    }
                    .setNegativeButton("キャンセル") { _, _ -> }
                    .show()
        }
    }

    private val mDefaultUEH: Thread.UncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()

    init {
        try {
            sPackageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
    }

    override fun uncaughtException(th: Thread, t: Throwable) {
        saveState(t)
        mDefaultUEH.uncaughtException(th, t)
    }

    private fun saveState(error: Throwable) {
        try {
            val writer = PrintWriter(context.openFileOutput(BUG_FILE, Context.MODE_PRIVATE))
            sPackageInfo?.let {
                writer.printf("[BUG][%s] versionName:%s, versionCode:%d\n",
                        it.packageName, it.versionName, it.versionCode)
            } ?: writer.printf("[BUG][Unknown]\n")

            writer.printf("Runtime Memory: total: %dKB, free: %dKB, used: %dKB\n",
                    Runtime.getRuntime().totalMemory() / 1024,
                    Runtime.getRuntime().freeMemory() / 1024,
                    (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024)
            (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(sMemoryInfo)
            writer.printf("availMem: %dKB, lowMemory: %b\n", sMemoryInfo.availMem / 1024, sMemoryInfo.lowMemory)
            writer.printf("DEVICE: %s\n", Build.DEVICE)
            writer.printf("MODEL: %s\n", Build.MODEL)
            writer.printf("VERSION.SDK: %s\n", Build.VERSION.SDK_INT)
            writer.println("")
            error.printStackTrace(writer)
            writer.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}