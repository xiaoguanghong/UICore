package com.angcyo.picker.dslitem

import android.graphics.Color
import android.graphics.drawable.TransitionDrawable
import androidx.annotation.DrawableRes
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter.containsPayload
import com.angcyo.dsladapter.margin
import com.angcyo.glide.giv
import com.angcyo.library.ex.*
import com.angcyo.loader.*
import com.angcyo.picker.R
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.base.bgColorAnimator
import com.angcyo.widget.base.longFeedback
import com.angcyo.widget.span.span
import kotlin.math.max

/**
 * 图片选择器, 小图片选择布局
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/02/01
 */

open class DslPickerImageItem : DslAdapterItem() {

    companion object {
        //动画更新
        const val PAYLOAD_UPDATE_ANIM = PAYLOAD_UPDATE_PART shl 1
        //无法选中的动画更新
        const val PAYLOAD_UPDATE_CANCEL_ANIM = PAYLOAD_UPDATE_ANIM shl 1
    }

    /**要加载的媒体*/
    val loaderMedia: LoaderMedia? get() = itemData as? LoaderMedia

    /**选中的索引数值*/
    var onGetSelectedIndex: (LoaderMedia?) -> CharSequence? = { null }

    /**请求选择item*/
    var onSelectorItem: (selected: Boolean) -> Unit = {}

    /**选择模式*/
    var checkModel = true

    /**显示文件大小*/
    var showFileSize: Boolean = false

    @DrawableRes
    var audioTipDrawable: Int = R.drawable.ic_picker_audio_folder
    @DrawableRes
    var videoTipDrawable: Int = -1

    var _transitionDuration = 200

    var _selectorMaskColor = Color.parseColor("#80000000")

    init {
        itemLayoutId = R.layout.dsl_picker_image_layout

        thisAreContentsTheSame = { _, newItem ->
            when {
                itemChanging -> false
                else -> (newItem as? DslPickerImageItem)?.loaderMedia == this.loaderMedia
            }
        }

        margin(1 * dpi)

        //长按赋值媒体信息
        onItemLongClick = {
            it.longFeedback()
            loaderMedia?.toString()?.copy(it.context)
            true
        }

        //大图预览
        onItemClick = {

        }
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        //局部更新
        val partUpdate = payloads.containsPayload(PAYLOAD_UPDATE_PART)
        val animUpdate = payloads.containsPayload(PAYLOAD_UPDATE_ANIM)
        val canceAnimUpdate = payloads.containsPayload(PAYLOAD_UPDATE_CANCEL_ANIM)

        if (!partUpdate) {
            //model
            itemHolder.visible(R.id.index_layout, checkModel)

            //缩略图
            itemHolder.giv(R.id.lib_image_view)?.apply {
                load(loaderMedia?.loadUri()) {
                    checkGifType = true
                }
                setOnClickListener(_clickListener)
                setOnLongClickListener(_longClickListener)
            }

            //audio video tip
            itemHolder.gone(R.id.lib_tip_image_view)
            if (loaderMedia?.isAudio() == true) {
                if (audioTipDrawable > 0) {
                    itemHolder.visible(R.id.lib_tip_image_view)
                    itemHolder.img(R.id.lib_tip_image_view)?.setImageResource(audioTipDrawable)
                }
            } else if (loaderMedia?.isVideo() == true) {
                if (videoTipDrawable > 0) {
                    itemHolder.visible(R.id.lib_tip_image_view)
                    itemHolder.img(R.id.lib_tip_image_view)?.setImageResource(videoTipDrawable)
                }
            }

            //文本
            if (loaderMedia?.isVideo() == true || loaderMedia?.isAudio() == true) {
                itemHolder.visible(R.id.duration_view)

                //时长
                itemHolder.tv(R.id.duration_view)?.text = span {
                    drawable {
                        backgroundDrawable = if (loaderMedia?.isVideo() == true) {
                            offsetY = 2 * dp
                            getDrawable(R.drawable.ic_picker_video)
                        } else {
                            getDrawable(R.drawable.ic_picker_audio)
                        }
                    }
                    appendSpace(6 * dpi)
                    //不足1秒的取1秒
                    val duration = max(loaderMedia!!.duration, 1_000)
                    append(
                        duration.toElapsedTime(
                            pattern = intArrayOf(-1, 1, 1),
                            h24 = booleanArrayOf(false, true, false),
                            units = arrayOf("", "", ":")
                        )
                    )
                }
            } else {
                itemHolder.gone(R.id.duration_view)
            }

            //事件
            itemHolder.click(R.id.index_layout) {
                onSelectorItem(itemIsSelected)
            }

            //索引背景
            itemHolder.view(R.id.index_view)?.setBackgroundResource(
                if (itemIsSelected) {
                    R.drawable.picker_index_checked_shape
                } else {
                    R.drawable.ic_picker_check_normal
                }
            )
            //选中提示背景
            itemHolder.view(R.id.selector_mask_view)?.setBackgroundColor(
                if (itemIsSelected) {
                    _selectorMaskColor
                } else {
                    Color.TRANSPARENT
                }
            )

            //debug
            if (isDebug()) {
                itemHolder.visible(R.id.lib_tip_text_view)
                itemHolder.tv(R.id.lib_tip_text_view)?.text = span {
                    append("${loaderMedia?.width ?: 0}")
                    append("x")
                    append("${loaderMedia?.height ?: 0}")
                    appendln()
                    append(loaderMedia?.fileSize?.fileSizeString() ?: "")
                    appendln()
                    append(loaderMedia?.mimeType() ?: "")
                }

            } else if (showFileSize) {
                itemHolder.visible(R.id.lib_tip_text_view)
                itemHolder.tv(R.id.lib_tip_text_view)?.text = span {
                    append(loaderMedia?.fileSize?.fileSizeString() ?: "")
                }
            } else {
                itemHolder.gone(R.id.lib_tip_text_view)
            }
        }

        if (animUpdate) {
            //点击时的动画, 在负载更新中执行
            if (itemIsSelected) {
                itemHolder.tv(R.id.index_view)?.background = TransitionDrawable(
                    arrayOf(
                        _drawable(R.drawable.ic_picker_check_normal),
                        _drawable(R.drawable.picker_index_checked_shape)
                    )
                ).apply {
                    startTransition(_transitionDuration)
                }

                itemHolder.view(R.id.selector_mask_view)
                    ?.bgColorAnimator(Color.TRANSPARENT, _selectorMaskColor)
            } else {
                itemHolder.tv(R.id.index_view)
                    ?.setBackgroundResource(R.drawable.ic_picker_check_normal)

                itemHolder.view(R.id.selector_mask_view)
                    ?.bgColorAnimator(_selectorMaskColor, Color.TRANSPARENT)
            }
        } else if (canceAnimUpdate) {
            //无法选中的动画
            itemHolder.tv(R.id.index_view)?.run {
                val duration = _transitionDuration
                background = TransitionDrawable(
                    arrayOf(
                        _drawable(R.drawable.picker_index_checked_shape),
                        _drawable(R.drawable.lib_white_circle_shape)
                    )
                ).apply {
                    startTransition(duration)
                }
                postDelayed({
                    setBackgroundResource(R.drawable.ic_picker_check_normal)
                }, duration.toLong())
            }
        }

        //索引
        itemHolder.tv(R.id.index_view)?.apply {
            text = onGetSelectedIndex(loaderMedia)
        }
    }
}