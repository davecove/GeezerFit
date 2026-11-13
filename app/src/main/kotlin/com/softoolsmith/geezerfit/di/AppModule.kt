package com.softoolsmith.geezerfit.di

import android.content.Context
import androidx.room.Room
import com.softoolsmith.geezerfit.data.db.AppDatabase
import com.softoolsmith.geezerfit.data.repository.PlanRepository
import com.softoolsmith.geezerfit.data.repository.remote.RemotePlanRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun db(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "geezerfit.db").build()

    @Provides fun planCacheDao(db: AppDatabase) = db.planCacheDao()
    @Provides fun sessionDao(db: AppDatabase) = db.sessionDao()
    @Provides fun draftDao(db: AppDatabase) = db.draftDao()

    @Provides @Singleton
    fun json(): Json = Json { ignoreUnknownKeys = true }

    @Provides @Singleton
    fun okHttp(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {
    @Binds abstract fun planRepository(impl: RemotePlanRepository): PlanRepository
}
