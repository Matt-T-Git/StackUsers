package com.stackusers.di

import com.stackusers.data.api.StackExchangeApiService
import com.stackusers.data.repository.UserRepositoryImpl
import com.stackusers.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

private const val BASE_URL = "https://api.stackexchange.com/2.3/"
private const val TIMEOUT_SECONDS = 30L

/**
 * Hilt module providing network-related dependencies.
 * Init in SingletonComponent so all instances are application-scoped.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * Provides OkHttpClient with:
     * - HTTP logging (body level) for debugging network traffic
     * - API key interceptor to append our registered API key to every request
     * - 30 second connect/read/write timeouts
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val apiKeyInterceptor = Interceptor { chain ->
            val original = chain.request()
            val url = original.url.newBuilder()
                .addQueryParameter("key", "rl_C1r2HphKLrWRhxSbAWGCYhwTi")
                .build()
            chain.proceed(original.newBuilder().url(url).build())
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(apiKeyInterceptor)
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Provides Retrofit instance configured with StackExchange base URL
     * and Gson for JSON serialization
     */
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Provides the concrete Retrofit implementation of [StackExchangeApiService]
     */
    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): StackExchangeApiService {
        return retrofit.create(StackExchangeApiService::class.java)
    }
}

/**
 * Hilt module binding the repository interface to its implementation.
 * Using @Binds (vs @Provides) is more efficient as it avoids generating
 * an extra factory class.
 *
 * Reviewer note:
 * I am using @suppress here as Android Studio produces a false positive for unused.
 * The methods are used, just not in a way the IDE can trace statically
 * Hilt's code generation happens at compile time via kapt
 * so Android Studio can't see the usage through the generated code
 *
 */
@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Suppress("unused")
    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository
}