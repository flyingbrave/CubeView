package com.zybang.yike.mvp.playback.test

import android.content.Context
import android.content.res.Resources
import android.content.res.Resources.NotFoundException
import android.graphics.*
import android.util.SparseArray
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.FrameLayout
import java.util.*

/**
 * cubeView
 */

class CubeView(context: Context) : FrameLayout(context) {

    private val NOTNOWN = 0
    private val VERTICALLY_ORIENTATION = 1
    private val HORIZONTALLY_ORIENTATION = -1
    private val ROTATION_MAX = 60
    private val ROTATION_MIN = -60
    private val ROTATION_DEFAULT_X = -10
    private val ROTATION_DEFAULT_Y = 15
    private val ZOOM_DEFAULT = 0.6f
    private val ZOOM_MIN = 0.33f
    private val ZOOM_MAX = 2f
    private val SPACING_DEFAULT = 25
    private val SPACING_MIN = 10
    private val SPACING_MAX = 100
    private val SHADOW_COLOR = Color.RED
    private val TEXT_OFFSET_DP = 2
    private val TEXT_SIZE_DP = 10
    private val CHILD_COUNT_ESTIMATION = 25

    
    private val viewBoundsRect = Rect()
    private val viewBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val camera = Camera()
    private var matrix2 = Matrix()
    private val location = IntArray(2)
    private val visibilities = BitSet(CHILD_COUNT_ESTIMATION)
    private val idNames = SparseArray<String>()
    private val layeredViewQueue: Deque<LayeredView> = ArrayDeque()
    private val layeredViewPool: Pool<LayeredView?> = object : Pool<LayeredView?>(CHILD_COUNT_ESTIMATION) {
             override fun newObject(): LayeredView? {
                return LayeredView()
            }
        }

    private var res: Resources? = null
    private var density = 0f
    private var slop = 0f
    private var textOffset = 0f
    private var textSize = 0f

    private var enabled2 = false
    private var isShowView = false

    private var pointerOne = MotionEvent.INVALID_POINTER_ID
    private var lastOneX = 0f
    private var lastOneY = 0f
    private var pointerTwo = MotionEvent.INVALID_POINTER_ID
    private var lastTwoX = 0f
    private var lastTwoY = 0f
    private var multiTouchTracking = NOTNOWN

    private var rotationY = ROTATION_DEFAULT_Y
    private var rotationX = ROTATION_DEFAULT_X
    private var zoom = ZOOM_DEFAULT
    private var spacing = SPACING_DEFAULT.toFloat()

    private var shadowColor = 0

    init {
        initParams()
    }



    private fun initParams() {
        res = context.resources
        density = context.resources.displayMetrics.density
        slop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()

        textSize = TEXT_SIZE_DP * density
        textOffset = TEXT_OFFSET_DP * density


        viewBorderPaint.style = Paint.Style.STROKE
        viewBorderPaint.textSize = textSize
        setShadowColor(SHADOW_COLOR)
        viewBorderPaint.typeface = Typeface.create("cubeView", Typeface.NORMAL)
    }



    fun setShadowColor(color: Int) {
        if (shadowColor != color) {
            viewBorderPaint.setShadowLayer(1f, -1f, 1f, color)
            shadowColor = color
            invalidate()
        }
    }

    fun setCubeEnabled(e: Boolean) {
        if (enabled2 != e) {
            enabled2 = e
            setWillNotDraw(!e)
            invalidate()
        }
    }

