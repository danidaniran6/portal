package io.dee.portal.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.dee.portal.core.data.api.PortalService
import io.dee.portal.core.data.db.dao.LocationDataDao
import io.dee.portal.map_screen.data.datasource.LocationProviderDatasource
import io.dee.portal.map_screen.data.datasource.LocationProviderDatasourceImpl
import io.dee.portal.map_screen.data.datasource.ReverseGeoCodingDatasource
import io.dee.portal.map_screen.data.datasource.ReverseGeoCodingDatasourceImpl
import io.dee.portal.map_screen.data.datasource.RoutingRemoteDatasource
import io.dee.portal.map_screen.data.datasource.RoutingRemoteDatasourceImpl
import io.dee.portal.search_screen.data.datasource.SearchLocalDatasource
import io.dee.portal.search_screen.data.datasource.SearchLocalDatasourceImpl
import io.dee.portal.search_screen.data.datasource.SearchRemoteDatasource
import io.dee.portal.search_screen.data.datasource.SearchRemoteDatasourceImpl
import io.dee.portal.utils.LocationProvider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatasourceModule {
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

    @Provides
    @Singleton
    fun providesRoutingRemoteDatasource(
        portalService: PortalService
    ): RoutingRemoteDatasource =
        RoutingRemoteDatasourceImpl(portalService)

    @Provides
    @Singleton
    fun provideLocationProviderDatasource(
        locationProvider: LocationProvider
    ): LocationProviderDatasource =
        LocationProviderDatasourceImpl(locationProvider)


}