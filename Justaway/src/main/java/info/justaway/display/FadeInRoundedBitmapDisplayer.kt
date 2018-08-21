/*******************************************************************************
 * Copyright 2011-2013 Sergey Tarasevich
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package info.justaway.display

import android.graphics.*
import com.nostra13.universalimageloader.core.assist.LoadedFrom
import com.nostra13.universalimageloader.core.display.BitmapDisplayer
import com.nostra13.universalimageloader.core.imageaware.ImageAware

class FadeInRoundedBitmapDisplayer(val roundPixels: Int): BitmapDisplayer {
    companion object {
        private fun transform(source: Bitmap, radius: Int): Bitmap {
            val margin = 0f
            val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
            Canvas(output).drawRoundRect(RectF(margin, margin, source.width - margin, source.height - margin),
                    radius.toFloat(), radius.toFloat(),
                    Paint().apply {
                        isAntiAlias = true
                        shader = BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
                    })
            return output
        }
    }

    override fun display(bitmap: Bitmap?, imageAware: ImageAware?, loadedFrom: LoadedFrom?) {
        bitmap?.let {
            imageAware?.setImageBitmap( try { transform(it, roundPixels) } catch (e: OutOfMemoryError) { it })
        }
    }
}