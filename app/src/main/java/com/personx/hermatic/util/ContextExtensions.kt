package com.personx.hermatic.util

import android.content.Context
import android.content.ContextWrapper
import com.personx.hermatic.MainActivity

fun Context.findActivity(): MainActivity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is MainActivity) return context
        context = context.baseContext
    }
    return null
}
