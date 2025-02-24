/* See LICENSE for licensing and NOTICE for copyright. */
package org.ldaptive;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import org.ldaptive.ssl.SslConfig;

/**
 * Contains all the configuration data needed to control connections.
 *
 * @author  Middleware Services
 */
public class ConnectionConfig extends AbstractConfig
{

  /** Predicate that attempts a single reconnect. */
  public static final Predicate<RetryMetadata> ONE_RECONNECT_ATTEMPT =
    metadata -> metadata instanceof ClosedRetryMetadata && metadata.getAttempts() == 0;

  /** Predicate that attempts to reconnect forever, waiting for 5 seconds after the first attempt. */
  public static final Predicate<RetryMetadata> INFINITE_RECONNECT_ATTEMPTS =
    metadata -> {
      if (metadata instanceof ClosedRetryMetadata) {
        if (metadata.getAttempts() > 0) {
          try {
            // CheckStyle:MagicNumber OFF
            Thread.sleep(Duration.ofSeconds(5).toMillis());
            // CheckStyle:MagicNumber ON
          } catch (InterruptedException ignored) {}
        }
        return true;
      }
      return false;
    };

  /** Predicate that attempts to reconnect forever, backing off in 5 second intervals after the first attempt. */
  public static final Predicate<RetryMetadata> INFINITE_RECONNECT_ATTEMPTS_WITH_BACKOFF =
    metadata -> {
      if (metadata instanceof ClosedRetryMetadata) {
        if (metadata.getAttempts() > 0) {
          try {
            // CheckStyle:MagicNumber OFF
            Thread.sleep(Duration.ofSeconds(5).multipliedBy(metadata.getAttempts()).toMillis());
            // CheckStyle:MagicNumber ON
          } catch (InterruptedException ignored) {}
        }
        return true;
      }
      return false;
    };

  /** URL to the LDAP(s). */
  private String ldapUrl;

  /** Duration of time that connects will block. */
  private Duration connectTimeout = Duration.ofMinutes(1);

  /** Duration of time to wait for startTLS responses. */
  private Duration startTLSTimeout = Duration.ofMinutes(1);

  /** Duration of time to wait for responses. */
  private Duration responseTimeout = Duration.ofMinutes(1);

  /**
   * Duration of time that operations will block on reconnects, should generally be longer than {@link
   * #connectTimeout}.
   */
  private Duration reconnectTimeout = Duration.ofMinutes(2);

  /** Whether to automatically reconnect to the server when a connection is lost. Default is true. */
  private boolean autoReconnect = true;

  /**
   * Condition used to determine whether another reconnect attempt should be made. Default makes a single attempt only
   * if the connection was previously opened.
   */
  private Predicate<RetryMetadata> autoReconnectCondition = ONE_RECONNECT_ATTEMPT;

  /** Whether pending operations should be replayed after a reconnect. Default is true. */
  private boolean autoReplay = true;

  /** Configuration for SSL and startTLS connections. */
  private SslConfig sslConfig;

  /** Connect to LDAP using startTLS. */
  private boolean useStartTLS;

  /** Connection initializers to execute on {@link Connection#open()}. */
  private ConnectionInitializer[] connectionInitializers;

  /** Connection strategy. */
  private ConnectionStrategy connectionStrategy = new ActivePassiveConnectionStrategy();

  /** Connection validator. */
  private ConnectionValidator connectionValidator;

  /** Transport options. */
  private final Map<String, Object> transportOptions = new HashMap<>();


  /** Default constructor. */
  public ConnectionConfig() {}


  /**
   * Creates a new connection config.
   *
   * @param  url  to connect to
   */
  public ConnectionConfig(final String url)
  {
    setLdapUrl(url);
  }


  /**
   * Returns the ldap url.
   *
   * @return  ldap url
   */
  public String getLdapUrl()
  {
    return ldapUrl;
  }


