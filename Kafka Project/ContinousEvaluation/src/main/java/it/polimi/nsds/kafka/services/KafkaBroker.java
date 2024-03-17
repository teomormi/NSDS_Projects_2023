package it.polimi.nsds.kafka.services;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import java.util.*;

public class KafkaBroker {

	public static String brokerAddress = "localhost:9092";
	private static final String offsetResetStrategy = "earliest";
	private static final boolean autoCommit = false;
	private static final String transactionalId = "myTransactionalId";

	/****  Kafka Properties  ****/
	public static Properties getConsumerProperties(String group) {
		final Properties props = new Properties();
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaBroker.brokerAddress);
		props.put(ConsumerConfig.GROUP_ID_CONFIG, group);
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, offsetResetStrategy);
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, String.valueOf(autoCommit));
		props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
		return props;
	}

	public static Properties getProducerProperties() {
		final Properties props = new Properties();
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaBroker.brokerAddress);
		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		// Idempotence = exactly once semantics between the producer and the partition
		props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, String.valueOf(true));
		return props;
	}

	// used by services that post dependent messages on different topics
	public static KafkaProducer<String,String> getProducerTransactional() {
		final Properties props = getProducerProperties();
		props.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, UUID.randomUUID().toString());
		KafkaProducer<String, String> producer = new KafkaProducer<>(props);
		producer.initTransactions();
		return producer;
	}

}