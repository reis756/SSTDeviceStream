package com.sst.sstdevicestream.ui.framebyframe

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sst.sstdevicestream.api.KafkaProducerHelper
import com.sst.sstdevicestream.api.RabbitMq
import com.sst.sstdevicestream.model.DeviceData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CameraViewModel : ViewModel() {

    private val isStreaming: MutableLiveData<Boolean> = MutableLiveData(false)

    private val _info: MutableLiveData<String> = MutableLiveData()
    val info: LiveData<String> = _info

    private val _error: MutableLiveData<String> = MutableLiveData()
    val error: LiveData<String> = _error

    private var startTimeFrame = 0L
    private var counter = 0
    private var kbps = 0.0

    val rabbitMq = RabbitMq()

    var rabbitMqQueueStream = ""
    var userUuid = ""
    lateinit var deviceData: DeviceData

    fun setUserId(userId: String) {
        this.userUuid = userId
        rabbitMqQueueStream = "stream_android_${userId}"
    }

    fun setupRabbitMq() {
        rabbitMq.setupConnectionFactory(
            RABBITMQ_USERNAME,
            RABBITMQ_PASSWORD,
            RABBITMQ_VIRTUAL_HOST,
            RABBITMQ_HOST,
            RABBITMQ_PORT
        )
        viewModelScope.launch(Dispatchers.IO) {
            try {
                rabbitMq.prepareConnection(
                    listOf(
                        rabbitMqQueueStream
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = e.message
            }
        }
    }

    fun publishMessage(queue: String, message: ByteArray, deviceLocation: DeviceData) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (isStreaming.value == true) {
                    rabbitMq.publishMessage(queue, message, deviceLocation)
                    getFps(message)
                } else {
                    kbps = 0.0
                    counter = 0
                }
            } catch (e: InterruptedException) {
                e.printStackTrace()
                _error.value = e.message
            }
        }
    }

    fun producerKafka() {
        val kafkaHelper = KafkaProducerHelper("44.195.107.125:9092")
        val topic = "seu_topico"
        val mensagem = "Sua mensagem aqui"
        kafkaHelper.produceMessage(topic, mensagem)
    }

    fun activeStreaming(streaming: Boolean) {
        isStreaming.value = streaming
    }

    private fun getFps(biteArray: ByteArray) {
        if (startTimeFrame == 0L) {
            startTimeFrame = System.currentTimeMillis()
            counter++
        } else {
            val difference: Long = System.currentTimeMillis() - startTimeFrame

            val seconds = difference / 1000.0

            if (seconds >= 1) {
                _info.postValue("$counter fps\n${String.format("%.2f", kbps)} kbps")
                counter = 0
                kbps = 0.0
                startTimeFrame = System.currentTimeMillis()
            } else {
                counter++
                kbps =+ getByteArraySize(biteArray)
            }
        }
        Log.i(TAG, "${_info.value}fps")
    }

    private fun getByteArraySize(biteArray: ByteArray): Double {
        val sizeByteArrayKB = biteArray.size / 1024.0

        Log.i(TAG, "Frame size ${String.format("%.2f", sizeByteArrayKB)} KB")

        return sizeByteArrayKB
    }

    companion object {
        const val RABBITMQ_USERNAME = "sst"
        const val RABBITMQ_PASSWORD = "12345"
        const val RABBITMQ_VIRTUAL_HOST = "/"
        const val RABBITMQ_HOST = "44.195.107.125"
        const val RABBITMQ_PORT = 5672

        const val TAG = "FrameByFrame"
    }
}