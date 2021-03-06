package com.angcyo.dsladapter

import android.os.Handler
import android.os.Looper
import androidx.recyclerview.widget.DiffUtil
import com.angcyo.dsladapter.internal.*
import com.angcyo.library.L
import com.angcyo.library.ex.nowTime
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2019/05/09
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */

open class DslDataFilter(val dslAdapter: DslAdapter) {

    /**
     * 过滤后的数据源, 缓存过滤后的数据源, 防止每次都计算.
     *
     * 当有原始数据源发生改变时, 需要调用 [updateFilterItems] 更新过滤后的数据源
     * */
    val filterDataList: MutableList<DslAdapterItem> = mutableListOf()

    val _dispatchUpdatesSet = mutableSetOf<OnDispatchUpdatesListener>()

    /**
     * 可以拦截参与计算[diff]的数据源
     * @param oldDataList 界面显示的数据源
     * @param newDataList 即将显示的数据源
     * @return 需要显示的数据源
     * */
    var onFilterDataList: (oldDataList: List<DslAdapterItem>, newDataList: List<DslAdapterItem>) -> List<DslAdapterItem> =
        { _, newDataList -> newDataList }

    /**前置过滤器*/
    val beforeFilterInterceptorList = mutableListOf<FilterInterceptor>()

    /**中置过滤拦截器*/
    val filterInterceptorList = mutableListOf(
        GroupItemFilterInterceptor(),
        SubItemFilterInterceptor(),
        HideItemFilterInterceptor()
    )

    /**后置过滤器*/
    val afterFilterInterceptorList = mutableListOf<FilterInterceptor>()

    //异步调度器
    private val asyncExecutor: ExecutorService by lazy {
        Executors.newFixedThreadPool(1)
    }

    //diff操作
    private var _lastDiffRunnable: DiffRunnable? = null

    private val mainHandler: Handler by lazy {
        Handler(Looper.getMainLooper())
    }

    private val handle: OnceHandler by lazy {
        OnceHandler()
    }

    /**更新过滤后的数据源, 采用的是[DiffUtil]*/
    fun updateFilterItemDepend(params: FilterParams) {
        if (handle.hasCallbacks()) {
            _lastDiffRunnable?.notifyUpdateDependItem()
        }

        var filterParams = params

        if (params.justFilter) {
            filterParams = params.copy(just = true, async = false)
        }

        val diffRunnable = DiffRunnable()
        val updateDependRunnable = UpdateDependRunnable()

        diffRunnable._params = filterParams
        updateDependRunnable._diffRunnable = diffRunnable
        _lastDiffRunnable = diffRunnable

        if (filterParams.just) {
            handle.clear()
            updateDependRunnable.run()
        } else {
            //确保多次触发, 只有一次被执行
            handle.shakeType = params.shakeType
            handle.once(params.shakeDelay, updateDependRunnable)
        }
    }

    /**过滤[originList]数据源*/
    open fun filterItemList(originList: List<DslAdapterItem>): List<DslAdapterItem> {
        var result = listOf<DslAdapterItem>()
        val chain = FilterChain(
            dslAdapter,
            this,
            _lastDiffRunnable?._params ?: FilterParams(),
            originList,
            originList,
            false
        )

        var interruptChain = false

        fun proceed(interceptorList: List<FilterInterceptor>) {
            if (!interruptChain) {
                for (filer in interceptorList) {
                    result = filer.intercept(chain)
                    chain.requestList = result
                    if (chain.interruptChain) {
                        interruptChain = true
                        break
                    }
                }
            }
        }

        proceed(beforeFilterInterceptorList)
        proceed(filterInterceptorList)
        proceed(afterFilterInterceptorList)

        return result
    }

    fun addDispatchUpdatesListener(listener: OnDispatchUpdatesListener) {
        _dispatchUpdatesSet.add(listener)
    }

    fun removeDispatchUpdatesListener(listener: OnDispatchUpdatesListener) {
        _dispatchUpdatesSet.remove(listener)
    }

