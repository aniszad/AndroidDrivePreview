package com.az.drivepreviewlibrary.data.managers

import java.io.InputStream

/**
 * Gd credentials provider
 *
 * @constructor Create empty Gd credentials provider
 */
interface GdCredentialsProvider {
    /**
     * Get credentials
     *
     * @return
     */
    fun getCredentials():InputStream?
}