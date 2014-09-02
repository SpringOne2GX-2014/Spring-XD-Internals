/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package hello;

import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.format.datetime.DateFormatter;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.xd.tuple.Tuple;
import org.springframework.xd.tuple.TupleBuilder;

/**
 * @author Patrick Peralta
 */
public class ModuleTemplate extends MessageProducerSupport {  // todo: rename this class to match your module name

	private final AtomicBoolean running = new AtomicBoolean(false);

	private final AtomicLong sequence = new AtomicLong();

	private final Random random = new Random();

	private final ExecutorService executorService = Executors.newSingleThreadExecutor(new CustomizableThreadFactory(
			"source-template"));  // todo: rename this thread to match your module name

	@Override
	protected void doStart() {
		if (running.compareAndSet(false, true)) {
			executorService.submit(new SampleExecutor());
		}
	}

	@Override
	protected void doStop() {
		if (running.compareAndSet(true, false)) {
			executorService.shutdown();
		}
	}

	class SampleExecutor implements Runnable {

		@Override
		public void run() {
			// todo: your module code goes here; don't forget to invoke
			// sendMessage to send data to the output channel
			DateFormatter dateFormatter = new DateFormatter("yyyy-MM-dd HH:mm:ss");

			while (running.get()) {
				Tuple tuple = TupleBuilder.tuple()
					.put("sequence", sequence.getAndIncrement())
					.put("date", dateFormatter.print(new Date(), Locale.US))
					.put("random", random.nextLong())
					.build();

				sendMessage(MessageBuilder.withPayload(tuple).build());

				try {
					Thread.sleep(1000);
				}
				catch (InterruptedException e) {
					// no need to reset the thread's interrupt status via
					// Thread.currentThread().interrupt() because this
					// thread will complete execution by clearing the
					// "running" flag

					running.set(false);
				}
			}

		}
	}

}
