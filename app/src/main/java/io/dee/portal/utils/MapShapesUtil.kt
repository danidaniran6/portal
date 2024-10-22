package io.dee.portal.utils

import android.content.Context
import android.content.res.Resources
import android.graphics.BitmapFactory
import com.carto.graphics.Color
import com.carto.styles.AnimationStyle
import com.carto.styles.AnimationStyleBuilder
import com.carto.styles.AnimationType
import com.carto.styles.LineEndType
import com.carto.styles.LineJoinType
import com.carto.styles.LineStyleBuilder
import com.carto.styles.MarkerStyle
import com.carto.styles.MarkerStyleBuilder
import dagger.hilt.android.qualifiers.ApplicationContext
import io.dee.portal.R
import io.dee.portal.map_screen.view.MapEvents
import org.neshan.common.model.LatLng
import org.neshan.mapsdk.internal.utils.BitmapUtils
import org.neshan.mapsdk.model.Marker
import org.neshan.mapsdk.model.Polyline
import javax.inject.Inject


class MapShapesUtil @Inject constructor(@ApplicationContext context: Context) {
    private val resources: Resources = context.resources

    private fun getAnimationStyle(): AnimationStyle? {
        val animStBl = AnimationStyleBuilder()
        animStBl.fadeAnimationType = AnimationType.ANIMATION_TYPE_SMOOTHSTEP
        animStBl.sizeAnimationType = AnimationType.ANIMATION_TYPE_SPRING
        animStBl.phaseInDuration = 0.2f
        animStBl.phaseOutDuration = 0.2f
        return animStBl.buildStyle()
    }

    private fun buildUserMarkerStyle(): MarkerStyle {
        val style = MarkerStyleBuilder()
        style.size = 30f

        style.bitmap = BitmapUtils.createBitmapFromAndroidBitmap(
            BitmapFactory.decodeResource(
                resources, org.neshan.mapsdk.R.drawable.ic_marker
            )
        )
        return style.buildStyle()
    }

    private fun buildNavigationMarkerStyle(): MarkerStyle = MarkerStyleBuilder().apply {

        size = 25f
        isScaleWithDPI = true
        bitmap = BitmapUtils.createBitmapFromAndroidBitmap(
            BitmapFactory.decodeResource(
                resources, R.drawable.ic_navigator
            )
        )
        animationStyle = this@MapShapesUtil.getAnimationStyle()
    }.buildStyle()

    private fun buildOriginMarkerStyle(): MarkerStyle = MarkerStyleBuilder().apply {
        size = 20f
        bitmap = BitmapUtils.createBitmapFromAndroidBitmap(
            BitmapFactory.decodeResource(
                resources, org.neshan.mapsdk.R.drawable.ic_cluster_marker_blue
            )
        )
        animationStyle = this@MapShapesUtil.getAnimationStyle()

    }.buildStyle()

    private fun buildDestinationMarkerStyle(): MarkerStyle =
        MarkerStyleBuilder().apply {

            size = 20f
            bitmap = BitmapUtils.createBitmapFromAndroidBitmap(
                BitmapFactory.decodeResource(
                    resources, org.neshan.mapsdk.R.drawable.ic_cluster_marker_blue
                )
            )
            animationStyle = this@MapShapesUtil.getAnimationStyle()


        }.buildStyle()

    fun provideUserMarker(location: LatLng): Marker =
        Marker(location, buildUserMarkerStyle()).apply {
            title = "user"
        }

    fun provideOriginMarker(location: LatLng): Marker =
        Marker(location, buildOriginMarkerStyle()).apply {
            title = "origin"
        }

    fun provideDestinationMarker(location: LatLng) :Marker=
        Marker(location, buildDestinationMarkerStyle()).apply {
            title = "destination"
        }

    fun provideNavigationMarker(location: LatLng): Marker =
        Marker(location, buildNavigationMarkerStyle()).apply {
            title = "navigator"
        }


    fun provideStepsPolyline(locations: List<LatLng>) = Polyline(
        locations as ArrayList<LatLng>, getStepsLineStyle()
    )

    private fun getStepsLineStyle() = LineStyleBuilder().apply {
        color = Color(2, 119, 189, 190)
        width = 20f
        stretchFactor = 10f
        lineEndType = LineEndType.LINE_END_TYPE_ROUND
        lineJoinType = LineJoinType.LINE_JOIN_TYPE_ROUND
    }.buildStyle()

    fun provideOriginToDestinationLine(locations: List<LatLng>) =
        Polyline(locations as java.util.ArrayList<LatLng>?, getLineStyle())

    private fun getLineStyle() = LineStyleBuilder().apply {
        color = Color(2, 119, 189, 190)
        width = 6f
        stretchFactor = 10f
        lineEndType = LineEndType.LINE_END_TYPE_ROUND
        lineJoinType = LineJoinType.LINE_JOIN_TYPE_ROUND

    }.buildStyle()

}