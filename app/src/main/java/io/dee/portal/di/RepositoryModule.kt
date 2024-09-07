package io.dee.portal.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.dee.portal.view.map_screen.data.datasource.ReverseGeoCodingDatasource
import io.dee.portal.view.map_screen.data.repository.MapRepository
import io.dee.portal.view.map_screen.data.repository.MapRepositoryImpl
import io.dee.portal.view.search_screen.data.SearchLocalDatasource
import io.dee.portal.view.search_screen.data.SearchRemoteDatasource
import io.dee.portal.view.search_screen.data.SearchRepository
import io.dee.portal.view.search_screen.data.SearchRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    fun provideMapRepository(
        datasource: ReverseGeoCodingDatasource
    ): MapRepository {
        return MapRepositoryImpl(datasource)
    }

    @Provides
    @Singleton
    fun provideSearchRepository(
        remoteDatasource: SearchRemoteDatasource,
        localDatasource: SearchLocalDatasource
    ): SearchRepository {
        return SearchRepositoryImpl(remoteDatasource, localDatasource)
    }

}