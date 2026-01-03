package com.vivokey.intra.di

import com.vivokey.intra.domain.AuthApiService
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

    private const val API_BASE_URL = "https://api.vivokey.com/"

    @Provides
    @Singleton
    @IntraOkHttp
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .build()
    }

    @Provides
    @Singleton
    @IntraAuthRetrofit
    fun provideAuthRetrofit(@IntraOkHttp okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(API_BASE_URL)
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
