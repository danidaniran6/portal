package io.dee.portal.data.local

import android.location.Location
import io.dee.portal.search_screen.data.dto.SearchDto
import org.neshan.common.model.LatLng

data class Location(
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var address: String = "",
    var title: String = "",
    var from: Type = Type.None
) {
    constructor(location: Location) : this() {
        this.latitude = location.latitude
        this.longitude = location.longitude
        this.title = "موقغیت کنونی"
        this.address = "موقغیت کنونی"
    }

    constructor(data: SearchDto.Item?) : this() {
        data?.let {
            this.latitude = data.location?.x ?: 0.0
            this.longitude = data.location?.y ?: 0.0
            this.address = data.address ?: ""
            this.title = data.title ?: ""
            this.from = Type.Search
        }

    }

    enum class Type {
        Search, Local, None
    }

    fun getLatLng(): LatLng {
        return LatLng(latitude, longitude)
    }

    fun getAddressOrLatLngString() =
        if (this.address.isNotEmpty()) this.address else getLatLngString()

    fun getLatLngString() = "${this.latitude},${this.longitude}"
}

