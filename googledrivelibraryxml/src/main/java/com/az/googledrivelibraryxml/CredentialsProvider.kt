package com.az.googledrivelibraryxml

import com.az.googledrivelibraryxml.managers.GdCredentialsProvider
import java.io.FileNotFoundException
import java.io.InputStream

class CredentialsProvider() : GdCredentialsProvider {
    override fun getCredentials(): InputStream {
        return this@CredentialsProvider::class.java.classLoader!!.getResourceAsStream(
                "res/raw/credentials.json"
            )
                ?: throw FileNotFoundException("credentials resource not found")

    }
}