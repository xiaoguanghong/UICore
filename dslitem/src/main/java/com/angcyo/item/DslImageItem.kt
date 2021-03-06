package com.angcyo.item

import android.net.Uri
import androidx.annotation.DrawableRes
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter.updateMedia
import com.angcyo.glide.DslGlide
import com.angcyo.glide.GlideImageView
import com.angcyo.glide.giv
import com.angcyo.library.ex.*
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.image.DslImageView
import com.angcyo.widget.span.span
import kotlin.math.max

/**
 * 简单的图片展示item
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/02/19
 */

open class DslImageItem : DslAdapterItem() {

    /**配置[DslGlide]*/
    var onConfigGlide: (DslGlide) -> Unit = {

    }

    /**配置[DslImageView]*/
    var onConfigImageView: (GlideImageView) -> Unit = {

    }

    /**检查gif图片类型, 如果是会使用[GifDrawable]*/
    var itemCheckGifType = true

    /**加载的媒体*/
    open var itemLoadUri: Uri? = null

    /**媒体类型*/
    open var itemMimeType: String? = null

    /**视频或者音频时长, 毫秒*/
    open var itemMediaDuration: Long = -1

    @DrawableRes
    var itemAudioCoverTipDrawable: Int = R.drawable.lib_audio_cover_tip

    @DrawableRes
    var itemVideoCoverTipDrawable: Int = R.drawable.lib_video_cover_tip

    @DrawableRes
    var itemAudioTipDrawable: Int = R.drawable.lib_audio_tip

    @DrawableRes
    var itemVideoTipDrawable: Int = R.drawable.lib_video_tip

    init {
        itemLayoutId = R.layout.dsl_image_item
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)
        //更新媒体
        val mediaUpdate = payloads.updateMedia()

        itemHolder.giv(R.id.lib_image_view)?.apply {
            onConfigImageView(this)
        }

        if (mediaUpdate) {
            //缩略图
            itemHolder.giv(R.id.lib_image_view)?.apply {
                load(itemLoadUri) {
                    checkGifType = itemCheckGifType
                    onConfigGlide(this)
                }
            }
        }

        //audio video tip
        itemHolder.gone(R.id.lib_tip_image_view)
        itemHolder.gone(R.id.lib_duration_view)
        if (itemMimeType?.isAudioMimeType() == true) {
            if (itemAudioCoverTipDrawable > 0) {
                itemHolder.visible(R.id.lib_tip_image_view)
                itemHolder.img(R.id.lib_tip_image_view)?.setImageResource(itemAudioCoverTipDrawable)
            }
        } else if (itemMimeType?.isVideoMimeType() == true) {
            if (itemVideoCoverTipDrawable > 0) {
                itemHolder.visible(R.id.lib_tip_image_view)
                itemHolder.img(R.id.lib_tip_image_view)?.setImageResource(itemVideoCoverTipDrawable)
            }
        }

        //时长
        if (itemMimeType?.isVideoMimeType() == true || itemMimeType?.isAudioMimeType() == true) {
            itemHolder.visible(R.id.lib_duration_view)

            itemHolder.tv(R.id.lib_duration_view)?.text = span {
                drawable {
                    backgroundDrawable =
                        if (itemMimeType?.isVideoMimeType() == true && itemVideoTipDrawable > 0) {
                            getDrawable(itemVideoTipDrawable)
                        } else if (itemMimeType?.isAudioMimeType() == true && itemAudioTipDrawable > 0) {
                            getDrawable(itemAudioTipDrawable)
                        } else {
                            null
                        }
                }

                if (itemMediaDuration > 0) {
                    appendSpace(6 * dpi)
                    val _duration = itemMediaDuration
                    //不足1秒的取1秒
                    val duration = if (_duration != 0L) max(_duration, 1_000) else 0
                    append(
                        duration.toElapsedTime(
                            pattern = intArrayOf(-1, 1, 1),
                            h24 = booleanArrayOf(false, true, false),
                            units = arrayOf("", "", ":")
                        )
                    )
                }
            }
        } else {
            itemHolder.gone(R.id.lib_duration_view)
        }
    }
}