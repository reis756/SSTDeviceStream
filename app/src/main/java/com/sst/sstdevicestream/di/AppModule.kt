package com.sst.sstdevicestream.di

import com.sst.sstdevicestream.data.APIClient
import com.sst.sstdevicestream.data.DeviceDataRepository
import com.sst.sstdevicestream.data.DeviceDataRepositoryImpl
import com.sst.sstdevicestream.data.DeviceStreamAPI
import com.sst.sstdevicestream.ui.framebyframe.CameraViewModel
import com.sst.sstdevicestream.ui.home.HomeViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    //API
    single<DeviceStreamAPI> {
        APIClient().provideDeviceStreamEndpoints()
    }

    //ViewModel
    viewModel { HomeViewModel(get()) }
    viewModel { CameraViewModel() }

    //Repository
    single<DeviceDataRepository> { DeviceDataRepositoryImpl(get()) }
}