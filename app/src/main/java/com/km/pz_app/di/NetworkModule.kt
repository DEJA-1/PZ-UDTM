package com.km.pz_app.di

import com.km.pz_app.data.dataProvider.RaspberryAddressProvider
import com.km.pz_app.data.dataProvider.SystemStatusApi
import com.km.pz_app.data.dataProvider.remoteTerminal.ITerminalWebSocketApi
import com.km.pz_app.data.dataProvider.remoteTerminal.TerminalWebSocketApi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

    @Provides
    @Singleton
    fun provideOkHttpClient(logging: HttpLoggingInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

    @Provides
    @Singleton
    suspend fun provideRetrofit(
        client: OkHttpClient,
        addressProvider: RaspberryAddressProvider
    ): Retrofit {
        val baseUrl = addressProvider.httpBaseUrl()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
    }

    @Provides
    @Singleton
    fun provideSystemStatusApi(retrofit: Retrofit): SystemStatusApi =
        retrofit.create(SystemStatusApi::class.java)
}
