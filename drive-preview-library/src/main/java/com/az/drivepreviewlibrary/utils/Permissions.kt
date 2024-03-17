package com.az.drivepreviewlibrary.utils

/**
 * Permissions
 *
 * @constructor Create empty Permissions
 */
public enum class Permissions {
    /**
     * Strict
     *
     * @constructor Create empty Strict
     */
    STRICT, // viewing only

    /**
     * User
     *
     * @constructor Create empty User
     */
    USER,  // viewing, downloading, sharing

    /**
     * Admin
     *
     * @constructor Create empty Admin
     */
    ADMIN, // viewing, downloading, sharing, deleting
}