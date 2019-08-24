package net.slash_omega.juktaway.widget

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.drawable.Drawable
import android.support.v7.widget.AppCompatImageView
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import kotlin.math.sqrt

class ScaleImageView : AppCompatImageView, View.OnTouchListener {
    constructor(c: Context, attr: AttributeSet) : super(c, attr) {
        mContext = c
    }

    constructor(c: Context) : super(c) {
        mContext = c
    }

    companion object {
        var sBounds = false
        private const val MAX_SCALE = 10f
    }

    private val mContext: Context
    var mActivity: Activity? = null
    private val mMatrix = Matrix()
    private val mMatrixValues = FloatArray(9)

    private var mWidth: Int = 0
    private var mHeight: Int = 0

    private var mIntrinsicWidth: Int = 0
    private var mIntrinsicHeight: Int = 0

    private var mMinScale: Float = 0.toFloat()

    private var mPrevDistance: Float = 0.toFloat()
    private var mIsScaling: Boolean = false
    private var mIsInitializedScaling: Boolean = false

    private var mPrevMoveX: Int = 0
    private var mPrevMoveY: Int = 0
    private lateinit var mDetector: GestureDetector


    init {
        initialize()
    }

    override fun setImageBitmap(bm: Bitmap?) {
        super.setImageBitmap(bm)
        initialize()
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        initialize()
    }

    override fun setImageResource(resId: Int) {
        super.setImageResource(resId)
        initialize()
    }

    override fun setFrame(l: Int, t: Int, r: Int, b: Int): Boolean {
        mWidth = r - l
        mHeight = b - t

        mMatrix.reset()
        val rNorm = r - l
        var scale = rNorm.toFloat() / mIntrinsicWidth.toFloat()

        val paddingHeight: Int
        val paddingWidth: Int
        // scaling vertical
        if (scale * mIntrinsicHeight > mHeight) {
            scale = mHeight.toFloat() / mIntrinsicHeight.toFloat()
            mMatrix.postScale(scale, scale)
            paddingWidth = (r - mWidth) / 2
            paddingHeight = 0
            // scaling horizontal
        } else {
            mMatrix.postScale(scale, scale)
            paddingHeight = (b - mHeight) / 2
            paddingWidth = 0
        }
        mMatrix.postTranslate(paddingWidth.toFloat(), paddingHeight.toFloat())

        imageMatrix = mMatrix

        if (!mIsInitializedScaling) {
            mIsInitializedScaling = true
            // zoomTo(scale, mWidth / 2, mHeight / 2);
        }

        cutting()
        var isChanges = super.setFrame(l, t, r, b)
        if (mMinScale != scale) {
            mMinScale = scale
            isChanges = true
        }
        return isChanges
    }

