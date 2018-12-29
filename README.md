# spring-actuator-kafka

[![Build Status](https://travis-ci.org/stepio/spring-actuator-kafka.svg?branch=master)](https://travis-ci.org/stepio/spring-actuator-kafka)
[![Sonarcloud Status](https://sonarcloud.io/api/project_badges/measure?project=stepio_spring-actuator-kafka&metric=alert_status)](https://sonarcloud.io/dashboard?id=stepio_spring-actuator-kafka)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/100dd036555647f689505fee873af770)](https://app.codacy.com/app/stepio/spring-actuator-kafka?utm_source=github.com&utm_medium=referral&utm_content=stepio/spring-actuator-kafka&utm_campaign=Badge_Grade_Dashboard)
[![DepShield Badge](https://depshield.sonatype.org/badges/stepio/spring-actuator-kafka/depshield.svg)](https://depshield.github.io)

This tiny project provides implementation for `MetricsReporter` interface, backed with Spring Actuator's `GaugeService`.

Tested with [spring-projects/spring-kafka](https://github.com/spring-projects/spring-kafka). Example for consumer creation:
```java
    private Class<S> deserializerClass;
    private String kafkaBroker;
    private String kafkaGroup;
    private Integer kafkaConcurrency;
    // Spring Actuator's GaugeService should be initialized before passing it to Kafka
    // Use @Autowired, for example
    private GaugeService gaugeService;

    public ConcurrentKafkaListenerContainerFactory<String, T> listenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, T> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(consumerConfigs()));
        factory.setConcurrency(kafkaConcurrency);
        return factory;
    }

    private Map<String, Object> consumerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBroker);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaGroup);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, deserializerClass);
        // implemented a "helper" method which sets the required properties:
        KafkaConfigUtils.configureKafkaMetrics(props, gaugeService);
        // alternalive option: public static void configureKafkaMetrics(Map<String, Object> configs, GaugeService gaugeService, String prefix, ScheduledExecutorService executorService, Long updateInterval)
        return props;
    }
```

Similar approach is relevant for producer creation:
```java
    private String kafkaBroker;
    // Spring Actuator's GaugeService should be initialized before passing it to Kafka
    // Use @Autowired, for example
    private GaugeService gaugeService;

    public KafkaTemplate<String, T> kafkaTemplate() {
        new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(createProducerConfigs()));
    }

    public Map<String, Object> createProducerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, broker);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        // implemented a "helper" method which sets the required properties:
        KafkaConfigUtils.configureKafkaMetrics(props, gaugeService);
        // alternalive option: public static void configureKafkaMetrics(Map<String, Object> configs, GaugeService gaugeService, String prefix, ScheduledExecutorService executorService, Long updateInterval)
        return props;
    }
```

As the result, your `/metrics` endoints gets lots of additional data, e.g.:
```properties
gauge.kafka.consumer-14.node-2.consumer-node-metrics.request-size-avg: 91,
gauge.kafka.consumer-7.consumer-fetch-manager-metrics.fetch-throttle-time-avg: 0,
gauge.kafka.consumer-15.consumer-metrics.request-size-avg: 95.13245033112582,
gauge.kafka.consumer-1.node-2.consumer-node-metrics.outgoing-byte-rate: 168.6803914951063,
gauge.kafka.consumer-11.consumer-metrics.io-ratio: 0.0002161823165518282,
gauge.kafka.consumer-14.consumer-fetch-manager-metrics.fetch-size-avg: 0,
gauge.kafka.consumer-11.node-2147483645.consumer-node-metrics.request-latency-max: "-Infinity",
gauge.kafka.consumer-16.node-2147483645.consumer-node-metrics.response-rate: 0.5638636920813925,
gauge.kafka.consumer-14.consumer-coordinator-metrics.commit-rate: 0.22379500377654069,
gauge.kafka.consumer-14.node-3.consumer-node-metrics.request-size-avg: 0,
gauge.kafka.consumer-8.consumer-coordinator-metrics.sync-time-max: 0,
gauge.kafka.consumer-11.consumer-fetch-manager-metrics.fetch-latency-avg: 500.9,
gauge.kafka.consumer-11.node-1.consumer-node-metrics.outgoing-byte-rate: 174.0321740153642,
gauge.kafka.consumer-9.consumer-fetch-manager-metrics.fetch-throttle-time-avg: 0,
gauge.kafka.consumer-14.node--2.consumer-node-metrics.request-size-max: "-Infinity",
gauge.kafka.consumer-15.consumer-coordinator-metrics.join-time-avg: 0,
gauge.kafka.consumer-4.node-2.consumer-node-metrics.request-latency-max: "-Infinity",
gauge.kafka.consumer-1.consumer-metrics.request-rate: 4.555501189491977,
gauge.kafka.consumer-2.node--2.consumer-node-metrics.request-size-avg: 0,

```

This implementation is just a POC - appreciate your feedback/recommendations. As a side effect, I do not intend to publish it to maven central as of now, so to get the jar you may do the following:

1.  Clone the project locally and navigate to its folder via console/terminal.
2.  Make sure that java 8 is installed and `JAVA_HOME` is set properly.
3.  Execute `./mvnw clean install`
4.  Enrich your project's pom.xml with next block:

```xml
    <dependency>
        <groupId>org.stepio.kafka</groupId>
        <artifactId>spring-actuator-kafka</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </dependency>
```

Related issues:
-   [spring-kafka/issues/127](https://github.com/spring-projects/spring-kafka/issues/127)
-   [spring-boot/issues/6227](https://github.com/spring-projects/spring-boot/issues/6227)
-   [KAFKA-3923](https://issues.apache.org/jira/browse/KAFKA-3923)
