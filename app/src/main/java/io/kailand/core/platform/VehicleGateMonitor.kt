package io.kailand.core.platform

import android.car.Car
import android.car.VehicleGear
import android.car.VehiclePropertyIds
import android.car.drivingstate.CarUxRestrictionsManager
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import io.kailand.core.contract.InteractionGate

/** Created only on Automotive API29+. No writes, restriction overrides or speed-based parking. */
class VehicleGateMonitor(context: Context, onGate: (InteractionGate) -> Unit) {
    private val context = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private val session = VehicleGateSession(onGate)
    private var car: Car? = null
    private var ux: CarUxRestrictionsManager? = null
    private var properties: CarPropertyManager? = null
    private var gearListener: CarPropertyManager.CarPropertyEventCallback? = null
    private var active = false
    private var lifecycleGeneration = 0L

    fun start() {
        stop()
        active = true
        val lifecycleToken = ++lifecycleGeneration
        session.start()
        try {
            car = Car.createCar(context, handler, Car.CAR_WAIT_TIMEOUT_DO_NOT_WAIT) { connectedCar, ready ->
                if (!active || lifecycleToken != lifecycleGeneration) return@createCar
                // A connection transition discards both previously confirmed inputs.
                val token = session.start()
                session.update(token) { VehicleGateInputs(connected = ready, foreground = true) }
                cleanupManagers()
                if (ready) observe(connectedCar, token)
            }
        } catch (_: Exception) {
            cleanupManagers()
            session.start()
        }
    }

    private fun observe(connectedCar: Car, token: Long) {
        try {
            val currentUx = connectedCar.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as CarUxRestrictionsManager
            val currentProperties = connectedCar.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
            ux = currentUx
            properties = currentProperties
            currentUx.registerListener { restrictions ->
                session.update(token) { it.copy(
                    requiresDistractionOptimization = restrictions.isRequiresDistractionOptimization,
                    activeRestrictions = restrictions.activeRestrictions,
                ) }
            }
            val listener = object : CarPropertyManager.CarPropertyEventCallback {
                override fun onChangeEvent(value: CarPropertyValue<*>) {
                    if (value.propertyId == VehiclePropertyIds.GEAR_SELECTION) {
                        session.update(token) { it.copy(parked = parking(value)) }
                    }
                }

                override fun onErrorEvent(propertyId: Int, areaId: Int) {
                    if (propertyId == VehiclePropertyIds.GEAR_SELECTION) {
                        session.update(token) { it.copy(parked = null) }
                    }
                }
            }
            gearListener = listener
            check(currentProperties.registerCallback(listener, VehiclePropertyIds.GEAR_SELECTION, CarPropertyManager.SENSOR_RATE_ONCHANGE))
            val restrictions = currentUx.currentCarUxRestrictions
            session.update(token) { it.copy(
                requiresDistractionOptimization = restrictions?.isRequiresDistractionOptimization,
                activeRestrictions = restrictions?.activeRestrictions,
            ) }
            val gear = currentProperties.getProperty<Int>(VehiclePropertyIds.GEAR_SELECTION, 0)
            session.update(token) { it.copy(parked = parking(gear)) }
        } catch (_: Exception) {
            cleanupManagers()
            session.update(token) { VehicleGateInputs(foreground = true) }
        }
    }

    private fun parking(value: CarPropertyValue<*>?): Boolean? {
        if (value?.status != CarPropertyValue.STATUS_AVAILABLE) return null
        return when (value.value as? Int) {
            VehicleGear.GEAR_PARK -> true
            VehicleGear.GEAR_REVERSE, VehicleGear.GEAR_NEUTRAL, VehicleGear.GEAR_DRIVE,
            VehicleGear.GEAR_FIRST, VehicleGear.GEAR_SECOND, VehicleGear.GEAR_THIRD,
            VehicleGear.GEAR_FOURTH, VehicleGear.GEAR_FIFTH, VehicleGear.GEAR_SIXTH,
            VehicleGear.GEAR_SEVENTH, VehicleGear.GEAR_EIGHTH, VehicleGear.GEAR_NINTH -> false
            else -> null
        }
    }

    fun stop() {
        active = false
        lifecycleGeneration++
        session.stop()
        cleanupManagers()
        val previous = car
        car = null
        runCatching { previous?.disconnect() }
    }

    private fun cleanupManagers() {
        runCatching { ux?.unregisterListener() }
        val listener = gearListener
        runCatching { if (listener != null) properties?.unregisterCallback(listener) }
        ux = null
        properties = null
        gearListener = null
    }
}
