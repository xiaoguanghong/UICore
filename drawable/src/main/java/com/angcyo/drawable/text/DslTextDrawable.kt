package com.angcyo.drawable.text

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.view.Gravity
import com.angcyo.drawable.DslGravity
import com.angcyo.drawable.base.AbsDslDrawable
import com.angcyo.drawable.textHeight
import com.angcyo.drawable.textWidth
import com.angcyo.library.ex.dp
import kotlin.math.max

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2019/12/20
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
open class DslTextDrawable : AbsDslDrawable() {

    /**文本大小*/
    var textSize = 12 * dp
        set(value) {
            field = value
            textPaint.textSize = field
        }

    /**文本颜色*/
    var textColor = Color.parseColor("#333333")
        set(value) {
            field = value
            textPaint.color = field
        }

    /**文本重力*/
    var textGravity = Gravity.CENTER

    /**绘制的文本*/
    var text: String? = null

    /**偏移纠正*/
    var textOffsetX = 0
    var textOffsetY = 0

    /**设置给[TextView]时, 并不会影响高度, 宽度会影响*/
    var textPaddingLeft = 0
    var textPaddingRight = 0
    var textPaddingTop = 0
    var textPaddingBottom = 0

    /**文本背景*/
    var textBgDrawable: Drawable? = null

    val _dslGravity = DslGravity()

    init {
        textPaint.color = textColor
        textPaint.textSize = textSize
        textPaint.style = Paint.Style.FILL
    }

    //计算属性
    val textWidth: Float
        get() = textPaint.textWidth(text)

    val textHeight: Float
        get() = textPaint.textHeight()

    override fun draw(canvas: Canvas) {

        if (text == null) {
            return
        }

        textBgDrawable?.apply {
            bounds = this@DslTextDrawable.bounds
            draw(canvas)
        }

        with(_dslGravity) {
            gravity = textGravity
            setGravityBounds(this@DslTextDrawable.bounds)
            applyGravity(textWidth, textHeight) { centerX, centerY ->

                val textDrawX: Float = centerX - _targetWidth / 2
                val textDrawY: Float = centerY + _targetHeight / 2

                //绘制文本
                canvas.drawText(
                    text!!,
                    textDrawX + textOffsetX + textPaddingLeft,
                    textDrawY - textPaint.descent() + textOffsetY + textPaddingTop,
                    textPaint
                )
            }
        }
    }

    override fun getIntrinsicWidth(): Int {
        return max(
            textWidth.toInt(),
            textBgDrawable?.minimumWidth ?: 0
        ) + textPaddingLeft + textPaddingRight
    }

    override fun getIntrinsicHeight(): Int {
        return max(
            textHeight.toInt(),
            textBgDrawable?.minimumHeight ?: 0
        ) + textPaddingTop + textPaddingBottom
    }
}