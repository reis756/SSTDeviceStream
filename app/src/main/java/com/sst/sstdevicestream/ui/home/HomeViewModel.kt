package com.sst.sstdevicestream.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sst.sstdevicestream.data.DeviceDataRepository
import com.sst.sstdevicestream.model.DeviceData
import com.sst.sstdevicestream.model.DeviceDataResponse
import kotlinx.coroutines.launch

class HomeViewModel(
    private val deviceDataRepository: DeviceDataRepository
) : ViewModel() {

    private val _deviceData = MutableLiveData<DeviceDataResponse>()
    val deviceData: LiveData<DeviceDataResponse> = _deviceData

    fun setDeviceData(deviceData: DeviceData) {
        viewModelScope.launch {
            _deviceData.postValue(deviceDataRepository.setDeviceData(deviceData))
        }
    }
}