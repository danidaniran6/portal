package io.dee.portal.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.dee.portal.api.PortalService
import io.dee.portal.data.db.dao.LocationDataDao
import io.dee.portal.view.map_screen.data.datasource.ReverseGeoCodingDatasource
import io.dee.portal.view.map_screen.data.datasource.ReverseGeoCodingDatasourceImpl
import io.dee.portal.view.map_screen.data.datasource.RoutingRemoteDatasource
import io.dee.portal.view.map_screen.data.datasource.RoutingRemoteDatasourceImpl
import io.dee.portal.view.search_driver.data.SearchDriverRemoteDatasource
import io.dee.portal.view.search_driver.data.SearchDriverRemoteDatasourceImpl
import io.dee.portal.view.search_screen.data.SearchLocalDatasource
import io.dee.portal.view.search_screen.data.SearchLocalDatasourceImpl
import io.dee.portal.view.search_screen.data.SearchRemoteDatasource
import io.dee.portal.view.search_screen.data.SearchRemoteDatasourceImpl
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
    fun providesSearchDriverRemoteDatasource(): SearchDriverRemoteDatasource =
        SearchDriverRemoteDatasourceImpl()

    @Provides
    @Singleton
    fun providesRoutingRemoteDatasource(
        portalService: PortalService
    ): RoutingRemoteDatasource =
        RoutingRemoteDatasourceImpl(portalService)


}