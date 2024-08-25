package com.hoker.intra.di

import com.hoker.intra.domain.AuthApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IntraOkHttp

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IntraAuthRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IntraAuthApiService

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val AUTH_BASE_URL = "https://auth.vivokey.com/"
    private const val API_HEADER = "X-API-VIVOKEY"
    private const val API_KEY = "9e084e64-eb74-41b8-a87d-4c0bdcd1be64"

    @Provides
    @Singleton
    @IntraOkHttp
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val newRequest = originalRequest.newBuilder()
                    .header(API_HEADER, API_KEY)
                    .build()
                chain.proceed(newRequest)
            }
            .build()
    }

    @Provides
    @Singleton
    @IntraAuthRetrofit
    fun provideAuthRetrofit(@IntraOkHttp okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(AUTH_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
    }

    @Provides
    @Singleton
    @IntraAuthApiService
    fun provideAuthApiService(@IntraAuthRetrofit retrofit: Retrofit): AuthApiService {
        return retrofit.create(AuthApiService::class.java)
    }
}