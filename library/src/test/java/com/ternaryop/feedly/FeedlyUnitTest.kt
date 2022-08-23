package com.ternaryop.feedly

import kotlinx.coroutines.runBlocking
import org.junit.Test

private const val ONE_HOUR_MILLIS = 60 * 60 * 1000

class FeedlyUnitTest : AbsFeedlyUnitTest() {

    @Test
    fun streamTest() {
        val ms = System.currentTimeMillis() - 5 * ONE_HOUR_MILLIS
        val params = StreamContentFindParam(30, ms)
        runBlocking {
            val streamContents =
                feedlyClient.getStreamContents(feedlyClient.globalSavedTag, params.toQueryMap())

            streamContents.items.forEach { content ->
                println("${content.title} -- ${content.nullableTitle} ===> ${content.origin}")
            }
        }
    }

    @Test
    fun refreshAccessTokenTest() {
        runBlocking {
            val accessToken = feedlyClient.refreshAccessToken()
            println("new access token")
            println(accessToken.accessToken)
        }
    }

    @Test
    fun unreadOnlyTest() = runBlocking {
        val params = StreamContentFindParam(10, unreadOnly = true)
            .toQueryMap()
        for (cat in properties.getProperty("unreadCategories").split("\n")) {
            println("\nCategory $cat\n")
            val streamContents =
                feedlyClient.getStreamContents(
                    cat,
                    params
                )

            streamContents.items.forEachIndexed { index, content ->
                println("$index => ${content.title} -- ${content.nullableTitle} ===> ${content.origin}")
            }
        }
    }
}