    private fun initialize() {
        mDetector = GestureDetector(mContext, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent?): Boolean {
                e?.let { _ ->
                    maxZoomTo(e.x.toInt(), e.y.toInt())
                    cutting()
                }
                return super.onDoubleTap(e)
            }

            override fun onSingleTapUp(e: MotionEvent?): Boolean {
                mActivity?.finish()
                return super.onSingleTapUp(e)
            }

            override fun onLongPress(e: MotionEvent?) {
                mActivity?.openOptionsMenu()
                super.onLongPress(e)
            }
        })
        scaleType = ScaleType.MATRIX
        drawable?.run {
            mIntrinsicWidth = intrinsicWidth
            mIntrinsicHeight = intrinsicHeight
            setOnTouchListener(this@ScaleImageView)
        }
    }

    private fun getValue(matrix: Matrix, whichValue: Int): Float {
        matrix.getValues(mMatrixValues)
        return mMatrixValues[whichValue]
    }

    private fun getScale(): Float {
        return getValue(mMatrix, Matrix.MSCALE_X)
    }

    private fun getTranslateX(): Float {
        return getValue(mMatrix, Matrix.MTRANS_X)
    }

    private fun getTranslateY(): Float {
        return getValue(mMatrix, Matrix.MTRANS_Y)
    }

    private fun maxZoomTo(x: Int, y: Int) {
        if (mMinScale != getScale() && getScale() - mMinScale > 0.1f) {
            // threshold 0.1f
            val scale = mMinScale / getScale()
            zoomTo(scale, x, y)
        } else {
            val scale = 2f / getScale()
            zoomTo(scale, x, y)
        }
    }

    private fun zoomTo(scale: Float, x: Int, y: Int) {
        if (getScale() * scale < mMinScale
                || (scale >= 1 && getScale() * scale > MAX_SCALE)) return
        mMatrix.postScale(scale, scale)
        // move to center
        mMatrix.postTranslate(-(mWidth * scale - mWidth) / 2, -(mHeight * scale - mHeight) / 2)

        // move x and y distance
        mMatrix.postTranslate(-(x - mWidth / 2) * scale, 0f)
        mMatrix.postTranslate(0f, -(y - mHeight / 2) * scale)
        imageMatrix = mMatrix
    }

    fun cutting() {
        val width = (mIntrinsicWidth * getScale()).toInt()
        val height = (mIntrinsicHeight * getScale()).toInt()
        var bounds = when {
            getTranslateX() < -(width - mWidth) -> {
                mMatrix.postTranslate(-(getTranslateX() + width - mWidth), 0f)
                true
            }
            getTranslateX() == (-(width - mWidth)).toFloat() -> true
            else -> false
        }
        if (getTranslateX() > 0) {
            mMatrix.postTranslate(-getTranslateX(), 0f)
            bounds = true
        } else if (getTranslateX() == 0f) {
            bounds = true
        }
        if (getTranslateY() < -(height - mHeight)) {
            mMatrix.postTranslate(0f, -(getTranslateY() + height - mHeight))
        }
        if (getTranslateY() > 0) {
            mMatrix.postTranslate(0f, -getTranslateY())
        }
        if (width < mWidth) {
            mMatrix.postTranslate(((mWidth - width) / 2).toFloat(), 0f)
        }
        if (height < mHeight) {
            mMatrix.postTranslate(0f, ((mHeight - height) / 2).toFloat())
        }
        sBounds = bounds
        imageMatrix = mMatrix
    }

    private fun distance(x0: Float, x1: Float, y0: Float, y1: Float): Float {
        val x = x0 - x1
        val y = y0 - y1
        return Math.sqrt((x * x + y * y).toDouble()).toFloat()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null || mDetector.onTouchEvent(event)) return true

        val touchCount = event.pointerCount
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                if (touchCount >= 2) {
                    mPrevDistance = distance(event.getX(0), event.getX(1), event.getY(0), event.getY(1))
                    mIsScaling = true
                } else {
                    mPrevMoveX = event.x.toInt()
                    mPrevMoveY = event.y.toInt()
                }
                if (touchCount >= 2 && mIsScaling) {
                    val dist = distance(event.getX(0), event.getX(1), event.getY(0), event.getY(1))
                    var scale = (dist - mPrevDistance) / displayDistance()
                    mPrevDistance = dist
                    scale += 1f
                    scale *= scale
                    zoomTo(scale, mWidth / 2, mHeight / 2)
                    cutting()
                } else {
                    val distanceX = mPrevMoveX - event.x.toInt()
                    val distanceY = mPrevMoveY - event.y.toInt()
                    mPrevMoveX = event.x.toInt()
                    mPrevMoveY = event.y.toInt()
                    mMatrix.postTranslate((-distanceX).toFloat(), (-distanceY).toFloat())
                    cutting()
                }
            }
            MotionEvent.ACTION_MOVE -> if (touchCount >= 2 && mIsScaling) {
                val dist = distance(event.getX(0), event.getX(1), event.getY(0), event.getY(1))
                var scale = (dist - mPrevDistance) / displayDistance()
                mPrevDistance = dist
                scale += 1f
                scale *= scale
                zoomTo(scale, mWidth / 2, mHeight / 2)
                cutting()
            } else {
                val distanceX = mPrevMoveX - event.x.toInt()
                val distanceY = mPrevMoveY - event.y.toInt()
                mPrevMoveX = event.x.toInt()
                mPrevMoveY = event.y.toInt()
                mMatrix.postTranslate((-distanceX).toFloat(), (-distanceY).toFloat())
                cutting()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> if (event.pointerCount <= 1) {
                mIsScaling = false
            }
        }
        return true
    }

    private fun displayDistance() = sqrt((mWidth * mWidth + mHeight * mHeight).toDouble()).toFloat()

    override fun onTouch(v: View, event: MotionEvent) = super.onTouchEvent(event)
}
