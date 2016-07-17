/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.stepio.kafka.support.metrics;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.kafka.clients.CommonClientConfigs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.actuate.metrics.GaugeService;

/**
 * Utility methods to simplify the initialization of properties, required for metrics' gathering.
 *
 * @author Igor Stepanov
 */
public final class KafkaConfigUtils {

	protected static final Logger LOGGER = LoggerFactory.getLogger(KafkaConfigUtils.class);

	private KafkaConfigUtils() {
		LOGGER.debug("Utility classes should not have a public or default constructor");
	}

	/**
	 * Method for setting Kafka-related properties, required for proper updating of metrics.
	 *
	 * @param configs         {@link Map} with Kafka-specific properties, required to initialize the appropriate consumer/producer.
	 * @param gaugeService    reference to an instance of Springs {@link GaugeService}, used to set the collected metrics
	 * @param prefix          initial part of the metric's label
	 * @param executorService reference to an instance of {@link ScheduledExecutorService}, used to schedule periodic values' recalculation for metrics
	 * @param updateInterval  interval for iterating the whole set of tracked metrics to recalculate and resubmit their values
	 */
	public static void configureKafkaMetrics(Map<String, Object> configs, GaugeService gaugeService, String prefix, ScheduledExecutorService executorService, Long updateInterval) {
		if (gaugeService == null) {
			throw new IllegalArgumentException("Initializing GaugeService as null is meaningless!");
		}
		configs.put(CommonClientConfigs.METRIC_REPORTER_CLASSES_CONFIG, Collections.singletonList(KafkaStatisticsProvider.class.getName()));
		configs.put(KafkaStatisticsProvider.METRICS_GAUGE_SERVICE_IMPL, gaugeService);
		LOGGER.debug("Set property {} with provided GaugeService instance reference", KafkaStatisticsProvider.METRICS_GAUGE_SERVICE_IMPL);
		if (executorService != null) {
			configs.put(KafkaStatisticsProvider.METRICS_UPDATE_EXECUTOR_IMPL, executorService);
			LOGGER.debug("Set property {} with provided ScheduledExecutorService instance reference", KafkaStatisticsProvider.METRICS_UPDATE_EXECUTOR_IMPL);
		}
		if (updateInterval != null) {
			configs.put(KafkaStatisticsProvider.METRICS_UPDATE_INTERVAL_PARAM, updateInterval);
			LOGGER.debug("Set property {} with value {}", KafkaStatisticsProvider.METRICS_UPDATE_INTERVAL_PARAM, updateInterval);
		}
		if (prefix != null) {
			configs.put(KafkaStatisticsProvider.METRICS_PREFIX_PARAM, prefix);
			LOGGER.debug("Set property {} with value {}", KafkaStatisticsProvider.METRICS_PREFIX_PARAM, prefix);
		}
	}

	/**
	 * Overloaded method, which sets updateInterval with default value.
	 *
	 * @param configs      {@link Map} with Kafka-specific properties, required to initialize the appropriate consumer/producer.
	 * @param gaugeService reference to an instance of Spring's {@link GaugeService}, used to set the collected metrics
	 */
	public static void configureKafkaMetrics(Map<String, Object> configs, GaugeService gaugeService) {
		configureKafkaMetrics(configs, gaugeService, null, null, null);
	}
}
