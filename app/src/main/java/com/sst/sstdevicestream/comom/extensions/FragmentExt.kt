package com.sst.sstdevicestream.comom.extensions

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

fun Fragment.pop() {
    try {
        findNavController().navigateUp()
    } catch (e: IllegalArgumentException) {
        e.stackTrace
    }
}