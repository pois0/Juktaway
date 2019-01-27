package net.slash_omega.juktaway.util

import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object FileUtil {
    fun writeToTempFile(cacheDir: File, ins: InputStream): File? =
        if (!cacheDir.exists() && !cacheDir.mkdirs()) null
        else tryAndTraceGet {
            File(cacheDir, "juktaway-temp-" + System.currentTimeMillis() + ".jpg").also {
                FileOutputStream(it).use { os -> ins.use { ins -> ins.copyTo(os) } }
            }
        }
}