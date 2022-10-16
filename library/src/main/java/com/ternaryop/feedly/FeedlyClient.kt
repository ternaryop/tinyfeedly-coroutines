package com.ternaryop.feedly

import com.squareup.moshi.Moshi
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.QueryMap
import java.io.IOException
import java.net.HttpURLConnection

/**
 * Created by dave on 24/02/17.
 * Feedly Manager
 * Migrated to Retrofit API Service on 09/08/2018.
 */

private const val API_PREFIX = "https://cloud.feedly.com/"

interface FeedlyService {
    @GET("v3/streams/contents")
    suspend fun getStreamContents(
        @Query("streamId") streamId: String,
        @QueryMap params: Map<String, String>
    ): StreamContent

    @FormUrlEncoded
    @POST("v3/auth/token")
    suspend fun refreshAccessToken(@FieldMap params: Map<String, String>): AccessToken

    @POST("v3/markers")
    suspend fun markers(@Body marker: Marker)

    @GET("v3/categories")
    suspend fun getCategories(
        @Query("sort") sort: String? = null
    ): List<Category>
}

data class FeedlyClientInfo(
    val userId: String,
    val refreshToken: String,
    val clientId: String,
    val clientSecret: String
)

class FeedlyClient(
    var accessToken: String,
    val feedlyClientInfo: FeedlyClientInfo = globalClientInfo
) {
    val globalSavedTag = "user/${feedlyClientInfo.userId}/tag/global.saved"

    suspend fun getStreamContents(streamId: String, params: Map<String, String>): StreamContent {
        return service()
            .getStreamContents(streamId, params)
    }

    suspend fun mark(ids: List<String>, action: MarkerAction) {
        if (ids.isNotEmpty()) {
            service().markers(Marker("entries", action, ids))
        }
    }

    suspend fun refreshAccessToken(): AccessToken {
        val data = mapOf(
            "refresh_token" to feedlyClientInfo.refreshToken,
            "client_id" to feedlyClientInfo.clientId,
            "client_secret" to feedlyClientInfo.clientSecret,
            "grant_type" to "refresh_token"
        )
        return service(false)
            .refreshAccessToken(data)
    }

    suspend fun getCategories(sort: String? = null): List<Category> = service().getCategories(sort)

    fun service(authorized: Boolean = true): FeedlyService = if (authorized) {
        builder.create(FeedlyService::class.java)
    } else {
        builderNoAuth.create(FeedlyService::class.java)
    }

    val builder: Retrofit by lazy {
        val builder = feedlyHttpBuilder()
            .also { it.interceptors().add(authInterceptor) }

        retrofit(builder)
    }

    val builderNoAuth: Retrofit by lazy {
        retrofit(feedlyHttpBuilder())
    }

    private fun retrofit(okHttpBuilder: OkHttpClient.Builder): Retrofit {
        val moshi = Moshi.Builder()
            .build()

        return Retrofit.Builder()
            .baseUrl(API_PREFIX)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(okHttpBuilder.build())
            .build()
    }

    private fun feedlyHttpBuilder(): OkHttpClient.Builder =
        (okHttpClient?.newBuilder() ?: OkHttpClient.Builder())
            .also {
                it.interceptors().add(errorInterceptor)
                it.interceptors().add(rateInterceptor)
            }

    private val authInterceptor = Interceptor { chain ->
        val newRequest = chain.request().newBuilder()
            .addHeader("Authorization", "OAuth $accessToken").build()
        chain.proceed(newRequest)
    }

    private val rateInterceptor = Interceptor { chain ->
        val request = chain.request()
        val response = chain.proceed(request)
        FeedlyRateLimit.update(response.code, response.headers)
        response
    }

    companion object {
        private var okHttpClient: OkHttpClient? = null
        private lateinit var globalClientInfo: FeedlyClientInfo

        fun setup(feedlyUser: FeedlyClientInfo, okHttpClient: OkHttpClient? = null) {
            this.globalClientInfo = feedlyUser
            this.okHttpClient = okHttpClient
        }

        private val errorInterceptor = Interceptor { chain ->
            val request = chain.request()
            val response = chain.proceed(request)
            val responseCode = response.code

            if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                response.body.source().also { source ->
                    source.request(java.lang.Long.MAX_VALUE) // Buffer the entire body.
                    val json = source.buffer.clone().readUtf8()
                    val error = Moshi.Builder().build().adapter(Error::class.java).fromJson(json)
                    if (error == null) {
                        throw IOException("Unable to parse error $json")
                    } else {
                        if (error.hasTokenExpired()) {
                            throw TokenExpiredException(error.errorMessage!!)
                        }
                        throw IOException("Error $responseCode: ${error.errorMessage}")
                    }
                }
            }
            response
        }
    }
}
