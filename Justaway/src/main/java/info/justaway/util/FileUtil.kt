package info.justaway.util

import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object FileUtil {
    fun writeToTempFile(cacheDir: File, ins: InputStream): File? {
        return if (!cacheDir.exists() && !cacheDir.mkdirs()) null
            else try {
                File(cacheDir, "justaway-temp-" + System.currentTimeMillis() + ".jpg").also {
                    FileOutputStream(it).use { os -> ins.use { ins -> ins.copyTo(os) }}
                }
            } catch (e: Exception) { null }
    }
}