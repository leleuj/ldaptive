/* See LICENSE for licensing and NOTICE for copyright. */
package org.ldaptive.transport;

import java.lang.reflect.Constructor;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.kqueue.KQueue;
import org.ldaptive.transport.netty.EpollTransport;
import org.ldaptive.transport.netty.KQueueTransport;
import org.ldaptive.transport.netty.NioTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating connections.
 *
 * @author  Middleware Services
 */
public final class TransportFactory
{

  /** Ldap transport system property. */
  private static final String TRANSPORT_PROPERTY = "org.ldaptive.transport";

  /** Logger for this class. */
  private static final Logger LOGGER = LoggerFactory.getLogger(TransportFactory.class);

  /** Custom transport constructor. */
  private static Constructor<?> transportConstructor;

  static {
    // Initialize a custom transport if a system property is found
    final String transportClass = System.getProperty(TRANSPORT_PROPERTY);
    if (transportClass != null) {
      try {
        LOGGER.info("Setting ldap transport to {}", transportClass);
        transportConstructor = Class.forName(transportClass).getDeclaredConstructor();
      } catch (Exception e) {
        LOGGER.error("Error instantiating {}", transportClass, e);
        throw new IllegalStateException(e);
      }
    }
  }


  /** Default constructor. */
  private TransportFactory() {}


  /**
   * The {@link #TRANSPORT_PROPERTY} property is checked and that class is loaded if provided. Otherwise a Netty
   * transport is returned.
   *
   * @return  ldaptive transport
   */
  public static Transport getTransport()
  {
    if (transportConstructor != null) {
      try {
        return (Transport) transportConstructor.newInstance();
      } catch (Exception e) {
        LOGGER.error("Error creating new transport instance with {}", transportConstructor, e);
        throw new IllegalStateException(e);
      }
    }
    final Transport transport;
    if (Epoll.isAvailable()) {
      transport = new EpollTransport(1);
    } else if (KQueue.isAvailable()) {
      transport = new KQueueTransport(1);
    } else {
      transport = new NioTransport(1);
    }
    return transport;
  }
}
