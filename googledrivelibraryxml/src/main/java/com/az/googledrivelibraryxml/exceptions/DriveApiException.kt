package com.az.googledrivelibraryxml.exceptions

import java.io.IOException

class DriveApiException(message: String, cause: Throwable? = null) : IOException(message, cause)