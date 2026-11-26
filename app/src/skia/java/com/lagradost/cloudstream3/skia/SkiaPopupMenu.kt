package com.lagradost.cloudstream3.skia

import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.lagradost.cloudstream3.R
import kotlin.math.roundToInt

/**
 * Small floating menu (icon + label rows) anchored to a button, in the Skia surface style.
 * Used where upstream has a row of icon buttons that Skia folds into one "more" button.
 */
object SkiaPopupMenu {
    class Item(val icon: Drawable?, val label: CharSequence, val onClick: () -> Unit)

    fun show(anchor: View, items: List<Item>) {
        if (items.isEmpty()) return
        val ctx = anchor.context
        val density = ctx.resources.displayMetrics.density
        fun dp(value: Int) = (value * density).roundToInt()
        fun attr(id: Int): Int {
            val out = TypedValue()
            ctx.theme.resolveAttribute(id, out, true)
            return out.data
        }

        val textColor = attr(R.attr.textColor)
        val list = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            minimumWidth = dp(220)
            setPadding(0, dp(6), 0, dp(6))
            background = GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setColor(ContextCompat.getColor(ctx, R.color.skia_surface_high))
                setStroke(dp(1), ContextCompat.getColor(ctx, R.color.skia_line))
            }
            elevation = dp(12).toFloat()
            clipToOutline = true
        }

        val popup = PopupWindow(
            list,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            setBackgroundDrawable(ColorDrawable(0))
            elevation = dp(12).toFloat()
            animationStyle = android.R.style.Animation_Dialog
        }

        val ripple = TypedValue().also {
            ctx.theme.resolveAttribute(android.R.attr.selectableItemBackground, it, true)
        }.resourceId

        for (item in items) {
            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                minimumHeight = dp(48)
                setPadding(dp(16), 0, dp(20), 0)
                if (ripple != 0) setBackgroundResource(ripple)
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    popup.dismiss()
                    item.onClick()
                }
            }
            row.addView(ImageView(ctx).apply {
                setImageDrawable(item.icon)
                imageTintList = ColorStateList.valueOf(textColor)
            }, LinearLayout.LayoutParams(dp(20), dp(20)))
            row.addView(TextView(ctx).apply {
                text = item.label
                setTextColor(textColor)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                maxLines = 1
            }, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { marginStart = dp(14) })
            list.addView(row, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ))
        }

        anchor.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        // Right edge level with the anchor's right edge, just below it.
        list.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val location = IntArray(2).also { anchor.getLocationInWindow(it) }
        val x = (location[0] + anchor.width - list.measuredWidth).coerceAtLeast(dp(8))
        val y = location[1] + anchor.height + dp(8)
        popup.showAtLocation(anchor, Gravity.TOP or Gravity.START, x, y)
    }
}
