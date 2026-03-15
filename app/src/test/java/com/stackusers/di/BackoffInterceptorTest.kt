package com.stackusers.di

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

class BackoffInterceptorTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var client: OkHttpClient
    private lateinit var request: Request

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        client = OkHttpClient.Builder()
            .addInterceptor(BackoffInterceptor())
            .build()

        request = Request.Builder()
            .url(mockWebServer.url("/test"))
            .build()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    // --- Happy path ---

    @Test
    fun returns200immediatelyWhenNoRateLimitHit() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val response = client.newCall(request).execute()

        assertThat(response.code).isEqualTo(200)
        assertThat(mockWebServer.requestCount).isEqualTo(1)
    }

    // --- 429 retry behaviour ---

    @Test
    fun retriesOnceAfterASingle429() {
        mockWebServer.enqueue(MockResponse().setResponseCode(429))
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val response = client.newCall(request).execute()

        assertThat(response.code).isEqualTo(200)
        assertThat(mockWebServer.requestCount).isEqualTo(2)
    }

    @Test
    fun retriesMultipleTimesBeforeEventualSuccess() {
        repeat(3) {
            mockWebServer.enqueue(MockResponse().setResponseCode(429))
        }
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val response = client.newCall(request).execute()

        assertThat(response.code).isEqualTo(200)
        assertThat(mockWebServer.requestCount).isEqualTo(4)
    }

    @Test
    fun returnsFinal429WhenMaxRetriesExhausted() {
        repeat(10) {
            mockWebServer.enqueue(MockResponse().setResponseCode(429))
        }

        val response = client.newCall(request).execute()

        assertThat(response.code).isEqualTo(429)
        assertThat(mockWebServer.requestCount).isEqualTo(BackoffInterceptor.MAX_RETRIES + 1)
    }

    // --- Non-429 errors are not retried ---

    @Test
    fun doesNotRetryOn500ServerError() {
        mockWebServer.enqueue(MockResponse().setResponseCode(500))

        val response = client.newCall(request).execute()

        assertThat(response.code).isEqualTo(500)
        assertThat(mockWebServer.requestCount).isEqualTo(1)
    }

    @Test
    fun doesNotRetryOn404NotFound() {
        mockWebServer.enqueue(MockResponse().setResponseCode(404))

        val response = client.newCall(request).execute()

        assertThat(response.code).isEqualTo(404)
        assertThat(mockWebServer.requestCount).isEqualTo(1)
    }

    @Test
    fun doesNotRetryOn401Unauthorized() {
        mockWebServer.enqueue(MockResponse().setResponseCode(401))

        val response = client.newCall(request).execute()

        assertThat(response.code).isEqualTo(401)
        assertThat(mockWebServer.requestCount).isEqualTo(1)
    }

    // --- Retry-After header ---

    @Test
    fun respectsRetryAfterHeaderWhenPresent() {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(429)
                .addHeader("Retry-After", "1")
        )
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val start = System.currentTimeMillis()
        val response = client.newCall(request).execute()
        val elapsed = System.currentTimeMillis() - start

        assertThat(response.code).isEqualTo(200)
        assertThat(elapsed).isGreaterThanOrEqualTo(1000L)
    }

    @Test
    fun fallsBackToDefaultDelayWhenRetryAfterHeaderIsAbsent() {
        mockWebServer.enqueue(MockResponse().setResponseCode(429))
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val start = System.currentTimeMillis()
        val response = client.newCall(request).execute()
        val elapsed = System.currentTimeMillis() - start

        assertThat(response.code).isEqualTo(200)
        assertThat(elapsed).isGreaterThanOrEqualTo(BackoffInterceptor.DEFAULT_DELAY_MS)
    }

    // --- StackExchange backoff field in response body ---

    @Test
    fun waitsWhenBackoffFieldIsPresentInResponseBody() {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"backoff":1,"items":[],"has_more":false}""")
        )
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"items":[],"has_more":false}""")
        )

        val start = System.currentTimeMillis()
        client.newCall(request).execute()
        client.newCall(request).execute()
        val elapsed = System.currentTimeMillis() - start

        assertThat(elapsed).isGreaterThanOrEqualTo(1000L)
    }

    @Test
    fun doesNotDelayWhenNoBackoffFieldPresent() {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"items":[],"has_more":false}""")
        )
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"items":[],"has_more":false}""")
        )

        val start = System.currentTimeMillis()
        client.newCall(request).execute()
        client.newCall(request).execute()
        val elapsed = System.currentTimeMillis() - start

        assertThat(elapsed).isLessThan(500L)
    }

    @Test
    fun correctlyParsesBackoffValueFromResponseBody() {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"backoff":2,"items":[],"has_more":false}""")
        )
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"items":[],"has_more":false}""")
        )

        client.newCall(request).execute()

        val start = System.currentTimeMillis()
        client.newCall(request).execute()
        val elapsed = System.currentTimeMillis() - start

        assertThat(elapsed).isGreaterThanOrEqualTo(2000L)
    }
}