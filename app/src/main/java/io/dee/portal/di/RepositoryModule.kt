package io.dee.portal.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.dee.portal.map_screen.data.datasource.ReverseGeoCodingDatasource
import io.dee.portal.map_screen.data.datasource.RoutingRemoteDatasource
import io.dee.portal.map_screen.data.repository.MapRepository
import io.dee.portal.map_screen.data.repository.MapRepositoryImpl
import io.dee.portal.search_driver.data.datasource.SearchDriverRemoteDatasource
import io.dee.portal.search_driver.data.repository.SearchDriverRepository
import io.dee.portal.search_driver.data.repository.SearchDriverRepositoryImpl
import io.dee.portal.search_screen.data.datasource.SearchLocalDatasource
import io.dee.portal.search_screen.data.datasource.SearchRemoteDatasource
import io.dee.portal.search_screen.data.repository.SearchRepository
import io.dee.portal.search_screen.data.repository.SearchRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    fun provideMapRepository(
        datasource: ReverseGeoCodingDatasource,
        routingRemoteDatasource: RoutingRemoteDatasource
    ): MapRepository {
        return MapRepositoryImpl(datasource, routingRemoteDatasource)
    }

    @Provides
    @Singleton
    fun provideSearchRepository(
        remoteDatasource: SearchRemoteDatasource,
        localDatasource: SearchLocalDatasource
    ): SearchRepository {
        return SearchRepositoryImpl(remoteDatasource, localDatasource)
    }

    @Provides
    @Singleton
    fun provideSearchDriverRepository(
        remoteDatasource: SearchDriverRemoteDatasource
    ): SearchDriverRepository {
        return SearchDriverRepositoryImpl(remoteDatasource)
    }

}