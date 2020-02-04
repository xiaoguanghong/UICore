package com.angcyo.picker

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.angcyo.loader.LoaderConfig
import com.angcyo.loader.LoaderFolder
import com.angcyo.loader.LoaderMedia

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/01/31
 */
class PickerViewModel : ViewModel() {

    /**配置信息*/
    val loaderConfig = MutableLiveData<LoaderConfig>()

    /**选中的媒体*/
    val selectorMediaList = MutableLiveData<MutableList<LoaderMedia>>(mutableListOf())

    /**所有媒体*/
    val loaderFolderList = MutableLiveData<List<LoaderFolder>>()

    /**当前显示的文件夹*/
    val currentFolder = MutableLiveData<LoaderFolder>()

    /**添加选中项*/
    fun addSelectedMedia(media: LoaderMedia?) {
        media?.run {
            selectorMediaList.value?.add(this)
            selectorMediaList.value = selectorMediaList.value
        }
    }

    /**移除选中项*/
    fun removeSelectedMedia(media: LoaderMedia?) {
        media?.run {
            selectorMediaList.value?.remove(this)
            selectorMediaList.value = selectorMediaList.value
        }
    }
}