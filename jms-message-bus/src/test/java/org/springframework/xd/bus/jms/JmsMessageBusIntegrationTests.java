package org.springframework.xd.bus.jms;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.xd.dirt.server.SingleNodeApplication;
import org.springframework.xd.dirt.stream.StreamDefinition;
import org.springframework.xd.dirt.test.SingleNodeIntegrationTestSupport;
import org.springframework.xd.test.RandomConfigurationSupport;

/**
 * @author David Turanski
 * @since 1.0
 */
public class JmsMessageBusIntegrationTests {
    private static SingleNodeIntegrationTestSupport integrationTestSupport;
    private static SingleNodeApplication singleNodeApplication;

    @BeforeClass
    public static void setUp() {
        new RandomConfigurationSupport();
        System.setProperty("XD_HOME","/Users/dturanski/spring-xd/spring-xd-1.0.0.RELEASE/xd");
        singleNodeApplication = new SingleNodeApplication();
        singleNodeApplication.run("--transport", "jms");
        integrationTestSupport = new SingleNodeIntegrationTestSupport(singleNodeApplication);
    }

    @Test
    public void testSimple() throws InterruptedException {
        StreamDefinition ticktock = new StreamDefinition("ticktock","time | log");
        StreamDefinition tap = new StreamDefinition("tap","tap:stream:ticktock > log");
        integrationTestSupport.createAndDeployStream(ticktock);
        integrationTestSupport.createAndDeployStream(tap);
        Thread.sleep(3000);
        integrationTestSupport.undeployStream(ticktock);
        integrationTestSupport.undeployStream(tap);
    }

    @AfterClass
    public static void cleanUp() {
        singleNodeApplication.close();
        RandomConfigurationSupport.cleanup();
        System.clearProperty("XD_HOME");
    }
}
