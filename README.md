## Flume-ng-kafka-sink

Kafka 0.8 sink for flume 1.4.0.

### Install

    mvn assembly:assembly
    cp target/flume-ng-kafka-sink-0.0.1-SNAPSHOT-jar-with-dependencies.jar /path/to/flume/libs

### Configure

This example reads from syslog udp 514, and writes them directly to my-topic in kafka.

    a1.sources  = r1
    a1.channels = ckafka1
    a1.sinks    = skafka1

    a1.sources.r1.type                  = syslogudp
    a1.sources.r1.port                  = 514
    a1.sources.r1.channels              = ckafka1

    a1.channels.ckafka1.type                = memory
    a1.channels.ckafka1.capacity            = 10000
    a1.channels.ckafka1.transactionCapacity = 200

    a1.sinks.skafka1.type                   = org.apache.flume.sinks.KafkaSink
    a1.sinks.skafka1.channel                = ckafka1
    a1.sinks.skafka1.batchSize              = 200
    a1.sinks.skafka1.topic                  = my-topic
    a1.sinks.skafka1.metadata.broker.list   = kafka1:9092,kafka2:9092,kafka3:9092
    a1.sinks.skafka1.producer.type          = sync
    a1.sinks.skafka1.request.required.acks  = 1
    a1.sinks.skafka1.serializer.class       = kafka.serializer.StringEncoder
    a1.sinks.skafka1.compression.codec      = 1

All sink configuration is passed to kafka producer. So it's normal to get warnings like `Property batchSize is not valid`.

Increase channel capacities and batch size if necessary.

### License

APLv2
