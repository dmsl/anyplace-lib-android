package cy.ac.ucy.cs.anyplace.lib.android.sensors.imu.sensorsCollector2.step

import cy.ac.ucy.cs.anyplace.lib.android.sensors.imu.DataEvent


interface IStepDetector {
    fun updateWithDataEvent(event : DataEvent): DataEvent?
    fun isStepDetected() : Boolean
    fun lastDetectedTimestamp() : Long
}