  /**
   * Sets the ldap url.
   *
   * @param  url  of the ldap
   */
  public void setLdapUrl(final String url)
  {
    checkImmutable();
    checkStringInput(url, true);
    logger.trace("setting ldapUrl: {}", url);
    ldapUrl = url;
  }


  /**
   * Returns the connect timeout.
   *
   * @return  timeout
   */
  public Duration getConnectTimeout()
  {
    return connectTimeout;
  }


  /**
   * Sets the maximum amount of time that connects will block.
   *
   * @param  time  timeout for connects
   */
  public void setConnectTimeout(final Duration time)
  {
    checkImmutable();
    if (time == null || time.isNegative()) {
      throw new IllegalArgumentException("Connect timeout cannot be null or negative");
    }
    logger.trace("setting connectTimeout: {}", time);
    connectTimeout = time;
  }


  /**
   * Returns the startTLS timeout.
   *
   * @return  timeout
   */
  public Duration getStartTLSTimeout()
  {
    return startTLSTimeout;
  }


  /**
   * Sets the maximum amount of time that startTLS operations will wait for a response.
   *
   * @param  time  timeout for responses
   */
  public void setStartTLSTimeout(final Duration time)
  {
    checkImmutable();
    if (time == null || time.isNegative()) {
      throw new IllegalArgumentException("StartTLS timeout cannot be null or negative");
    }
    logger.trace("setting startTLSTimeout: {}", time);
    startTLSTimeout = time;
  }


  /**
   * Returns the response timeout.
   *
   * @return  timeout
   */
  public Duration getResponseTimeout()
  {
    return responseTimeout;
  }


  /**
   * Sets the maximum amount of time that operations will wait for a response.
   *
   * @param  time  timeout for responses
   */
  public void setResponseTimeout(final Duration time)
  {
    checkImmutable();
    if (time == null || time.isNegative()) {
      throw new IllegalArgumentException("Response timeout cannot be null or negative");
    }
    logger.trace("setting responseTimeout: {}", time);
    responseTimeout = time;
  }


  /**
   * Returns the reconnect timeout.
   *
   * @return  timeout
   */
  public Duration getReconnectTimeout()
  {
    return reconnectTimeout;
  }


  /**
   * Sets the maximum amount of time that operations will block waiting for a reconnect.
   *
   * @param  time  timeout for reconnects
   */
  public void setReconnectTimeout(final Duration time)
  {
    checkImmutable();
    if (time == null || time.isNegative()) {
      throw new IllegalArgumentException("Reconnect timeout cannot be null or negative");
    }
    logger.trace("setting reconnectTimeout: {}", time);
    reconnectTimeout = time;
  }


  /**
   * Returns whether connections will attempt to reconnect.
   *
   * @return  whether to automatically reconnect when a connection is lost
   */
  public boolean getAutoReconnect()
  {
    return autoReconnect;
  }


  /**
   * Sets whether connections will attempt to reconnect when unexpectedly closed.
   *
   * @param  b  whether to automatically reconnect when a connection is lost
   */
  public void setAutoReconnect(final boolean b)
  {
    checkImmutable();
    logger.trace("setting autoReconnect: {}", b);
    autoReconnect = b;
  }


  /**
   * Returns the auto reconnect condition.
   *
   * @return  auto reconnect condition
   */
  public Predicate<RetryMetadata> getAutoReconnectCondition()
  {
    return autoReconnectCondition;
  }


  /**
   * Sets the auto reconnect condition.
   *
   * @param  predicate  to determine whether to attempt a reconnect
   */
  public void setAutoReconnectCondition(final Predicate<RetryMetadata> predicate)
  {
    checkImmutable();
    logger.trace("setting autoReconnectCondition: {}", predicate);
    autoReconnectCondition = predicate;
  }


  /**
   * Returns whether operations should be replayed after a reconnect.
   *
   * @return  whether to auto replay
   */
  public boolean getAutoReplay()
  {
    return autoReplay;
  }


