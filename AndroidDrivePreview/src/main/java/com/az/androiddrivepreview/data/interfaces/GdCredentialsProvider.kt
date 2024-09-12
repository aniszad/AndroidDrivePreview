package com.az.androiddrivepreview.data.interfaces

import java.io.InputStream

/**
 * Gd credentials provider
 *
 * @constructor Create empty Gd credentials provider
 */
public interface GdCredentialsProvider {
    /**
     * Get credentials
     *
     * @return
     */
    fun getCredentials():InputStream?
}