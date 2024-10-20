package io.dee.portal.utils

import android.content.res.Resources
import android.graphics.BitmapFactory
import com.carto.styles.AnimationStyle
import com.carto.styles.AnimationStyleBuilder
import com.carto.styles.AnimationType
import com.carto.styles.MarkerStyle
import com.carto.styles.MarkerStyleBuilder
import io.dee.portal.R
import org.neshan.mapsdk.internal.utils.BitmapUtils

object MapShapesUtil {

    private fun getAnimationStyle(): AnimationStyle? {
        val animStBl = AnimationStyleBuilder()
        animStBl.fadeAnimationType = AnimationType.ANIMATION_TYPE_SMOOTHSTEP
        animStBl.sizeAnimationType = AnimationType.ANIMATION_TYPE_SPRING
        animStBl.phaseInDuration = 0.2f
        animStBl.phaseOutDuration = 0.2f
        return animStBl.buildStyle()
    }

    fun buildUserMarkerStyle(resources: Resources): MarkerStyle {
        val style = MarkerStyleBuilder()
        style.size = 30f

        style.bitmap = BitmapUtils.createBitmapFromAndroidBitmap(
            BitmapFactory.decodeResource(
                resources, org.neshan.mapsdk.R.drawable.ic_marker
            )
        )
        return style.buildStyle()
    }

    fun buildNavigationMarkerStyle(resources: Resources): MarkerStyle = MarkerStyleBuilder().apply {

        size = 25f
        isScaleWithDPI = true
        bitmap = BitmapUtils.createBitmapFromAndroidBitmap(
            BitmapFactory.decodeResource(
                resources, R.drawable.ic_navigator
            )
        )
        animationStyle = this@MapShapesUtil.getAnimationStyle()
    }.buildStyle()

    fun buildOriginMarkerStyle(resources: Resources) = MarkerStyleBuilder().apply {
        size = 20f
        bitmap = BitmapUtils.createBitmapFromAndroidBitmap(
            BitmapFactory.decodeResource(
                resources, org.neshan.mapsdk.R.drawable.ic_cluster_marker_blue
            )
        )
        animationStyle = this@MapShapesUtil.getAnimationStyle()

    }.buildStyle()

    fun buildDestinationMarkerStyle(resources: Resources) = MarkerStyleBuilder().apply {

        size = 20f
        bitmap = BitmapUtils.createBitmapFromAndroidBitmap(
            BitmapFactory.decodeResource(
                resources, org.neshan.mapsdk.R.drawable.ic_cluster_marker_blue
            )
        )
        animationStyle = this@MapShapesUtil.getAnimationStyle()


    }.buildStyle()

}