  /**
   * Sets whether operations will be replayed after a reconnect.
   *
   * @param  b  whether to replay operations
   */
  public void setAutoReplay(final boolean b)
  {
    checkImmutable();
    logger.trace("setting autoReplay: {}", b);
    autoReplay = b;
  }


  /**
   * Returns the ssl config.
   *
   * @return  ssl config
   */
  public SslConfig getSslConfig()
  {
    return sslConfig;
  }


  /**
   * Sets the ssl config.
   *
   * @param  config  ssl config
   */
  public void setSslConfig(final SslConfig config)
  {
    checkImmutable();
    logger.trace("setting sslConfig: {}", config);
    sslConfig = config;
  }


  /**
   * Returns whether startTLS will be used for connections.
   *
   * @return  whether startTLS will be used
   */
  public boolean getUseStartTLS()
  {
    return useStartTLS;
  }


  /**
   * Sets whether startTLS will be used for connections.
   *
   * @param  b  whether startTLS will be used
   */
  public void setUseStartTLS(final boolean b)
  {
    checkImmutable();
    logger.trace("setting useStartTLS: {}", b);
    useStartTLS = b;
  }


  /**
   * Returns the connection initializers.
   *
   * @return  connection initializers
   */
  public ConnectionInitializer[] getConnectionInitializers()
  {
    return connectionInitializers;
  }


  /**
   * Sets the connection initializers.
   *
   * @param  initializers  connection initializers
   */
  public void setConnectionInitializers(final ConnectionInitializer... initializers)
  {
    checkImmutable();
    checkArrayContainsNull(initializers);
    logger.trace("setting connectionInitializers: {}", Arrays.toString(initializers));
    connectionInitializers = initializers;
  }


  /**
   * Returns the connection strategy.
   *
   * @return  strategy for making connections
   */
  public ConnectionStrategy getConnectionStrategy()
  {
    return connectionStrategy;
  }


  /**
   * Sets the connection strategy.
   *
   * @param  strategy  for making new connections
   */
  public void setConnectionStrategy(final ConnectionStrategy strategy)
  {
    checkImmutable();
    logger.trace("setting connectionStrategy: {}", strategy);
    connectionStrategy = strategy;
  }


  /**
   * Returns the connection validator.
   *
   * @return  connection validator
   */
  public ConnectionValidator getConnectionValidator()
  {
    return connectionValidator;
  }


  /**
   * Sets the connection validator.
   *
   * @param  validator  for validating connections
   */
  public void setConnectionValidator(final ConnectionValidator validator)
  {
    checkImmutable();
    logger.trace("setting connectionValidator: {}", validator);
    connectionValidator = validator;
  }


  /**
   * Returns transport options.
   *
   * @return  transport options
   */
  public Map<String, ?> getTransportOptions()
  {
    return transportOptions;
  }


  /**
   * Sets transport options.
   *
   * @param  options  to set
   */
  public void setTransportOptions(final Map<String, ?> options)
  {
    checkImmutable();
    logger.trace("setting transport options: {}", options);
    transportOptions.putAll(options);
  }


  /**
   * Returns a transport option.
   *
   * @param  id  transport option id
   *
   * @return  transport option
   */
  public Object getTransportOption(final String id)
  {
    return transportOptions.get(id);
  }


  /**
   * Sets a transport option.
   *
   * @param  id  of the transport option
   * @param  value  of the transport option
   */
  public void setTransportOption(final String id, final Object value)
  {
    checkImmutable();
    logger.trace("setting transport options: {}={}", id, value);
    transportOptions.put(id, value);
  }


