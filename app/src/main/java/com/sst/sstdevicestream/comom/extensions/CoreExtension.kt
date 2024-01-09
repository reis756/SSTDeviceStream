package com.sst.sstdevicestream.comom.extensions

inline fun <T1, T2, R> safeLet(
    p1: T1?,
    p2: T2?,
    block: (T1, T2) -> R
): R? {
    return p1?.let { safeP1 ->
        p2?.let { safeP2 ->
            block(safeP1, safeP2)
        }
    }
}

val <T> T.exhaustive: T
    get() = this
