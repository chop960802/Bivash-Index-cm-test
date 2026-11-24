package com.example.logic

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.security.MessageDigest

object ChecksumUtil {
  val moshi: Moshi by lazy {
    Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
  }

  inline fun <reified T> sha256Of(value: T): String {
    val adapter = moshi.adapter(T::class.java)
    val canonicalJson = adapter.toJson(value)
    return sha256String(canonicalJson)
  }

  fun sha256String(input: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
  }
}
