package io.dee.portal.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.dee.portal.api.PortalService
import io.dee.portal.data.dao.LocationDataDao
import io.dee.portal.view.map_screen.data.datasource.ReverseGeoCodingDatasource
import io.dee.portal.view.map_screen.data.datasource.ReverseGeoCodingDatasourceImpl
import io.dee.portal.view.search_screen.data.SearchLocalDatasource
import io.dee.portal.view.search_screen.data.SearchLocalDatasourceImpl
import io.dee.portal.view.search_screen.data.SearchRemoteDatasource
import io.dee.portal.view.search_screen.data.SearchRemoteDatasourceImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class DatasourceModule {
    @Provides
    @Singleton
    fun provideReverseGeoCodingDatasource(
        portalService: PortalService
    ): ReverseGeoCodingDatasource {
        return ReverseGeoCodingDatasourceImpl(portalService)
    }

    @Provides
    @Singleton
    fun provideSearchRemoteDatasource(
        portalService: PortalService
    ): SearchRemoteDatasource {
        return SearchRemoteDatasourceImpl(portalService)
    }

    @Provides
    @Singleton
    fun provideSearchLocalDatasource(
        dao: LocationDataDao
    ): SearchLocalDatasource {
        return SearchLocalDatasourceImpl(dao)
    }


}