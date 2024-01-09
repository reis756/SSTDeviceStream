package com.sst.sstdevicestream.data

import com.sst.sstdevicestream.model.DeviceData
import com.sst.sstdevicestream.model.DeviceDataResponse


interface DeviceDataRepository {
    suspend fun setDeviceData(deviceData: DeviceData): DeviceDataResponse
}
class DeviceDataRepositoryImpl(
    private val retrofit: DeviceStreamAPI
): DeviceDataRepository {
    override suspend fun setDeviceData(deviceData: DeviceData): DeviceDataResponse =
        retrofit.setDeviceData(deviceData)
}