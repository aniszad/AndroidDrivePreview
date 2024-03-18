package com.az.drivepreviewlibrary.data.exceptions


/**
 * Drive download exception
 *
 * @constructor
 *
 * @param message
 * @param cause
 */
class DriveDownloadException(message: String, cause: Throwable? = null) : Exception(message, cause)