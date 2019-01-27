package net.slash_omega.juktaway.model

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import net.slash_omega.juktaway.util.tryAndTraceGet

import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object ImageResizer {

    /**
     * 画像生成
     * 表示サイズ合わせて画像生成時に可能なかぎり縮小して生成します。
     *
     * @param file 画像ファイル
     * @param maxFileSize 最大ファイルサイズ
     * @return 縮小後画像ファイル
     */
    fun compress(file: File, maxFileSize: Long): File = if (file.length() < maxFileSize) file
        else try {
            val tempFile = File.createTempFile(file.name, ".small.jpg")
            val option = BitmapFactory.Options().apply {
                inJustDecodeBounds = false
                inSampleSize = 2
            }
            var bitmap: Bitmap? = BitmapFactory.decodeFile(file.path, option)
            val matrix = getExifMatrix(file)
            if (matrix != null) {
                bitmap = Bitmap.createBitmap(bitmap!!, 0, 0, bitmap.width, bitmap.height, matrix, true)
            }
            for (i in 0..9) {
                bitmap = half(bitmap)
                val out = FileOutputStream(tempFile!!.path)
                bitmap?.compress(Bitmap.CompressFormat.JPEG, 80, out)
                out.close()
                if (tempFile.length() < maxFileSize) break
            }
            tempFile
        } catch (e: IOException) {
            e.printStackTrace()
            file
        }

    private fun getExifMatrix(file: File): Matrix? =
        tryAndTraceGet {
            Matrix().apply {
                when (ExifInterface(file.absolutePath)
                        .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)) {
                    ExifInterface.ORIENTATION_UNDEFINED -> { }
                    ExifInterface.ORIENTATION_NORMAL -> { }
                    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
                    ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
                    ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
                    ExifInterface.ORIENTATION_TRANSVERSE -> {
                        postRotate(-90f)
                        postScale(1f, -1f)
                    }
                    ExifInterface.ORIENTATION_TRANSPOSE -> {
                        postRotate(90f)
                        postScale(1f, -1f)
                    }
                    ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(-90f)
                }
            }
        }

    /**
     * 画像リサイズ
     * @param bitmap 変換対象ビットマップ
     * @return 変換後Bitmap
     */
    private fun half(bitmap: Bitmap?) = bitmap?.run {
        Bitmap.createBitmap(this, 0, 0, width, height, Matrix().apply {
            postScale(0.5f, 0.5f)
        }, false).apply { bitmap.recycle() }
    }
}
