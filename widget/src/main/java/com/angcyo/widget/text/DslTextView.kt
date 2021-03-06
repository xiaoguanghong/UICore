package com.angcyo.widget.text

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import com.angcyo.widget.R
import com.angcyo.widget.drawable.DslAttrBadgeDrawable
import java.util.*

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/01/08
 */
open class DslTextView : DslScrollTextView {

    /**角标绘制*/
    var dslBadeDrawable = DslAttrBadgeDrawable()

    var textFormat: String? = null

    /**未处理过的原始[text]*/
    var _originText: CharSequence? = null

    constructor(context: Context) : super(context) {
        initAttribute(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initAttribute(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initAttribute(context, attrs)
    }

    private fun initAttribute(context: Context, attributeSet: AttributeSet?) {
        dslBadeDrawable.initAttribute(context, attributeSet)
        dslBadeDrawable.callback = this

        val typedArray =
            context.obtainStyledAttributes(attributeSet, R.styleable.DslTextView)
        textFormat = typedArray.getString(R.styleable.DslTextView_r_text_format)
        typedArray.recycle()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        dslBadeDrawable.apply {
            setBounds(0, 0, measuredWidth, measuredHeight)
            draw(canvas)
        }
    }

    override fun setText(text: CharSequence?, type: BufferType?) {
        _originText = text
        super.setText(wrapText(text), type)
    }

    open fun wrapText(text: CharSequence?): CharSequence? {
        return _originText?.run {
            if (textFormat.isNullOrBlank()) {
                this
            } else {
                String.format(Locale.CHINA, textFormat!!, text)
            }
        }
    }
}