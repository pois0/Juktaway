package net.slash_omega.juktaway.widget

import android.content.Context
import android.support.v4.view.ViewPager
import android.util.AttributeSet
import android.view.MotionEvent

class ScaleImageViewPager(context: Context, attrs: AttributeSet) : ViewPager(context, attrs) {

    private var mEnabled = true

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return if (ScaleImageView.sBounds) {
            super.onTouchEvent(event)
        } else false
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        if (ScaleImageView.sBounds) {
            try {
                mEnabled = super.onInterceptTouchEvent(event)
            } catch (e: Exception) {
                //
            }

            return mEnabled
        }
        return false
    }
}
