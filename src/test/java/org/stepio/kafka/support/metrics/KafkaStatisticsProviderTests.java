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

import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyString;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.boot.actuate.metrics.GaugeService;

/**
 * Tests for {@link KafkaStatisticsProvider}.
 *
 * @author Igor Stepanov
 */
public class KafkaStatisticsProviderTests {

	private AtomicInteger submittingCounter;

	@Before
	public void initCounter() {
		this.submittingCounter = new AtomicInteger();
	}

	private GaugeService mockGaugeService() {
		GaugeService gauge = mock(GaugeService.class);
		willAnswer(new Answer<Void>() {
			public Void answer(InvocationOnMock invocation) {
				Object[] args = invocation.getArguments();
				KafkaConfigUtils.LOGGER.info("Called GaugeService.submit with arguments: {}", Arrays.toString(args));
				KafkaStatisticsProviderTests.this.submittingCounter.incrementAndGet();
				return null;
			}
		}).given(gauge).submit(anyString(), anyDouble());
		return gauge;
	}
}
