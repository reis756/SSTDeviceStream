package com.sst.sstdevicestream.api

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import java.net.InetAddress
import java.util.Properties

class KafkaProducerHelper(private val bootstrapServers: String) {

    fun produceMessage(topic: String, message: String) {
        val props = Properties()
        props[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
        props[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = "org.apache.kafka.common.serialization.StringSerializer"
        props[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = "org.apache.kafka.common.serialization.StringSerializer"

        val producer = KafkaProducer<String, String>(props)

        val record = ProducerRecord(topic, "", message)
        producer.send(record)
        producer.close()
    }
}