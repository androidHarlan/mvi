package com.badoo.mvicoredemo.ui.common

import android.content.Context
import android.graphics.PorterDuff
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import com.badoo.mvicoredemo.R

class ToggleButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatButton(context, attrs, defStyleAttr) {

    fun toggle(toggle: Boolean) {
        if (toggle) {
            background.setColorFilter(
                ContextCompat.getColor(context, R.color.colorAccent),
                PorterDuff.Mode.SRC_ATOP
            )
        } else {
            background.clearColorFilter()
        }
    }
}
