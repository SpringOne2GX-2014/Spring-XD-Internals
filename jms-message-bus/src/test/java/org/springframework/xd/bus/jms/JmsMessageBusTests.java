package org.springframework.xd.bus.jms;

import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author David Turanski
 * @since 1.0
 */
public class JmsMessageBusTests {
    @Test
    public void testActiveMq() {
        new ClassPathXmlApplicationContext("/META-INF/spring-xd/transports/activemq-config.xml") ;
    }
}
