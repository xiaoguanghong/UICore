package com.angcyo.library.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.graphics.Point
import android.graphics.Rect
import android.net.ConnectivityManager
import android.net.Proxy
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.telephony.TelephonyManager
import android.text.format.Formatter
import android.view.View
import android.view.Window
import com.angcyo.library.*
import com.angcyo.library.ex.connect
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException
import java.net.NetworkInterface
import java.util.*
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2019/12/30
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
object Device {

    var deviceId: String = ""
        get() = if (field.isEmpty()) getUniqueDeviceId() else field

    /**
     * 获得独一无二的Psuedo ID
     * https://www.jianshu.com/p/130918ed8b2f
     * */
    private fun getUniqueDeviceId(): String {
        var result: String
        var serial: String?
        val idShort = "35" +
                Build.BOARD.length % 10 +
                Build.BRAND.length % 10 +
                Build.CPU_ABI.length % 10 +
                Build.DEVICE.length % 10 +
                Build.DISPLAY.length % 10 +
                Build.HOST.length % 10 +
                Build.ID.length % 10 +
                Build.MANUFACTURER.length % 10 +
                Build.MODEL.length % 10 +
                Build.PRODUCT.length % 10 +
                Build.TAGS.length % 10 +
                Build.TYPE.length % 10 +
                Build.USER.length % 10 //13 位
        try {
            serial = Build::class.java.getField("SERIAL").toString()
            //API>=9 使用serial号
            result = UUID(idShort.hashCode().toLong(), serial.hashCode().toLong()).toString()
        } catch (exception: Exception) { //serial需要一个初始化
            serial = "serial" // 随便一个初始化
            //使用硬件信息拼凑出来的15位号码
            result = UUID(idShort.hashCode().toLong(), serial.hashCode().toLong()).toString()
        }
        deviceId = result
        return result
    }

    /**sd卡已用空间*/
    fun getSdUsedBytes(): Long {
        //SD空间信息
        val statFs =
            StatFs(app().getExternalFilesDir("")?.absolutePath ?: app().filesDir.absolutePath)
        val usedBytes = statFs.totalBytes - statFs.availableBytes
        return usedBytes
    }

    /**sd卡可用空间*/
    fun getSdAvailableBytes(): Long {
        //SD空间信息
        val statFs =
            StatFs(app().getExternalFilesDir("")?.absolutePath ?: app().filesDir.absolutePath)
        return statFs.availableBytes
    }

    /**sd卡总空间*/
    fun getSdTotalBytes(): Long {
        //SD空间信息
        val statFs =
            StatFs(app().getExternalFilesDir("")?.absolutePath ?: app().filesDir.absolutePath)
        return statFs.totalBytes
    }

    /**
     * 获取当前可用内存，返回数据以字节为单位。
     *
     * @param context 可传入应用程序上下文。
     * @return 当前可用内存单位为B。
     */
    fun getAvailableMemory(context: Context = app()): Long {
        val memoryInfo = getMemoryInfo(context)
        return memoryInfo.availMem
    }

    /**获取总内存大小*/
    fun getTotalMemory(context: Context = app()): Long {
        val memoryInfo = getMemoryInfo(context)
        return memoryInfo.totalMem
    }

    /**
     * 获取系统总内存
     *
     * @param context 可传入应用程序上下文。
     * @return 总内存大单位为B。
     */
    fun getTotalMemorySize(): Long {
        val dir = "/proc/meminfo"
        try {
            val fr = FileReader(dir)
            val br = BufferedReader(fr, 2048)
            val memoryLine = br.readLine()
            val subMemoryLine = memoryLine.substring(memoryLine.indexOf("MemTotal:"))
            br.close()
            return subMemoryLine.replace("\\D+".toRegex(), "").toInt() * 1024L
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return 0
    }

    fun getMemoryInfo(context: Context): ActivityManager.MemoryInfo {
        val am =
            context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memoryInfo)
        return memoryInfo
    }

    /**
     * SDCARD是否存
     */
    fun externalMemoryAvailable(): Boolean {
        return Environment.getExternalStorageState() ==
                Environment.MEDIA_MOUNTED
    }

