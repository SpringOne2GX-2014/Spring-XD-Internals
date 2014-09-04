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

package org.springframework.xd.modules;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.xd.tuple.TupleBuilder;

/**
 * Implementation of Spring Integration {@link MessageProducerSupport}
 * that publishes <a href="http://en.wikipedia.org/wiki/Vmstat"><code>vmstat</code></a>
 * output as Spring XD {@link org.springframework.xd.tuple.Tuple tuples}.
 * The following fields are included:
 * <ul>
 *  <li>waitingProcessCount</li>
 *  <li>sleepingProcessCount</li>
 *  <li>virtualMemoryUsage</li>
 *  <li>freeMemory</li>
 *  <li>bufferMemory</li>
 *  <li>cacheMemory</li>
 *  <li>swapIn</li>
 *  <li>bytesIn</li>
 *  <li>bytesOut</li>
 *  <li>interruptsPerSecond</li>
 *  <li>contextSwitchesPerSecond</li>
 *  <li>userCpu</li>
 *  <li>kernelCpu</li>
 *  <li>idleCpu</li>
 *  </ul>
 *  To deploy a simple stream:
 *  <p>
 *    <code>
 *      stream create --name v --definition "vmstat|log"
 *    </code>
 *  </p>
 *  To include a filter:
 *  <p>
 *    <code>
 *      stream create --name v --definition "vmstat|filter --expression='payload.userCpu > 50'|log"
 *    </code>
 *  </p>
 *
 * @author Patrick Peralta
 */
public class VMStat extends MessageProducerSupport {

	/**
	 * The default {@code vmstat} command.
	 */
	public static final String DEFAULT_VM_STAT_COMMAND = "vmstat -n 1";

	/**
	 * Logger.
	 */
	private final Logger logger = LoggerFactory.getLogger(getClass());

	/**
	 * The {@code vmstat} command. This command <em>must</em> include
	 * the {@code delay} parameter so that the program continues to
	 * produce output (as opposed to exiting immediately). The
	 * default value is "{@value #DEFAULT_VM_STAT_COMMAND}".
	 */
	private volatile String vmStatCommand = DEFAULT_VM_STAT_COMMAND;

	/**
	 * Flag that determines if the module is running. This is set
	 * on {@link #doStart()} and is cleared on {@link #doStop()} or
	 * if an exception occurs while processing the {@code vmstat}
	 * output.
	 */
	private final AtomicBoolean running = new AtomicBoolean(false);

	/**
	 * Latch that is triggered when the module is no longer producing
	 * output (either via explicit shutdown or error).
	 */
	private final CountDownLatch shutdownLatch = new CountDownLatch(1);

	/**
	 * Thread for executing and reading the output of {@code vmstat}.
	 */
	private final ExecutorService executorService =
			Executors.newSingleThreadExecutor(new CustomizableThreadFactory("vmstat"));

	/**
	 * @see #vmStatCommand
	 */
	public String getVmStatCommand() {
		return vmStatCommand;
	}

	/**
	 * @see #vmStatCommand
	 */
	public void setVmStatCommand(String vmStatCommand) {
		this.vmStatCommand = vmStatCommand;
	}

	/**
	 * Start the thread that executes and processes the {@code vmstat}
	 * output. This has no effect if the module is already running.
	 *
	 * @throws UnsupportedOperationException if this module is deployed
	 *         on a non Linux operating system
	 */
	@Override
	protected void doStart() {
		String os = System.getProperty("os.name");
		if (!os.toLowerCase().contains("linux")) {
			throw new UnsupportedOperationException("This module is only supported on Linux; OS detected: " + os);
		}

		if(running.compareAndSet(false, true)) {
			logger.info("Starting vmstat");
			executorService.submit(new VMStatExecutor());
			logger.info("Started vmstat");
		}
	}

	/**
	 * Stop the {@code vmstat} process and shut down the thread
	 * processing its output. This has no effect if the module
	 * is not running.
	 */
	@Override
	protected void doStop() {
		if (running.compareAndSet(true, false)) {
			logger.info("Stopping vmstat");
			running.set(false);
			try {
				shutdownLatch.await(5, TimeUnit.SECONDS);
			}
			catch (InterruptedException e) {
				logger.warn("Interrupted while waiting for vmstat shutdown", e);
				Thread.currentThread().interrupt();
			}
			finally {
				executorService.shutdown();
				logger.info("Stopped vmstat");
			}
		}
	}


	/**
	 * {@link Callable} that executes and processes {@code vmstat} output.
	 */
	public class VMStatExecutor implements Callable<Void> {

		@Override
		public Void call() throws Exception {
			logger.debug("Starting vmstat loop");
			ProcessBuilder builder = new ProcessBuilder();
			builder.redirectErrorStream(true);
			builder.command(getVmStatCommand().split("\\s"));

			Process process = null;
			String line = null;
			try {
				process = builder.start();

				InputStreamReader in = new InputStreamReader(new BufferedInputStream(process.getInputStream()));
				BufferedReader reader = new BufferedReader(in);

				while (running.get() && (line = reader.readLine()) != null) {
					TupleBuilder tupleBuilder = TupleBuilder.tuple();
					StringTokenizer tokenizer = new StringTokenizer(line);

					try {
						tupleBuilder.put("waitingProcessCount", Integer.parseInt(tokenizer.nextToken()))
								.put("sleepingProcessCount", Integer.parseInt(tokenizer.nextToken()))
								.put("virtualMemoryUsage", Long.parseLong(tokenizer.nextToken()))
								.put("freeMemory", Long.parseLong(tokenizer.nextToken()))
								.put("bufferMemory", Long.parseLong(tokenizer.nextToken()))
								.put("cacheMemory", Long.parseLong(tokenizer.nextToken()))
								.put("swapIn", Long.parseLong(tokenizer.nextToken()))
								.put("swapOut", Long.parseLong(tokenizer.nextToken()))
								.put("bytesIn", Long.parseLong(tokenizer.nextToken()))
								.put("bytesOut", Long.parseLong(tokenizer.nextToken()))
								.put("interruptsPerSecond", Long.parseLong(tokenizer.nextToken()))
								.put("contextSwitchesPerSecond", Long.parseLong(tokenizer.nextToken()))
								.put("userCpu", Long.parseLong(tokenizer.nextToken()))
								.put("kernelCpu", Long.parseLong(tokenizer.nextToken()))
								.put("idleCpu", Long.parseLong(tokenizer.nextToken()));
					}
					catch (Exception e) {
						// most likely cause is parsing a header line; discard and continue
						tupleBuilder = null;
					}

					if (tupleBuilder != null) {
						sendMessage(MessageBuilder.withPayload(tupleBuilder.build()).build());
					}
				}
			}
			catch (IOException e) {
				logger.error("Unhandled exception", e);
				running.set(false);
			}
			finally {
				if (process != null) {
					process.destroy();
				}
				shutdownLatch.countDown();
				logger.debug("Stopping vmstat loop");
				logger.trace("running: {}, line: {}", running, line);
			}

			return null;
		}
	}

}
