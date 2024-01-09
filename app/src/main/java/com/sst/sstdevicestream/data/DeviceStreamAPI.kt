package com.sst.sstdevicestream.data

import com.sst.sstdevicestream.model.DeviceData
import com.sst.sstdevicestream.model.DeviceDataResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface DeviceStreamAPI {
    @POST("/device_data")
    suspend fun setDeviceData(
        @Body deviceData: DeviceData
    ): DeviceDataResponse
}