    /**
     * 获取SDCARD总的存储空间
     *
     * @return
     */
    fun getTotalExternalMemorySize(): Long {
        return if (externalMemoryAvailable()) {
            val path = Environment.getExternalStorageDirectory()
            val stat = StatFs(path.path)
            val blockSize: Long
            val totalBlocks: Long
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                blockSize = stat.blockSizeLong
                totalBlocks = stat.blockCountLong
            } else {
                blockSize = stat.blockSize.toLong()
                totalBlocks = stat.blockCount.toLong()
            }
            totalBlocks * blockSize
        } else {
            -1
        }
    }

    /**
     * 获取SDCARD剩余存储空间 kb单位->KB->MB
     *
     * @return
     */
    fun getAvailableExternalMemorySize(): Long {
        return if (externalMemoryAvailable()) {
            val path = Environment.getExternalStorageDirectory()
            val stat = StatFs(path.path)
            val blockSize: Long
            val availableBlocks: Long
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                blockSize = stat.blockSizeLong
                availableBlocks = stat.availableBlocksLong
            } else {
                blockSize = stat.blockSize.toLong()
                availableBlocks = stat.availableBlocks.toLong()
            }
            availableBlocks * blockSize
        } else {
            -1
        }
    }

    /**设备信息*/
    fun deviceInfo(context: Context, builder: Appendable): Appendable {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        builder.append("psuedoID: ")
        builder.appendln(getUniqueDeviceId())
        builder.appendln()
        // cpu架构
        builder.append("CPU : ")
//        builder.appendln(Build.CPU_ABI)
//        //        builder.appendln();
//        builder.append("CPU ABI 2: ")
//        builder.appendln(Build.CPU_ABI2)
        builder.append(Build.SUPPORTED_ABIS.connect("/").toString())

        builder.appendln()
        builder.append("memoryClass: ")
        builder.appendln(manager.memoryClass)
        builder.append("largeMemoryClass: ")
        builder.appendln(manager.largeMemoryClass)
        builder.appendln()
        builder.append("手机内存大小:")
        builder.appendln(Formatter.formatFileSize(context, getTotalMemorySize()))
        //        builder.appendln();
//        builder.append("JVM可用内存大小:");
//        builder.appendln(Formatter.formatFileSize(context, Runtime.getRuntime().maxMemory()));
//        builder.appendln();
        val memoryInfo: ActivityManager.MemoryInfo = getMemoryInfo(context)
        builder.append("系统总内存:")
        builder.appendln(Formatter.formatFileSize(context, memoryInfo.totalMem))
        builder.append("系统剩余内存:")
        builder.appendln(Formatter.formatFileSize(context, memoryInfo.availMem))
        //        builder.append("是否内存警告:");
//        builder.appendln(memoryInfo.lowMemory);
//        builder.append("阈值:");
//        builder.appendln(Formatter.formatFileSize(context, memoryInfo.threshold));
//        builder.appendln();
//        builder.append("getNativeHeapSize:");
//        builder.appendln(Formatter.formatFileSize(context, Debug.getNativeHeapSize()));
//
//        builder.append("getNativeHeapAllocatedSize:");
//        builder.appendln(Formatter.formatFileSize(context, Debug.getNativeHeapAllocatedSize()));
//
//        builder.append("getNativeHeapFreeSize:");
//        builder.appendln(Formatter.formatFileSize(context, Debug.getNativeHeapFreeSize()));
        builder.appendln()
        builder.append("SD空间大小:")
        builder.appendln(Formatter.formatFileSize(context, getTotalExternalMemorySize()))
        builder.append("SD可用空间大小:")
        builder.appendln(Formatter.formatFileSize(context, getAvailableExternalMemorySize()))
        return builder
    }

    /**设备屏幕信息*/
    fun screenInfo(context: Context, builder: Appendable): Appendable {
        builder.apply {

            val decorView = (context as? Activity)?.window?.decorView
            val contentView = (context as? Activity)?.findViewById<View>(Window.ID_ANDROID_CONTENT)

            val displayMetrics = context.resources.displayMetrics
            val widthDp: Float = displayMetrics.widthPixels / displayMetrics.density
            val heightDp: Float = displayMetrics.heightPixels / displayMetrics.density

            // 屏幕尺寸
            val width = displayMetrics.widthPixels / displayMetrics.xdpi
            //displayMetrics.heightPixels / displayMetrics.ydpi

            val dvHeight = decorView?.measuredHeight ?: 0
            val dvWidth = decorView?.measuredWidth ?: 0

            val cvHeight = contentView?.measuredHeight ?: 0
            val cvWidth = contentView?.measuredWidth ?: 0

            val height =
                (decorView?.measuredHeight ?: displayMetrics.heightPixels) / displayMetrics.ydpi

            val x = width.toDouble().pow(2.0)
            val y = height.toDouble().pow(2.0)
            val screenInches = sqrt(x + y)

            append("wPx:").append(displayMetrics.widthPixels)
            append(" hPx:").append(displayMetrics.heightPixels)

            if (decorView != null) {
                append(" dw:").append(decorView.measuredWidth)
                append(" dh:").append(decorView.measuredHeight)
            }

            if (contentView != null) {
                append(" cw:").append(contentView.measuredWidth)
                append(" ch:").append(contentView.measuredHeight)
            }

            //dp值
            appendln()
            append("wDp:").append(widthDp)
            append(" hDp:").append(heightDp)
            append(" dp:").append(displayMetrics.density)
            append(" sp:").append(displayMetrics.scaledDensity)
            append(" dpi:").appendln(displayMetrics.densityDpi)

            //多少寸
            append("w:").append("%.02f".format(width))
            append(" h:").append("%.02f".format(height))
            append(" inches:").append("%.02f".format(screenInches))
            //导航栏, 状态栏高度
            val statusBarHeight = getStatusBarHeight()
            val navBarHeight = max(dvWidth - cvWidth, dvHeight - cvHeight)
            append(" sh:").append(statusBarHeight).append(" ")
                .append(statusBarHeight / displayMetrics.density).append("dp")
            append(" nh:").append(navBarHeight).append(" ")
                .append(navBarHeight / displayMetrics.density).append("dp").appendln()

            val rect = Rect()
            val point = Point()
            if (decorView != null) {
                decorView.getGlobalVisibleRect(rect, point)
                append(" d:").append(rect)
                append(" d:").append(point).appendln()
            }

            if (contentView != null) {
                contentView.getGlobalVisibleRect(rect, point)
                append(" c:").append(rect)
                append(" c:").append(point)

                appendln()
                contentView.getWindowVisibleDisplayFrame(rect)
                append("frame:").append(rect)
            }
        }
        return builder
    }

    /**本地APK编译信息*/
    fun buildString(builder: Appendable): Appendable {
        builder.apply {
            append(getAppVersionName()).append(":").append(getAppVersionCode())
            append(" ")
            append(getAppString("user_name"))
            append(" ")
            appendln(getAppString("os_name"))

            appendln(getAppString("build_time"))
        }
        return builder
    }

    /**
     * 需要权限:
     * android.permission.READ_PHONE_STATE
     * android.permission.READ_SMS.
     * @param context 上下文
     * @return 返回手机号码 tel number
     */
    @SuppressLint("MissingPermission")
    fun getTelNumber(context: Context): String? {
        val tm = context
            .getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        return tm.line1Number
    }

    /** 是否使用代理(WiFi状态下的,避免被抓包) */
    fun isWifiProxy(context: Context = app()): Boolean {
        return !(proxyInfo(context).isNullOrBlank())
    }

    /**代理信息*/
    fun proxyInfo(context: Context = app()): String? {
        val isIcsOrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH
        val proxyAddress: String?
        val proxyPort: Int
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                val cm: ConnectivityManager? =
                    context.getSystemService(ConnectivityManager::class.java)
                proxyAddress = cm?.defaultProxy?.host
                proxyPort = cm?.defaultProxy?.port ?: -1
            }
            isIcsOrLater -> {
                proxyAddress = System.getProperty("http.proxyHost")
                proxyPort = System.getProperty("http.proxyPort")?.toIntOrNull() ?: -1
            }
            else -> {
                proxyAddress = Proxy.getHost(context)
                proxyPort = Proxy.getPort(context)
            }
        }
        return proxyAddress?.run { "$this:$proxyPort" }
    }

    /** 是否正在使用VPN */
    fun isVpnUsed(): Boolean {
        return !vpnInfo().isNullOrBlank()
    }

    /**是否使用了代理Proxy*/
    fun isProxyUsed(context: Context = app()): Boolean {
        return !proxyInfo(context).isNullOrBlank()
    }

    /**vpn信息*/
    fun vpnInfo(): String? {
        var name: String? = null
        try {
            val enumerationList: Enumeration<NetworkInterface>? =
                NetworkInterface.getNetworkInterfaces()
            enumerationList?.run {
                while (hasMoreElements()) {
                    val network = nextElement()
                    if (!network.isUp || network.interfaceAddresses.size == 0) {
                        continue
                    }
                    //Log.d("-----", "isVpnUsed() NetworkInterface Name: " + intf.getName());
                    if ("tun0" == network.name || "ppp0" == network.name) {
                        //RUtils.saveToSDCard("proxy.log", "isVpnUsed:" + intf.name)
                        name = network.name// The VPN is up
                    }
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        return name
    }
}

fun Appendable.append(value: Int): Appendable {
    return append(value.toString())
}

fun Appendable.append(value: Float): Appendable {
    return append(value.toString())
}

fun Appendable.append(value: Rect): Appendable {
    return append(value.toString())
}

fun Appendable.append(value: Point): Appendable {
    return append(value.toString())
}

fun Appendable.appendln(value: Int): Appendable {
    return appendln(value.toString())
}

fun Appendable.appendln(value: Float): Appendable {
    return appendln(value.toString())
}