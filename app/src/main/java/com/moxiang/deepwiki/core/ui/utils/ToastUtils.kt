package com.moxiang.deepwiki.core.ui.utils

import android.content.Context
import android.widget.Toast

/**
 * Toast 工具扩展函数
 */
fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

fun Context.showLongToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}
