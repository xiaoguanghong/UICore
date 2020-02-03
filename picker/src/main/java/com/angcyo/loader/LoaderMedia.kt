package com.angcyo.loader

import android.net.Uri
import android.os.Parcelable
import com.angcyo.library.ex.*
import kotlinx.android.parcel.Parcelize

/**
 * Loader 加载出来的媒体数据
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/01/22
 */

@Parcelize
data class LoaderMedia(
    //数据库中的id
    var id: Long = -1,

    //网络路径
    var url: String? = null,
    //本地路径
    var localPath: String? = null,
    //压缩后的路径
    var compressPath: String? = null,
    //剪切后的路径
    var cutPath: String? = null,
    //视频 音频媒体时长, 毫秒
    var duration: Long = 0,

    //Android Q文件存储机制修改成了沙盒模式, 不能直接通过路径的方式访问文件
    var localUri: Uri? = null,

    //数据库字段↓

    //angcyo
    var width: Int = 0,
    var height: Int = 0,

    /** 1558921509 秒 */
    var modifyTime: Long = 0,

    /** 1558921509 秒 */
    var addTime: Long = 0,

    /** 文件大小, b->kb */
    var fileSize: Long = 0,

    var displayName: String? = null,

    var mimeType: String? = null,

    /**
     * The orientation for the media item, expressed in degrees. For
     * example, 0, 90, 180, or 270 degrees.
     * [android.provider.MediaStore.MediaColumns.ORIENTATION]*/
    var orientation: Int = 0,

    //纬度
    var latitude: Double = 0.0,
    //经度
    var longitude: Double = 0.0

) : Parcelable

//媒体类型
fun LoaderMedia.mimeType(): String {
    return mimeType ?: (loadPath()?.mimeType() ?: "image/*")
}

fun LoaderMedia.isVideo(): Boolean {
    return mimeType().isVideoMimeType()
}

fun LoaderMedia.isAudio(): Boolean {
    return mimeType().isAudioMimeType()
}

fun LoaderMedia.isImage(): Boolean {
    return mimeType().isImageMimeType()
}

/**加载路径*/
fun LoaderMedia.loadPath(): String? {
    if (cutPath?.isFileExist() == true) {
        return cutPath
    }
    if (compressPath?.isFileExist() == true) {
        return compressPath
    }
    if (localPath?.isFileExist() == true) {
        return localPath
    }
    return url
}

/**加载的[Uri]*/
fun LoaderMedia.loadUri(): Uri? {
    if (cutPath?.isFileExist() == true) {
        return Uri.fromFile(cutPath!!.file())
    }
    if (compressPath?.isFileExist() == true) {
        return Uri.fromFile(compressPath!!.file())
    }
    if (localUri != null) {
        return localUri
    }
    if (localPath?.isFileExist() == true) {
        return Uri.fromFile(localPath!!.file())
    }
    return Uri.parse(url)
}