  /**
   * Returns a new connection config initialized with the supplied config.
   *
   * @param  config  connection config to read properties from
   *
   * @return  connection config
   */
  public static ConnectionConfig copy(final ConnectionConfig config)
  {
    final ConnectionConfig cc = new ConnectionConfig();
    cc.setLdapUrl(config.getLdapUrl());
    cc.setConnectTimeout(config.getConnectTimeout());
    cc.setStartTLSTimeout(config.getStartTLSTimeout());
    cc.setResponseTimeout(config.getResponseTimeout());
    cc.setReconnectTimeout(config.getReconnectTimeout());
    cc.setAutoReconnect(config.getAutoReconnect());
    cc.setAutoReconnectCondition(config.getAutoReconnectCondition());
    cc.setAutoReplay(config.getAutoReplay());
    cc.setSslConfig(config.getSslConfig() != null ? SslConfig.copy(config.getSslConfig()) : null);
    cc.setUseStartTLS(config.getUseStartTLS());
    cc.setConnectionInitializers(config.getConnectionInitializers());
    cc.setConnectionStrategy(
      config.getConnectionStrategy() != null ? config.getConnectionStrategy().newInstance() : null);
    cc.setConnectionValidator(config.getConnectionValidator());
    cc.setTransportOptions(config.getTransportOptions());
    return cc;
  }


  @Override
  public String toString()
  {
    return "[" +
      getClass().getName() + "@" + hashCode() + "::" +
      "ldapUrl=" + ldapUrl + ", " +
      "connectTimeout=" + connectTimeout + ", " +
      "startTLSTimeout=" + startTLSTimeout + ", " +
      "responseTimeout=" + responseTimeout + ", " +
      "reconnectTimeout=" + reconnectTimeout + ", " +
      "autoReconnect=" + autoReconnect + ", " +
      "autoReconnectCondition=" + autoReconnectCondition + ", " +
      "autoReplay=" + autoReplay + ", " +
      "sslConfig=" + sslConfig + ", " +
      "useStartTLS=" + useStartTLS + ", " +
      "connectionInitializers=" + Arrays.toString(connectionInitializers) + ", " +
      "connectionStrategy=" + connectionStrategy + ", " +
      "connectionValidator=" + connectionValidator + ", " +
      "transportOptions=" + transportOptions + "]";
  }


  /**
   * Creates a builder for this class.
   *
   * @return  new builder
   */
  public static Builder builder()
  {
    return new Builder();
  }


  // CheckStyle:OFF
  public static class Builder
  {

    private final ConnectionConfig object = new ConnectionConfig();


    protected Builder() {}


    public Builder url(final String url)
    {
      object.setLdapUrl(url);
      return this;
    }


    public Builder connectTimeout(final Duration timeout)
    {
      object.setConnectTimeout(timeout);
      return this;
    }


    public Builder responseTimeout(final Duration timeout)
    {
      object.setResponseTimeout(timeout);
      return this;
    }


    public Builder startTLSTimeout(final Duration timeout)
    {
      object.setStartTLSTimeout(timeout);
      return this;
    }


    public Builder reconnectTimeout(final Duration timeout)
    {
      object.setReconnectTimeout(timeout);
      return this;
    }


    public Builder autoReconnect(final boolean b)
    {
      object.setAutoReconnect(b);
      return this;
    }


    public Builder autoReconnectCondition(final Predicate<RetryMetadata> predicate)
    {
      object.setAutoReconnectCondition(predicate);
      return this;
    }


    public Builder autoReplay(final boolean b)
    {
      object.setAutoReplay(b);
      return this;
    }


    public Builder sslConfig(final SslConfig config)
    {
      object.setSslConfig(config);
      return this;
    }


    public Builder useStartTLS(final boolean b)
    {
      object.setUseStartTLS(b);
      return this;
    }


    public Builder connectionInitializers(final ConnectionInitializer... initializers)
    {
      object.setConnectionInitializers(initializers);
      return this;
    }


    public Builder connectionStrategy(final ConnectionStrategy strategy)
    {
      object.setConnectionStrategy(strategy);
      return this;
    }


    public Builder connectionValidator(final ConnectionValidator validator)
    {
      object.setConnectionValidator(validator);
      return this;
    }


    public Builder transportOption(final String id, final Object value)
    {
      object.setTransportOption(id, value);
      return this;
    }


    public ConnectionConfig build()
    {
      return object;
    }
  }
  // CheckStyle:ON
}
