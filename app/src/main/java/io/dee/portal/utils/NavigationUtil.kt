package io.dee.portal.utils

import io.dee.portal.map_screen.data.dto.DecodedSteps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.neshan.common.model.LatLng

object NavigationUtil {
    suspend fun snapToLine(location: LatLng, polyline: List<LatLng>): LatLng {
        return withContext(Dispatchers.Default) {
            if (polyline.isEmpty()) return@withContext location
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
            closestPoint  ?: location
        }

    }


    suspend fun findCurrentStep(
        currentLocation: LatLng,
        steps: List<DecodedSteps>,
        tolerance: Double = 10.0,
    ): CurrentStepState {
        return withContext(Dispatchers.Default) {
            var closestStepIndex = -1
            for (stepIndex in steps.indices) {
                val polyline = steps[stepIndex].decodedPolyline ?: emptyList()

                for (i in 0 until polyline.size - 1) {
                    val segmentStart = polyline[i]
                    val segmentEnd = polyline[i + 1]
                    val closestPoint =
                        getClosestPointOnSegment(currentLocation, segmentStart, segmentEnd)
                    val distance = distanceBetween(currentLocation, closestPoint)

                    if (distance <= tolerance) {
                        closestStepIndex = stepIndex
                        break
                    }
                }
            }
            val stepsFinished = isRouteFinished(currentLocation, steps)
            if (stepsFinished) {
                CurrentStepState.RouteFinished
            } else {
                CurrentStepState.StepFounded(closestStepIndex)
            }
        }
    }

    private fun isRouteFinished(
        currentLocation: LatLng, steps: List<DecodedSteps>
    ): Boolean {
        if (steps.isNotEmpty()) {
            val lastStep = steps.last()
            val lastPoint = lastStep.decodedPolyline?.lastOrNull() ?: return false
            val lastLocation = android.location.Location("").apply {
                latitude = lastPoint.latitude
                longitude = lastPoint.longitude
            }
            val current = android.location.Location("").apply {
                latitude = currentLocation.latitude
                longitude = currentLocation.longitude
            }

            val distanceToLastPoint = lastLocation.distanceTo(current)
            if (distanceToLastPoint <= 10) {
                return true
            }
        }
        return false
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
            a.latitude + clampT * abx, a.longitude + clampT * aby
        )
    }

    private fun distanceBetween(a: LatLng, b: LatLng): Double {
        val startLocation = android.location.Location("startLocation").apply {
            latitude = a.latitude
            longitude = a.longitude
        }

        val endLocation = android.location.Location("endLocation").apply {
            latitude = b.latitude
            longitude = b.longitude
        }
        return endLocation.distanceTo(startLocation).toDouble()
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

    sealed interface CurrentStepState {
        data class StepFounded(val stepIndex: Int) : CurrentStepState
        object RouteFinished : CurrentStepState
        object NeedReroute : CurrentStepState
        object NoStepFound : CurrentStepState


    }

}