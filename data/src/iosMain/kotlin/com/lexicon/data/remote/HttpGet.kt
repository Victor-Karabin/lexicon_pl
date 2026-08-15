@file:OptIn(ExperimentalForeignApi::class)

package com.lexicon.data.remote

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSCharacterSet
import platform.Foundation.NSData
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSURLSession
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.URLQueryAllowedCharacterSet
import platform.Foundation.create
import platform.Foundation.dataTaskWithRequest
import platform.Foundation.setValue
import platform.Foundation.stringByAddingPercentEncodingWithAllowedCharacters
import kotlin.coroutines.resume

private const val OK_RANGE_START = 200
private const val OK_RANGE_END = 299

/**
 * A GET, as a suspend function.
 *
 * The image and translation clients on Android are Retrofit over OkHttp, neither of
 * which exists here. Rather than take a Ktor dependency for four calls, this is
 * NSURLSession with the callback turned into a coroutine — the whole HTTP surface
 * this app needs on iOS is "fetch a URL and give me the text back".
 */
suspend fun httpGet(
    url: String,
    headers: Map<String, String> = emptyMap(),
): String? {
    val target = NSURL.URLWithString(url) ?: return null
    val request = NSMutableURLRequest.requestWithURL(target)
    headers.forEach { (name, value) -> request.setValue(value, forHTTPHeaderField = name) }

    return suspendCancellableCoroutine { continuation ->
        val task = NSURLSession.sharedSession.dataTaskWithRequest(request) { data, response, _ ->
            val status = (response as? NSHTTPURLResponse)?.statusCode?.toInt()
            val body = if (status in OK_RANGE_START..OK_RANGE_END) (data as? NSData)?.asText() else null
            if (continuation.isActive) continuation.resume(body)
        }
        continuation.invokeOnCancellation { task.cancel() }
        task.resume()
    }
}

/** Percent-encoding for a value going into a query string. */
fun String.urlEncoded(): String =
    (this as NSString)
        .stringByAddingPercentEncodingWithAllowedCharacters(NSCharacterSet.URLQueryAllowedCharacterSet)
        ?: this

private fun NSData.asText(): String? = NSString.create(data = this, encoding = NSUTF8StringEncoding) as String?
