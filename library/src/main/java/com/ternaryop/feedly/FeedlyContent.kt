package com.ternaryop.feedly

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Created by dave on 24/02/17.
 * Contains the item content
 */

interface FeedlyContent {
    val id: String
    val title: String
    val originId: String
    val canonicalUrl: String?
    val actionTimestamp: Long
    val origin: FeedlyOrigin
}

@JsonClass(generateAdapter = true)
data class FeedlyOrigin(val title: String)

@JsonClass(generateAdapter = true)
data class Category(val id: String, val label: String, val description: String? = null)

@JsonClass(generateAdapter = true)
data class SimpleFeedlyContent(
    override val id: String,
    @Json(name = "title") internal val nullableTitle: String?,
    override val originId: String,
    override val canonicalUrl: String?,
    override val actionTimestamp: Long,
    override val origin: FeedlyOrigin,
    val categories: List<Category>?
) : FeedlyContent {
    override val title: String
        get() = nullableTitle ?: "No title"
}

@JsonClass(generateAdapter = true)
data class StreamContent(var id: String, val items: List<SimpleFeedlyContent>)

data class StreamContentFindParam(val count: Int = 0, val newerThan: Long = 0, val continuation: String? = null) {
    fun toQueryMap(): Map<String, String> {
        val map = mutableMapOf<String, String>()

        if (count > 0) {
            map["count"] = count.toString()
        }
        if (newerThan > 0) {
            map["newerThan"] = newerThan.toString()
        }
        continuation?.let { map["continuation"] = it }

        return map
    }
}

@JsonClass(generateAdapter = true)
data class AccessToken(@Json(name = "access_token") val accessToken: String)

@JsonClass(generateAdapter = true)
data class Marker(val type: String, val action: String, val entryIds: List<String>)

@JsonClass(generateAdapter = true)
data class Error(val errorCode: Int, val errorId: String, val errorMessage: String?) {
    fun hasTokenExpired(): Boolean = errorMessage != null && errorMessage.startsWith("token expired")
}
