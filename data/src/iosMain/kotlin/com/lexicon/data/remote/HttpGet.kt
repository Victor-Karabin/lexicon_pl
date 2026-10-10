@file:OptIn(ExperimentalForeignApi::class)

package com.lexicon.data.remote

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSCharacterSet
import platform.Foundation.NSData
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSMutableCharacterSet
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSURLSession
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.URLQueryAllowedCharacterSet
import platform.Foundation.create
import platform.Foundation.dataTaskWithRequest
import platform.Foundation.dataUsingEncoding
import platform.Foundation.setHTTPBody
import platform.Foundation.setHTTPMethod
import platform.Foundation.setValue
import platform.Foundation.stringByAddingPercentEncodingWithAllowedCharacters
import kotlin.coroutines.resume

private const val OK_RANGE_START = 200
private const val OK_RANGE_END = 299

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

sealed interface HttpReply {
    data class Ok(val body: String) : HttpReply

    data class Failed(val status: Int?) : HttpReply

    data object Offline : HttpReply
}

suspend fun httpPost(
    url: String,
    headers: Map<String, String>,
    body: String,
): HttpReply {
    val target = NSURL.URLWithString(url) ?: return HttpReply.Failed(null)
    val request = NSMutableURLRequest.requestWithURL(target)
    request.setHTTPMethod("POST")
    request.setHTTPBody((body as NSString).dataUsingEncoding(NSUTF8StringEncoding))
    headers.forEach { (name, value) -> request.setValue(value, forHTTPHeaderField = name) }

    return suspendCancellableCoroutine { continuation ->
        val task = NSURLSession.sharedSession.dataTaskWithRequest(request) { data, response, error ->
            val status = (response as? NSHTTPURLResponse)?.statusCode?.toInt()
            val reply = when {
                error != null && status == null -> HttpReply.Offline
                status in OK_RANGE_START..OK_RANGE_END -> (data as? NSData)?.asText()?.let(HttpReply::Ok) ?: HttpReply.Failed(status)
                else -> HttpReply.Failed(status)
            }
            if (continuation.isActive) continuation.resume(reply)
        }
        continuation.invokeOnCancellation { task.cancel() }
        task.resume()
    }
}

private val queryValueCharacters: NSCharacterSet =
    (NSCharacterSet.URLQueryAllowedCharacterSet.mutableCopy() as NSMutableCharacterSet).apply {
        removeCharactersInString("&=+?#")
    }

fun String.urlEncoded(): String =
    (this as NSString)
        .stringByAddingPercentEncodingWithAllowedCharacters(queryValueCharacters)
        ?: this

private fun NSData.asText(): String? = NSString.create(data = this, encoding = NSUTF8StringEncoding) as String?
