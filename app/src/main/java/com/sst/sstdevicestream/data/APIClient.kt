package com.sst.sstdevicestream.data

class APIClient {
    fun provideDeviceStreamEndpoints(): DeviceStreamAPI = RetrofitService.getInstance(
        baseUrl = "https://44.195.107.125:5000",
        interceptors = listOf()
    )
}