package com.az.androiddrivepreview.utils

import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Custom date formatter
 *
 * @constructor Create empty Custom date formatter
 */
class CustomDateFormatter {
    /**
     * Format date
     *
     * @param date
     * @return
     */
    fun formatDate(date: String): String {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        val parsedDate = inputFormat.parse(date)
        return outputFormat.format(parsedDate!!)
    }
}