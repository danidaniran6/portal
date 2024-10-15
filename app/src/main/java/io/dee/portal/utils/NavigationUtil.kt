package io.dee.portal.utils

import io.dee.portal.map_screen.data.dto.DecodedSteps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.neshan.common.model.LatLng
import kotlin.math.pow
import kotlin.math.sqrt

object NavigationUtil {
    suspend fun snapToLine(location: LatLng, polyline: List<LatLng>): LatLng {
        return withContext(Dispatchers.Default) {
            var closestPoint: LatLng? = null
            var minDistance = Double.MAX_VALUE

            for (i in 0 until polyline.size - 1) {
                val segmentStart = polyline[i]
                val segmentEnd = polyline[i + 1]

                val projectedPoint = getClosestPointOnSegment(location, segmentStart, segmentEnd)
                val distance = distanceBetween(location, projectedPoint)

                if (distance < minDistance) {
                    minDistance = distance
                    closestPoint = projectedPoint
                }
            }

            closestPoint ?: location
        }

    }

    private fun getClosestPointOnSegment(p: LatLng, a: LatLng, b: LatLng): LatLng {
        val apx = p.latitude - a.latitude
        val apy = p.longitude - a.longitude
        val abx = b.latitude - a.latitude
        val aby = b.longitude - a.longitude

        val ab2 = abx * abx + aby * aby
        val ap_ab = apx * abx + apy * aby
        val t = ap_ab / ab2

        val clampT = t.coerceIn(0.0, 1.0)

        return LatLng(
            a.latitude + clampT * abx,
            a.longitude + clampT * aby
        )
    }

    private fun distanceBetween(a: LatLng, b: LatLng): Double {
        val latDiff = a.latitude - b.latitude
        val lngDiff = a.longitude - b.longitude
        return sqrt(latDiff.pow(2) + lngDiff.pow(2))
    }

    suspend fun findCurrentStep(
        currentLocation: LatLng,
        steps: List<DecodedSteps>,
        tolerance: Double = 0.0001
    ): Int {
        return withContext(Dispatchers.Default) {
            var closestStepIndex = -1
            var minDistance = Double.MAX_VALUE

            for (stepIndex in steps.indices) {
                val polyline = steps[stepIndex].decodedPolyline ?: return@withContext -1

                for (i in 0 until polyline.size - 1) {
                    val segmentStart = polyline[i]
                    val segmentEnd = polyline[i + 1]

                    val closestPoint =
                        getClosestPointOnSegment(currentLocation, segmentStart, segmentEnd)
                    val distance = distanceBetween(currentLocation, closestPoint)

                    if (distance < minDistance && distance <= tolerance) {
                        minDistance = distance
                        closestStepIndex = stepIndex
                    }
                }
            }

            closestStepIndex
        }
    }

    fun calculateBearing(loc1: LatLng, loc2: LatLng): Float {
        val startLocation = android.location.Location("startLocation").apply {
            latitude = loc1.latitude
            longitude = loc1.longitude
        }

        val endLocation = android.location.Location("endLocation").apply {
            latitude = loc2.latitude
            longitude = loc2.longitude
        }

        return 360 - startLocation.bearingTo(endLocation)

    }

}