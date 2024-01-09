package com.sst.sstdevicestream.api

import android.util.Log
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.Return
import com.rabbitmq.client.ReturnCallback
import com.sst.sstdevicestream.model.DeviceData
import java.net.URISyntaxException
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.util.concurrent.BlockingDeque
import java.util.concurrent.LinkedBlockingDeque


class RabbitMq {

    private var factory = ConnectionFactory()
    private var channel: Channel? = null
    private val queue: BlockingDeque<String> = LinkedBlockingDeque()

    fun setupConnectionFactory(
        userName: String,
        password: String,
        virtualHost: String,
        host: String,
        port: Int
    ) {
        try {
            factory.username = userName
            factory.password = password
            factory.virtualHost = virtualHost
            factory.host = host
            factory.port = port
            factory.isAutomaticRecoveryEnabled = false
        } catch (e1: KeyManagementException) {
            e1.printStackTrace()
        } catch (e1: NoSuchAlgorithmException) {
            e1.printStackTrace()
        } catch (e1: URISyntaxException) {
            e1.printStackTrace()
        }
    }

    fun prepareConnection(queueName: List<String>) {
        while (true) {
            try {
                val connection: Connection = factory.newConnection()
                channel = connection.createChannel()
                queueName.forEach {
                    channel?.queueDeclare(it, false, false, true, null)
                }
                while (true) {
                    val message = queue.takeFirst()
                    try {
                        queueName.forEach {
                            publishMessage(it, message.toByteArray(), null)
                        }
                    } catch (e: Exception) {
                        Log.d("RabbitMQ", "[f] $message")
                        throw e
                    }
                }
            } catch (e: InterruptedException) {
                break
            } catch (e: Exception) {
                Log.d("RabbitMQ", "Connection broken: " + e.javaClass.name)
                try {
                    Thread.sleep(5000)
                } catch (e1: InterruptedException) {
                    break
                }
            }
        }
    }

    fun publishMessage(queueName: String, message: ByteArray, deviceData: DeviceData?) {
        try {
            val headers: MutableMap<String, Any> = HashMap()

            deviceData?.let {
                headers["userId"] = it.deviceId
                headers["device"] = it.device
                headers["latitude"] = it.latitude.toString()
                headers["longitude"] = it.longitude.toString()
            }

            channel?.basicPublish(
                "",
                queueName,
                AMQP.BasicProperties.Builder()
                    .headers(headers)
                    .build(),
                message
            )
        } catch (e: InterruptedException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}