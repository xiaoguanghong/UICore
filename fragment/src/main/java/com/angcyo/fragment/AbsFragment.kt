package com.angcyo.fragment

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import com.angcyo.base.getColor
import com.angcyo.widget.DslViewHolder

/**
 * Created by angcyo on 2018/12/03 23:17
 *
 * 一些生命周期日志的输出,和创建跟视图
 *
 * @author angcyo
 */
abstract class AbsFragment : Fragment() {

    //<editor-fold desc="对象变量">

    /**
     * ViewHolder 中 SparseArray 初始化的容量, 防止扩容带来的性能损失
     */
    var viewHolderInitialCapacity: Int = DslViewHolder.DEFAULT_INITIAL_CAPACITY

    //</editor-fold desc="对象变量">

    //<editor-fold desc="对象属性">

    lateinit var baseViewHolder: DslViewHolder

    lateinit var attachContext: Context

    //</editor-fold">

    //<editor-fold desc="生命周期, 系统的方法">

    /**
     * 此方法, 通常在 hide show fragment的时候调用
     */
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
    }

    /**
     * 此方法, 通常在 FragmentStatePagerAdapter 中调用
     */
    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
    }

    override fun onAttachFragment(childFragment: Fragment) {
        super.onAttachFragment(childFragment)
    }

    override fun getContext(): Context {
        return super.getContext() ?: attachContext
    }

    /**
     * OnAttach -> OnCreate -> OnCreateView (initBaseView) -> OnActivityCreated -> OnViewStateRestored -> OnStart -> OnResume
     */
    override fun onAttach(context: Context) {
        super.onAttach(context)
        attachContext = context
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val layoutId = getFragmentLayoutId()
        val rootView: View
        rootView = if (layoutId != -1) {
            inflater.inflate(layoutId, container, false)
        } else {
            createRootView()
        }
        baseViewHolder = DslViewHolder(rootView, viewHolderInitialCapacity)
        initBaseView(savedInstanceState)
        return rootView
    }

    /**
     * 状态恢复, 回调顺序 最优先
     */
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
    }

    /**
     * View需要恢复状态
     */
    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
    }

    /**
     * OnPause -> OnStop -> OnDestroyView -> OnDestroy -> OnDetach
     */
    override fun onPause() {
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        baseViewHolder.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onDetach() {
        super.onDetach()
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        onOrientationChanged(newConfig.orientation)
    }

    //</editor-fold>

    //<editor-fold desc="自定义的方法">

    open fun onOrientationChanged(orientation: Int) {
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) { //切换到横屏
            onOrientationToLandscape()
        } else if (orientation == Configuration.ORIENTATION_PORTRAIT) { //切换到竖屏
            onOrientationToPortrait()
        }
    }

    open fun onOrientationToLandscape() {}

    open fun onOrientationToPortrait() {}

    /**根布局*/
    @LayoutRes
    open fun getFragmentLayoutId() = -1

    /**
     * 不指定布局Id的时候, 可以用代码创建跟视图
     */
    open fun createRootView(): View {
        val view = View(context)
        view.setBackgroundColor(getColor(R.color.status_bar_color))
        return view
    }

    /**初始化布局, 此时的[View]还没有[attach]*/
    open fun initBaseView(savedInstanceState: Bundle?) {

    }

    //</editor-fold>
}