    /**Diff调用处理*/
    internal inner class DiffRunnable : Runnable {
        var _params: FilterParams? = null
        var _newList: List<DslAdapterItem>? = null
        var _diffResult: DiffUtil.DiffResult? = null

        val _resultRunnable = Runnable {
            //因为是异步操作, 所以在 [dispatchUpdatesTo] 时, 才覆盖 filterDataList 数据源

            val oldSize = filterDataList.size
            var newSize = 0

            val diffList = _newList
            _newList = null

            diffList?.let {
                newSize = it.size
                filterDataList.clear()
                filterDataList.addAll(it)
            }

            diffList?.forEach {
                //清空标志
                it.itemChanging = false
            }

            val updateDependItemList = _getUpdateDependItemList()

            //是否调用了[Dispatch]
            var isDispatchUpdatesTo = false

            if (_params?.justFilter == true) {
                //仅过滤数据源,不更新界面
                //跳过 dispatchUpdatesTo
            } else {
                //根据diff, 更新adapter
                if (updateDependItemList.isEmpty() &&
                    _params?.updateDependItemWithEmpty == false &&
                    oldSize == newSize
                ) {
                    //跳过[dispatchUpdatesTo]刷新界面, 但是要更新自己
                    dslAdapter.notifyItemChanged(
                        _params?.fromDslAdapterItem,
                        _params?.payload,
                        true
                    )
                } else {
                    _diffResult?.dispatchUpdatesTo(dslAdapter)
                    isDispatchUpdatesTo = true
                }
            }

            notifyUpdateDependItem(updateDependItemList)

            if (isDispatchUpdatesTo && _dispatchUpdatesSet.isNotEmpty()) {
                val updatesSet = mutableSetOf<OnDispatchUpdatesListener>()
                updatesSet.addAll(_dispatchUpdatesSet)
                updatesSet.forEach {
                    it.onDispatchUpdatesAfter(dslAdapter)
                }
            }

            //_lastDiffSubmit = null
            _lastDiffRunnable = null
            _diffResult = null
            _params = null
        }

        override fun run() {
            val startTime = nowTime()
            L.d("开始计算Diff:$startTime")
            val diffResult = calculateDiff()
            val nowTime = nowTime()
            val s = (nowTime - startTime) / 1000
            val ms = ((nowTime - startTime) % 1000) * 1f / 1000
            L.i("Diff计算耗时:${String.format("%.3f", s + ms)}s")
            _diffResult = diffResult

            //回调到主线程
            if (Looper.getMainLooper() == Looper.myLooper()) {
                _resultRunnable.run()
            } else {
                mainHandler.post(_resultRunnable)
            }
        }

        /**计算[Diff]*/
        fun calculateDiff(): DiffUtil.DiffResult {

            //2个数据源
            val oldList = ArrayList(filterDataList)
            val newList = filterItemList(dslAdapter.adapterItems)

            //异步操作, 先保存数据源
            _newList = onFilterDataList(oldList, newList)

            //开始计算diff
            val diffResult = DiffUtil.calculateDiff(
                RDiffCallback(
                    oldList,
                    _newList,
                    object :
                        RItemDiffCallback<DslAdapterItem> {

                        override fun areItemsTheSame(
                            oldData: DslAdapterItem,
                            newData: DslAdapterItem
                        ): Boolean {
                            return oldData.thisAreItemsTheSame(
                                _params?.fromDslAdapterItem,
                                newData
                            )
                        }

                        override fun areContentsTheSame(
                            oldData: DslAdapterItem,
                            newData: DslAdapterItem
                        ): Boolean {
                            return oldData.thisAreContentsTheSame(
                                _params?.fromDslAdapterItem,
                                newData
                            )
                        }

                        override fun getChangePayload(
                            oldData: DslAdapterItem,
                            newData: DslAdapterItem
                        ): Any? {
                            return oldData.thisGetChangePayload(
                                _params?.fromDslAdapterItem,
                                _params?.payload,
                                newData
                            )
                        }
                    }
                )
            )

            return diffResult
        }

        fun _getUpdateDependItemList(): List<DslAdapterItem> {
            //需要通知更新的子项
            val notifyChildFormItemList = mutableListOf<DslAdapterItem>()

            _params?.fromDslAdapterItem?.let { fromItem ->
                dslAdapter.getValidFilterDataList().forEachIndexed { index, dslAdapterItem ->
                    if (fromItem.isItemInUpdateList(dslAdapterItem, index)) {
                        notifyChildFormItemList.add(dslAdapterItem)
                    }
                }
            }

            return notifyChildFormItemList
        }

        fun notifyUpdateDependItem(itemList: List<DslAdapterItem>) {
            if (_params?.fromDslAdapterItem == null) {
                return
            }

            itemList.forEachIndexed { index, dslAdapterItem ->
                dslAdapterItem.apply {
                    onItemUpdateFromInner(_params!!.fromDslAdapterItem!!)
                    dslAdapterItem.updateAdapterItem(true)
                }

                L.v("$index. 通知更新:${dslAdapterItem.javaClass.simpleName} ${dslAdapterItem.itemTag}")
            }

            _params = null
        }

        //仅仅只是通知更新被依赖的子项关系
        fun notifyUpdateDependItem() {
            notifyUpdateDependItem(_getUpdateDependItemList())
        }
    }

    /**抖动过滤处理*/
    internal inner class UpdateDependRunnable : Runnable {
        var _diffRunnable: DiffRunnable? = null
        //diff操作取消
        private var _lastDiffSubmit: Future<*>? = null

        override fun run() {
            if (_diffRunnable == null) {
                return
            }
            _lastDiffSubmit?.cancel(true)

            _diffRunnable?.run {
                if (_params?.async == true) {
                    _lastDiffSubmit = asyncExecutor.submit(this)
                } else {
                    this.run()
                }
            }
        }
    }
}

data class FilterParams(
    /**
     * 触发更新的来源, 定向更新其子项.
     * */
    val fromDslAdapterItem: DslAdapterItem? = null,
    /**
     * 异步计算Diff
     * */
    var async: Boolean = true,
    /**
     * 立即执行, 不检查抖动
     * */
    var just: Boolean = false,
    /**
     * 只过滤列表数据, 不通知界面操作, 开启此属性.[async=true] [just=true]
     * */
    var justFilter: Boolean = false,
    /**
     * 前提, Diff 之后, 2个数据列表的大小要一致.
     *
     * 当依赖的[DslAdapterItem] [isItemInUpdateList]列表为空时, 是否要调用[dispatchUpdatesTo]更新界面
     * */
    var updateDependItemWithEmpty: Boolean = true,

    /**局部更新标识参数*/
    var payload: Any? = null,

    /**自定义的扩展数据传递*/
    var filterData: Any? = null,

    /**默认是节流模式*/
    var shakeType: Int = OnceHandler.SHAKE_TYPE_THROTTLE,

    /**抖动检查延迟时长*/
    var shakeDelay: Long = 16
)

typealias DispatchUpdates = (dslAdapter: DslAdapter) -> Unit

interface OnDispatchUpdatesListener {
    /**
     * 当触发了[dispatchUpdatesTo]后回调
     * */
    fun onDispatchUpdatesAfter(dslAdapter: DslAdapter)
}