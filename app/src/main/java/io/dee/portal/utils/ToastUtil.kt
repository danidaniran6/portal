package io.dee.portal.utils

import android.content.Context

fun String.toast(context: Context) {
    android.widget.Toast.makeText(context, this, android.widget.Toast.LENGTH_SHORT).show()
}

fun Context.toast(message: String) {
    android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()

}