    fun showViewId(b: Boolean) {
        if (this.isShowView != b) {
            this.isShowView = b
            invalidate()
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return enabled2 || super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!enabled2) {
            return super.onTouchEvent(event)
        }
        val action = event.actionMasked
        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val index = if (action == MotionEvent.ACTION_DOWN) 0 else event.actionIndex
                if (pointerOne == MotionEvent.INVALID_POINTER_ID) {
                    pointerOne = event.getPointerId(index)
                    lastOneX = event.getX(index)
                    lastOneY = event.getY(index)

                } else if (pointerTwo == MotionEvent.INVALID_POINTER_ID) {
                    pointerTwo = event.getPointerId(index)
                    lastTwoX = event.getX(index)
                    lastTwoY = event.getY(index)

                } 
            }
            MotionEvent.ACTION_MOVE -> {
                if (pointerTwo == MotionEvent.INVALID_POINTER_ID) {
                    var i = 0
                    val count = event.pointerCount
                    while (i < count) {
                        if (pointerOne == event.getPointerId(i)) {
                            val eventX = event.getX(i)
                            val eventY = event.getY(i)
                            val dx = eventX - lastOneX
                            val dy = eventY - lastOneY
                            val drx = 90 * (dx / width)
                            val dry = 90 * (-dy / height)
                            rotationY = Math.min(Math.max(rotationY + drx, ROTATION_MIN.toFloat()), ROTATION_MAX.toFloat()).toInt()
                            rotationX = Math.min(Math.max(rotationX + dry, ROTATION_MIN.toFloat()), ROTATION_MAX.toFloat()).toInt()

                            lastOneX = eventX
                            lastOneY = eventY
                            invalidate()
                        }
                        i++
                    }
                } else {
                    val pointerOneIndex = event.findPointerIndex(pointerOne)
                    val pointerTwoIndex = event.findPointerIndex(pointerTwo)
                    val xOne = event.getX(pointerOneIndex)
                    val yOne = event.getY(pointerOneIndex)
                    val xTwo = event.getX(pointerTwoIndex)
                    val yTwo = event.getY(pointerTwoIndex)
                    val dxOne = xOne - lastOneX
                    val dyOne = yOne - lastOneY
                    val dxTwo = xTwo - lastTwoX
                    val dyTwo = yTwo - lastTwoY
                    if (multiTouchTracking == NOTNOWN) {
                        val adx = Math.abs(dxOne) + Math.abs(dxTwo)
                        val ady = Math.abs(dyOne) + Math.abs(dyTwo)
                        if (adx > slop * 2 || ady > slop * 2) {
                            multiTouchTracking = if (adx > ady) {
                                HORIZONTALLY_ORIENTATION
                            } else {
                                VERTICALLY_ORIENTATION
                            }
                        }
                    }
                    if (multiTouchTracking == VERTICALLY_ORIENTATION) {
                        zoom += if (yOne >= yTwo) {
                            dyOne / height - dyTwo / height
                        } else {
                            dyTwo / height - dyOne / height
                        }
                        zoom = Math.min(Math.max(zoom, ZOOM_MIN), ZOOM_MAX)
                        invalidate()
                    } else if (multiTouchTracking == HORIZONTALLY_ORIENTATION) {
                        spacing += if (xOne >= xTwo) {
                            dxOne / width * SPACING_MAX - dxTwo / width * SPACING_MAX
                        } else {
                            dxTwo / width * SPACING_MAX - dxOne / width * SPACING_MAX
                        }
                        spacing = Math.min(
                            Math.max(spacing, SPACING_MIN.toFloat()),
                            SPACING_MAX.toFloat()
                        )
                        invalidate()
                    }
                    if (multiTouchTracking != NOTNOWN) {
                        lastOneX = xOne
                        lastOneY = yOne
                        lastTwoX = xTwo
                        lastTwoY = yTwo
                    }
                }
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val index = if (action != MotionEvent.ACTION_POINTER_UP) 0 else event.actionIndex
                val pointerId = event.getPointerId(index)
                if (pointerOne == pointerId) {
                    pointerOne = pointerTwo
                    lastOneX = lastTwoX
                    lastOneY = lastTwoY

                    pointerTwo = MotionEvent.INVALID_POINTER_ID
                    multiTouchTracking = NOTNOWN
                } else if (pointerTwo == pointerId) {

                    pointerTwo = MotionEvent.INVALID_POINTER_ID
                    multiTouchTracking = NOTNOWN
                }
            }
        }
        return true
    }

    override fun draw(canvas: Canvas) {
        if (!enabled2) {
            super.draw(canvas)
            return
        }
        getLocationInWindow(location)
        val x = location[0].toFloat()
        val y = location[1].toFloat()
        val saveCount = canvas.save()
        val cx = width / 2f
        val cy = height / 2f
        camera.save()
        camera.rotate(rotationX.toFloat(), rotationY.toFloat(), 0f)
        camera.getMatrix(matrix2)
        camera.restore()
        matrix2.preTranslate(-cx, -cy)
        matrix2.postTranslate(cx, cy)
        canvas.concat(matrix2)
        canvas.scale(zoom, zoom, cx, cy)
        if (!layeredViewQueue.isEmpty()) {
            throw AssertionError("View queue is not empty.")
        }

        var i = 0
        val count = childCount
        while (i < count) {
            val layeredView: LayeredView? = layeredViewPool.obtain()
            layeredView?.set(getChildAt(i), 0)
            layeredViewQueue.add(layeredView)
            i++
        }
        while (!layeredViewQueue.isEmpty()) {
            val layeredView: LayeredView = layeredViewQueue.removeFirst()
            val view = layeredView.view
            val layer = layeredView.layer

            layeredView.clear()
            layeredViewPool.restore(layeredView)

            if (view is ViewGroup) {
                val viewGroup = view
                visibilities.clear()
                var i = 0
                val count = viewGroup.childCount
                while (i < count) {
                    val child = viewGroup.getChildAt(i)
                    if (child.visibility == VISIBLE) {
                        visibilities.set(i)
                        child.visibility = INVISIBLE
                    }
                    i++
                }
            }
            val viewSaveCount = canvas.save()

            val translateShowX = (rotationY / ROTATION_MAX).toFloat()
            val translateShowY = (rotationX / ROTATION_MAX).toFloat()
            val tx = layer * spacing * density * translateShowX
            val ty = layer * spacing * density * translateShowY
            canvas.translate(tx, -ty)

            view?.getLocationInWindow(location)
            canvas.translate(location[0] - x, location[1] - y)
            if (view != null) {
                viewBoundsRect[0, 0, view?.width] = view.height
            }
            canvas.drawRect(viewBoundsRect, viewBorderPaint)
            view?.draw(canvas)
            if (isShowView) {
                val id = view?.id
                if (id != NO_ID) {
                    canvas.drawText(nameForId(id!!)!!, textOffset, textSize, viewBorderPaint)
                }
            }
            canvas.restoreToCount(viewSaveCount)

            if (view is ViewGroup) {
                val viewGroup = view
                var i = 0
                val count = viewGroup.childCount
                while (i < count) {
                    if (visibilities[i]) {
                        val child = viewGroup.getChildAt(i)
                        child.visibility = VISIBLE
                        val childLayeredView: LayeredView? =
                            layeredViewPool.obtain()
                        childLayeredView?.set(child, layer + 1)
                        layeredViewQueue.add(childLayeredView)
                    }
                    i++
                }
            }
        }
        canvas.restoreToCount(saveCount)
    }

    private fun nameForId(id: Int): String? {
        var name = idNames[id]
        if (name == null) {
            try {
                name = res!!.getResourceEntryName(id)
            } catch (e: NotFoundException) {

            }
            idNames.put(id, name)
        }
        return name
    }

    private abstract class Pool<T>  constructor(initialSize: Int) {
        private val pool: Deque<T>
        fun obtain(): T {
            return if (pool.isEmpty()) newObject() else pool.removeLast()
        }

        fun restore(instance: T) {
            pool.addLast(instance)
        }

        protected abstract fun newObject(): T

        init {
            pool = ArrayDeque(initialSize)
            for (i in 0 until initialSize) {
                pool.addLast(newObject())
            }
        }
    }

    private class LayeredView {
        var view: View? = null
        var layer = 0
        operator fun set(view: View?, layer: Int) {
            this.view = view
            this.layer = layer
        }

        fun clear() {
            view = null
            layer = -1
        }
    }

}