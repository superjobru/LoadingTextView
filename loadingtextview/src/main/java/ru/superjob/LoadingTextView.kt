package ru.superjob

import android.content.Context
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.support.annotation.DrawableRes
import android.support.graphics.drawable.AnimatedVectorDrawableCompat
import android.support.v4.content.ContextCompat
import android.support.v7.content.res.AppCompatResources
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import ru.superjob.loadingtextview.R
import java.util.concurrent.TimeUnit

class LoadingTextView : TextView {

	private var actionClickListener: ActionClickListener? = null
	private lateinit var animatedProgressDrawable: AnimatedRightDrawable
	private lateinit var staticDrawable: StaticRightDrawable
	private var leftDrawable: Drawable? = null
	private var loadingInProgress: Boolean = false
	private var currentState: DrawableState? = null
	private var timeout: Long = 0


	constructor(context: Context) : super(context) {
		init(null, 0)
	}

	constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
		init(attrs, 0)
	}

	constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
		init(attrs, defStyle)
	}

	private fun init(attrs: AttributeSet?, defStyle: Int) {
		val a = context.obtainStyledAttributes(
				attrs, R.styleable.LoadingTextView, defStyle, 0)

		val loadingAnimation = a.getResourceId(R.styleable.LoadingTextView_customLoadAnimation, R.drawable.ic_loading)
		animatedProgressDrawable = AnimatedRightDrawable(context, loadingAnimation)

		val styleResIconRight = if (a.hasValue(R.styleable.LoadingTextView_android_drawableEnd)) R.styleable.LoadingTextView_android_drawableEnd
		else R.styleable.LoadingTextView_android_drawableRight

		timeout = a.getInteger(R.styleable.LoadingTextView_timeout, 0).toLong()

		val rightDrawable = a.getResourceId(styleResIconRight, R.drawable.ic_near_me_gray)
		staticDrawable = StaticRightDrawable(context, rightDrawable)

		val leftDrawableId = a.getResourceId(R.styleable.LoadingTextView_android_drawableStart, 0)
		if (leftDrawableId != 0) leftDrawable = ContextCompat.getDrawable(context, leftDrawableId)
		a.recycle()

		setCompoundDrawablesRelativeWithIntrinsicBounds(leftDrawable, null, staticDrawable.getDrawable(), null)

		setOnTouchListener(View.OnTouchListener { _, event ->
			if (event.action == MotionEvent.ACTION_DOWN) {
				if (event.rawX >= (this.right - this.compoundPaddingRight)) {
					if (!loadingInProgress) {
						actionClickListener?.onClicked()
					}
					return@OnTouchListener true
				}
			}
			return@OnTouchListener false
		})
		currentState = staticDrawable
	}

	private fun setState(state: DrawableState) {
		currentState = state
		setCompoundDrawablesRelativeWithIntrinsicBounds(leftDrawable, compoundDrawables[1], state.getDrawable(), compoundDrawables[3])
		state.run()
	}

	fun setRightDrawable(@DrawableRes drawable: Int) {
		staticDrawable = StaticRightDrawable(context, drawable)
		currentState?.let { setState(it) }
	}

	fun setLeftDrawable(@DrawableRes drawable: Int) {
		leftDrawable = ContextCompat.getDrawable(context, drawable)
		currentState?.let { setState(it) }
	}

	fun setResultText(text: CharSequence) {
		setText(text)
		setLoading(false)
	}

	fun setTimeOut(timeout: Long) {
		this.timeout = timeout
	}

	fun setLoading(loading: Boolean) {
		loadingInProgress = loading
		if (loading) {
			if (timeout > 0) {
				runCountDownTimer(timeout)
			}
			setState(animatedProgressDrawable)
		} else {
			setState(staticDrawable)
		}
	}

	private fun runCountDownTimer(timeout: Long) {
		handler?.postDelayed({
								 setLoading(false)
							 }, TimeUnit.SECONDS.toMillis(timeout))
	}

	fun setCustomLoadingDrawable(@DrawableRes animatedVector: Int) {
		animatedProgressDrawable = AnimatedRightDrawable(context, animatedVector)
	}

	interface DrawableState {
		fun getDrawable(): Drawable?
		fun run()
	}

	private class AnimatedRightDrawable(context: Context, drawableRes: Int) : DrawableState {
		private var animatedVector: AnimatedVectorDrawableCompat? = null

		init {
			val drawable = AppCompatResources.getDrawable(context, drawableRes)

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				if (drawable is AnimatedVectorDrawable) {
					animatedVector = AnimatedVectorDrawableCompat.create(context, drawableRes)
				} else if (drawable is AnimatedVectorDrawableCompat) {
					animatedVector = drawable
				}

			} else {
				if (drawable is AnimatedVectorDrawableCompat) {
					animatedVector = drawable
				}
			}
			if (animatedVector == null) {
				throw IllegalStateException("Drawable type must be AnimatedVector!")
			}

		}

		override fun getDrawable(): AnimatedVectorDrawableCompat? {
			return animatedVector
		}

		override fun run() {
			animatedVector?.start()
		}
	}

	override fun onDetachedFromWindow() {
		handler?.removeCallbacksAndMessages(null)
		super.onDetachedFromWindow()
	}

	private class StaticRightDrawable(context: Context, drawable: Int) : DrawableState {
		private val staticRightDrawable = ContextCompat.getDrawable(context, drawable)

		override fun run() {
			//nothing
		}

		override fun getDrawable(): Drawable? {
			return staticRightDrawable
		}

	}

	fun setActionClickListener(actionClickListener: ActionClickListener) {
		this.actionClickListener = actionClickListener
	}

	fun setActionClickListener(action: () -> Unit) {
		this.actionClickListener = object : ActionClickListener {
			override fun onClicked() {
				action.invoke()
			}
		}
	}

	interface ActionClickListener {
		fun onClicked()
	}
}