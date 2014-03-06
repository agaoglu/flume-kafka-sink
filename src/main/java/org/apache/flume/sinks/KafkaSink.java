package org.apache.flume.sinks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;

import org.apache.flume.Channel;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.EventDeliveryException;
import org.apache.flume.Transaction;
import org.apache.flume.conf.Configurable;
import org.apache.flume.sink.AbstractSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KafkaSink extends AbstractSink implements Configurable {

	private final static Logger logger = LoggerFactory.getLogger(KafkaSink.class);
	private final Properties kafkaProps = new Properties();
	private Producer<String, String> producer;
	private String topic;
	private Integer batchSize;
	private List<KeyedMessage<String, String>> batch;

	public void configure(Context context) {
		topic = context.getString("topic");
		batchSize = context.getInteger("batchSize", 100);
		batch = new ArrayList<KeyedMessage<String,String>>(batchSize);
		for (Entry<String, String> e : context.getParameters().entrySet())
			kafkaProps.put(e.getKey(), e.getValue());
	}

	@Override
	public synchronized void start() {
		producer = new Producer<String, String>(new ProducerConfig(kafkaProps));
		super.start();
	}

	@Override
	public synchronized void stop() {
		producer.close();
		super.stop();
	}

	public Status process() throws EventDeliveryException {
		Status status = null;

		// Start transaction
		Channel ch = getChannel();
		Transaction txn = ch.getTransaction();
		txn.begin();
		try {
			// This try clause includes whatever Channel operations you want to
			// do
			batch.clear();
			
			for (int i = 0; i < batchSize; i++) {
				Event event = ch.take();
				if (event == null) break;

				// Send the Event to the external repository.
				batch.add(new KeyedMessage<String, String>(topic, new String(event.getBody())));
			}
			
			if (!batch.isEmpty())
				producer.send(batch);

			txn.commit();
			status = Status.READY;
		} catch (Throwable t) {
			txn.rollback();

			// Log exception, handle individual exceptions as needed
			logger.warn("Unknown exception, txn rolled-back", t);

			status = Status.BACKOFF;

			// re-throw all Errors
			if (t instanceof Error) {
				throw (Error) t;
			}
		} finally {
			txn.close();
		}
		return status;
	}

}
