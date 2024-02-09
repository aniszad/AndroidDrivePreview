package com.az.googledrivelibraryxml.utils

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class TextViewOpenSans(context: Context, attributeSet: AttributeSet) : AppCompatTextView(context, attributeSet) {

    init {
        typeface = Typeface.createFromAsset(context.assets, "open_sans_regular.ttf")
    }

}