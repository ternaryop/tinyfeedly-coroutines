package com.ternaryop.feedly

import org.junit.Before
import java.util.*

abstract class AbsFeedlyUnitTest {
    lateinit var properties: Properties
    lateinit var feedlyClient: FeedlyClient

    @Before
    fun before() {
        properties = Properties()
        javaClass.classLoader!!.getResourceAsStream("config.properties")
            .use { properties.load(it) }
        FeedlyClient.setup(
            FeedlyClientInfo(
                properties.getProperty("FEEDLY_USER_ID"),
                properties.getProperty("FEEDLY_REFRESH_TOKEN"),
                properties.getProperty("FEEDLY_CLIENT_ID"),
                properties.getProperty("FEEDLY_CLIENT_SECRET")
            ),
            null
        )

        feedlyClient = FeedlyClient(properties.getProperty("FEEDLY_ACCESS_TOKEN"))
    }
}