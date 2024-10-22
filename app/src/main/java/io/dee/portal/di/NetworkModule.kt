package io.dee.portal.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.dee.portal.core.data.api.PortalService
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun providePortalService(
        @ApplicationContext context: Context
    ): PortalService {
        return PortalService.create(context = context)

    }
}