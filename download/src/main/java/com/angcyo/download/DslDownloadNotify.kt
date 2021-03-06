package com.angcyo.download

import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import com.angcyo.library.app
import com.angcyo.library.component.*
import com.angcyo.library.ex.toBitmap
import com.liulishuo.okdownload.DownloadTask
import com.liulishuo.okdownload.core.cause.EndCause

/**
 * 下载通知显示
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/03/03
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
class DslDownloadNotify : DslListener() {

    var _downloadUrl: String? = null
    var _notifyId: Int = 0

    var logo: Bitmap? = null

    init {
        logo = app().packageName.appBean().appIcon.toBitmap()

        onTaskStart = {
            _notify {
                _remoteView(it.filename)
            }
        }

        onTaskFinish = { downloadTask, cause, exception ->
            val filename = downloadTask.filename
            _notify {
                notifyOngoing = false
                _remoteView(filename, 100) {
                    if (cause == EndCause.COMPLETED) {
                        if (filename?.toLowerCase()?.endsWith("apk") == true) {
                            setTextViewText(R.id.lib_sub_text_view, "下载完成, 点击安装!")

                            //安装APK Intent
                            DslIntent.getInstallAppIntent(downloadTask.file)?.run {
                                notifyContentIntent = DslNotify.pendingActivity(app(), this)
                            }
                        } else {
                            setTextViewText(R.id.lib_sub_text_view, "下载完成!")
                        }
                    } else {
                        setTextViewText(R.id.lib_sub_text_view, "下载失败:${exception?.message}")
                    }
                }
            }
        }
    }

    fun _notify(action: DslNotify.() -> Unit) {
        dslNotify {
            channelName = "文件下载"
            notifyId = _notifyId
            notifyOngoing = true
            notifyDefaults = NotificationCompat.DEFAULT_VIBRATE
            notifyPriority = NotificationCompat.PRIORITY_HIGH
            action()
        }
    }

    fun DslNotify._remoteView(
        name: String? = null,
        process: Int = -1,
        ico: Int = -1,
        action: DslRemoteView.() -> Unit = {}
    ) {
        notifyCustomContentView =
            dslRemoteView {
                layoutId = R.layout.layout_download_notify
                if (name != null) {
                    setTextViewText(R.id.lib_text_view, name)
                }
                if (ico > 0) {
                    setImageViewResource(R.id.lib_image_view, ico)
                } else {
                    logo?.run {
                        setImageViewBitmap(R.id.lib_image_view, this)
                    }
                }
                setProgressBar(R.id.lib_progress_bar, process)
                action()
            }
    }

    /**安装*/
    fun install(url: String) {
        uninstall()
        _downloadUrl = url
        _notifyId = System.currentTimeMillis().toInt()
        url.listener(this)
    }

    fun uninstall() {
        if (_notifyId > 0) {
            DslNotify.cancelNotify(app(), _notifyId)
        }
        _downloadUrl?.listener(this)
    }

    override fun taskProgress(
        task: DownloadTask,
        totalLength: Long,
        totalOffset: Long,
        increaseBytes: Long,
        speed: Long
    ) {
        super.taskProgress(task, totalLength, totalOffset, increaseBytes, speed)
        val percent = (totalOffset * 100 / totalLength).toInt()
        //计算每秒多少
        val sp: String = calcTaskSpeed(task, increaseBytes) + "/s"

        _notify {
            notifyDefaults = NotificationCompat.DEFAULT_LIGHTS
            notifyPriority = NotificationCompat.PRIORITY_LOW
            _remoteView(task.filename, percent) {
                setTextViewText(R.id.lib_sub_text_view, "速度:$sp")
            }
        }
    }
}

fun String.downloadNotify(action: DslDownloadNotify.() -> Unit = {}) {
    dslDownloadNotify(this, action)
}

fun dslDownloadNotify(url: String, action: DslDownloadNotify.() -> Unit = {}) {
    DslDownloadNotify().apply {
        action()
        install(url)
    }
}