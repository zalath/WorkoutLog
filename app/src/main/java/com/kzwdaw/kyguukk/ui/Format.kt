package com.kzwdaw.kyguukk.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateTimeFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

fun Long.toDateTimeStr(): String = dateTimeFmt.format(Date(this))

fun Long.toDateStr(): String = dateFmt.format(Date(this))

/** 格式化测量值，保留1位小数。 */
fun Double.fmtCm(): String = "%.1f".format(this)

/** 安全格式化 nullable 值。 */
fun Double?.fmtCmOrDash(): String = if (this == null) "—" else "%.1f".format(this)
