package com.az.googledrivelibraryxml.managers

import java.io.InputStream

interface GdCredentialsProvider {
    fun getCredentials():InputStream?
}