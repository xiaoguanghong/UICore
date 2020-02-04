package com.angcyo.widget.base

import android.animation.Animator
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.annotation.TargetApi
import android.os.Build
import android.view.View
import android.view.ViewAnimationUtils
import android.view.animation.Animation
import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import com.angcyo.library.ex.c

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2019/12/20
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */

fun anim(from: Int, to: Int, config: AnimatorConfig.() -> Unit = {}): ValueAnimator {
    return _animator(ValueAnimator.ofInt(from, to), config)
}

fun anim(from: Float, to: Float, config: AnimatorConfig.() -> Unit = {}): ValueAnimator {
    return _animator(ValueAnimator.ofFloat(from, to), config)
}

fun _animator(animator: ValueAnimator, config: AnimatorConfig.() -> Unit = {}): ValueAnimator {
    val animatorConfig = AnimatorConfig()

    animator.duration = 300
    animator.interpolator = LinearInterpolator()
    animator.addUpdateListener {
        animatorConfig.onAnimatorUpdateValue(it.animatedValue, it.animatedFraction)
    }
    animator.addListener(RAnimatorListener().apply {
        onAnimatorFinish = { _, _ ->
            animatorConfig.onAnimatorEnd(animator)
        }
    })

    animatorConfig.config()
    animatorConfig.onAnimatorConfig(animator)

    animator.start()
    return animator
}

class AnimatorConfig {
    var onAnimatorConfig: (animator: ValueAnimator) -> Unit = {}

    var onAnimatorUpdateValue: (value: Any, fraction: Float) -> Unit = { _, _ -> }

    var onAnimatorEnd: (animator: ValueAnimator) -> Unit = {}
}

/**缩放属性动画*/
fun View.scale(
    from: Float,
    to: Float,
    duration: Long = 300,
    interpolator: Interpolator = LinearInterpolator(),
    onEnd: () -> Unit = {}
): ValueAnimator {
    return anim(from, to) {
        onAnimatorUpdateValue = { value, _ ->
            scaleX = value as Float
            scaleY = scaleX
        }

        onAnimatorConfig = {
            it.duration = duration
            it.interpolator = interpolator
            onAnimatorEnd = { _ -> onEnd() }
        }
    }
}

/**平移属性动画*/
fun View.translationX(
    from: Float,
    to: Float,
    duration: Long = 300,
    interpolator: Interpolator = LinearInterpolator(),
    onEnd: () -> Unit = {}
): ValueAnimator {
    return anim(from, to) {
        onAnimatorUpdateValue = { value, _ ->
            translationX = value as Float
        }

        onAnimatorConfig = {
            it.duration = duration
            it.interpolator = interpolator
            onAnimatorEnd = { _ -> onEnd() }
        }
    }
}

fun View.translationY(
    from: Float,
    to: Float,
    duration: Long = 300,
    interpolator: Interpolator = LinearInterpolator(),
    onEnd: () -> Unit = {}
): ValueAnimator {
    return anim(from, to) {
        onAnimatorUpdateValue = { value, _ ->
            translationY = value as Float
        }

        onAnimatorConfig = {
            it.duration = duration
            it.interpolator = interpolator
            onAnimatorEnd = { _ -> onEnd() }
        }
    }
}

/**补间动画*/
fun View.rotateAnimation(
    fromDegrees: Float = 0f,
    toDegrees: Float = 360f,
    duration: Long = 300,
    interpolator: Interpolator = LinearInterpolator(),
    config: RotateAnimation.() -> Unit = {},
    onEnd: (animation: Animation) -> Unit = {}
): RotateAnimation {
    return RotateAnimation(
        fromDegrees,
        toDegrees,
        RotateAnimation.RELATIVE_TO_SELF,
        0.5f,
        RotateAnimation.RELATIVE_TO_SELF,
        0.5f
    ).apply {
        this.duration = duration
        this.interpolator = interpolator
        setAnimationListener(object : RAnimationListener() {
            override fun onAnimationEnd(animation: Animation) {
                onEnd(animation)
            }
        })
        config()
        this@rotateAnimation.startAnimation(this)
    }
}


/**
 * 揭露动画
 * https://developer.android.com/training/animation/reveal-or-hide-view#Reveal
 * */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
fun View.reveal(action: RevealConfig.() -> Unit = {}) {
    this.doOnPreDraw {
        val config = RevealConfig()
        config.centerX = this.measuredWidth / 2
        config.centerY = this.measuredHeight / 2
        config.endRadius = c(config.centerX.toDouble(), config.centerY.toDouble()).toFloat()

        //第一次获取基础数据
        config.action()

        ViewAnimationUtils.createCircularReveal(
            this,
            config.centerX,
            config.centerY,
            config.startRadius,
            config.endRadius
        ).apply {
            duration = 240

            config.animator = this
            //第二次获取动画数据
            config.action()
            start()
        }
    }
}

data class RevealConfig(
    var animator: Animator? = null,

    //默认是视图的中心
    var centerX: Int = 0,
    var centerY: Int = 0,

    var startRadius: Float = 0f,
    //默认是视图的对角半径
    var endRadius: Float = 0f
)

fun View.bgColorAnimator(
    from: Int,
    to: Int,
    duration: Long = 300,
    onEnd: (cancel: Boolean) -> Unit = {},
    config: ValueAnimator.() -> Unit = {}
) {
    //背景动画
    val colorAnimator = ValueAnimator.ofObject(ArgbEvaluator(), from, to)
    colorAnimator.addUpdateListener { animation ->
        val color = animation.animatedValue as Int
        setBackgroundColor(color)
    }
    colorAnimator.addListener(object : RAnimatorListener() {
        override fun onAnimatorFinish(animator: Animator, fromCancel: Boolean) {
            super.onAnimatorFinish(animator, fromCancel)
            onEnd(fromCancel)
        }
    })
    colorAnimator.interpolator = LinearInterpolator()
    colorAnimator.duration = duration
    colorAnimator.config()
    colorAnimator.start()
}