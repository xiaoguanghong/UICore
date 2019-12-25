package com.angcyo.widget.base

import android.app.Activity
import android.graphics.Rect
import android.view.*
import android.widget.EditText
import androidx.annotation.LayoutRes
import com.angcyo.widget.layout.RSoftInputLayout

/**
 * Kotlin ViewGroup的扩展
 * Created by angcyo on 2017-07-26.
 */
/**
 * 计算child在parent中的位置坐标, 请确保child在parent中.
 * */
public fun ViewGroup.getLocationInParent(child: View, location: Rect) {
    var x = 0
    var y = 0

    var view = child
    while (view.parent != this) {
        x += view.left
        y += view.top
        view = view.parent as View
    }

    x += view.left
    y += view.top

    location.set(x, y, x + child.measuredWidth, y + child.measuredHeight)
}

/**返回当软键盘弹出时, 布局向上偏移了多少距离*/
public fun View.getLayoutOffsetTopWidthSoftInput(): Int {
    val rect = Rect()
    var offsetTop = 0

    try {
        val activity = this.context as Activity
        val softInputMode = activity.window.attributes.softInputMode
        if (softInputMode == WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN) {
            val keyboardHeight = RSoftInputLayout.getSoftKeyboardHeight(this)

            /**在ADJUST_PAN模式下, 键盘弹出时, 坐标需要进行偏移*/
            if (keyboardHeight > 0) {
                //return targetView
                val findFocus = this.findFocus()
                if (findFocus is EditText) {
                    findFocus.getWindowVisibleDisplayFrame(rect)
                    offsetTop = findFocus.bottom - rect.bottom
                }
            }
        }

    } catch (e: Exception) {
    }
    return offsetTop
}


/**获取touch坐标对应的RecyclerView, 如果没有则null*/
public fun ViewGroup.getTouchOnRecyclerView(
    touchRawX: Float,
    touchRawY: Float
): androidx.recyclerview.widget.RecyclerView? {
    return findRecyclerView(touchRawX, touchRawY)
}

public fun ViewGroup.getTouchOnRecyclerView(event: MotionEvent): androidx.recyclerview.widget.RecyclerView? {
    return findRecyclerView(event.rawX, event.rawY)
}

/**
 * 根据touch坐标, 返回touch的View
 */
public fun ViewGroup.findView(
    event: MotionEvent,
    intercept: (View, Rect) -> Boolean = { _, _ -> false },
    jumpTarget: (View, Rect) -> Boolean = { _, _ -> false }
): View? {
    return findView(
        this,
        event.rawX,
        event.rawY,
        getLayoutOffsetTopWidthSoftInput(),
        intercept,
        jumpTarget
    )
}

public fun ViewGroup.findView(
    touchRawX: Float,
    touchRawY: Float,
    intercept: (View, Rect) -> Boolean = { _, _ -> false },
    jumpTarget: (View, Rect) -> Boolean = { _, _ -> false }
): View? {
    return findView(
        this,
        touchRawX,
        touchRawY,
        getLayoutOffsetTopWidthSoftInput(),
        intercept,
        jumpTarget
    )
}

public fun ViewGroup.findView(
    targetView: View /*判断需要结束的View*/,
    touchRawX: Float,
    touchRawY: Float,
    offsetTop: Int = 0,
    /*是否需要拦截View, 拦截后 立马返回. 通常用来拦截ViewGroup, 防止枚举目标ViewGroup*/
    intercept: (View, Rect) -> Boolean = { _, _ -> false },
    /*找到目标后, 是否需要跳过目标继续搜索*/
    jumpTarget: (View, Rect) -> Boolean = { _, _ -> false }
): View? {
    /**键盘的高度*/
    var touchView: View? = targetView
    val rect = Rect()

    for (i in childCount - 1 downTo 0) {
        val childAt = getChildAt(i)

        if (childAt.visibility != View.VISIBLE) {
            continue
        }

//        childAt.getWindowVisibleDisplayFrame(rect)
//        L.e("${this}:1 ->$i $rect")
        childAt.getGlobalVisibleRect(rect)
//        L.e("${this}:2 ->$i $rect")
//        L.e("call: ------------------end -> ")
        rect.offset(0, -offsetTop)

        //检查当前view, 是否在 touch坐标中
        fun check(view: View): View? {
            if (view.visibility == View.VISIBLE &&
                view.measuredHeight != 0 &&
                view.measuredWidth != 0 &&
                (view.left != view.right) &&
                (view.top != view.bottom) &&
                rect.contains(touchRawX.toInt(), touchRawY.toInt())
            ) {
                return view
            }
            return null
        }

        val checkView = check(childAt)

        //拦截处理
        if (checkView != null && intercept.invoke(childAt, rect)) {
            touchView = childAt
            break
        }

        if (childAt is ViewGroup && childAt.childCount > 0) {
            val resultView =
                childAt.findView(targetView, touchRawX, touchRawY, offsetTop, intercept, jumpTarget)
            if (resultView != null && resultView != targetView) {
                if (jumpTarget.invoke(resultView, rect)) {

                } else {
                    touchView = resultView
                    break
                }
            } else {
                if (checkView != null) {
                    if (jumpTarget.invoke(checkView, rect)) {

                    } else {
                        touchView = checkView
                        break
                    }
                }
            }
        } else {
            if (checkView != null) {
                if (jumpTarget.invoke(checkView, rect)) {

                } else {
                    touchView = checkView
                    break
                }
            }
        }
    }
    return touchView
}

public fun ViewGroup.findRecyclerView(
    touchRawX: Float,
    touchRawY: Float
): androidx.recyclerview.widget.RecyclerView? {
    /**键盘的高度*/
    var touchView: androidx.recyclerview.widget.RecyclerView? = null

    val findView = findView(touchRawX, touchRawY,
        { view, _ ->
            view is androidx.recyclerview.widget.RecyclerView
        }, { view, _ ->
            view !is androidx.recyclerview.widget.RecyclerView
        })

    if (findView is androidx.recyclerview.widget.RecyclerView) {
        touchView = findView
    }

    return touchView
}

/**枚举所有child view*/
public fun ViewGroup.childs(map: (index: Int, child: View) -> Unit) {
    for (index in 0 until childCount) {
        val childAt = getChildAt(index)
        map.invoke(index, childAt)
    }
}

public fun ViewGroup.inflate(@LayoutRes layoutId: Int, attachToRoot: Boolean = true): View {
    if (layoutId == -1) {
        return this
    }
    return LayoutInflater.from(context).inflate(layoutId, this, attachToRoot)
}