/*
 *  Copyright (c) 2026 fibonsai.com
 *  All rights reserved.
 *
 *  This source is subject to the Apache License, Version 2.0.
 *  Please see the LICENSE file for more information.
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.fibonsai.xtratej.adapter.duckdb;

import com.fibonsai.directflux.DirectFlux;
import com.fibonsai.xtratej.adapter.core.Adapter;
import com.fibonsai.xtratej.adapter.core.WithParams;
import com.fibonsai.xtratej.event.series.dao.EmptyTimeSeries;
import com.fibonsai.xtratej.event.series.dao.TimeSeries;
import com.fibonsai.xtratej.event.series.dao.builders.Double2TimeSeriesBuilder;
import org.duckdb.DuckDBDriver;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.NullNode;

import java.sql.*;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class DuckDBClient implements Adapter, WithParams {

    private static final Logger log = LoggerFactory.getLogger(DuckDBClient.class);

    private final @Nullable Connection conn;
    private final DirectFlux<TimeSeries> caller;

    private long delayElementsNano = 100;
    private JsonNode otherProperties = NullNode.getInstance();
    private String account = "";
    private String secret = "";
    private String query = "SELECT 1";
    private @Nullable Thread thread = null;
    private boolean subscribed = false;
    private boolean connected = false;

    private Runnable onConnect = () -> {};
    private Runnable onDisconnect = () -> {};

    public enum DuckDBKey {
        ACCOUNT("account"),
        SECRET("secret"),
        DELAY_ELEMENTS_NANO("delay-elements-nanos"),
        OTHER_PROPERTIES("other_properties"),
        QUERY("query")
        ;

        private final String key;

        DuckDBKey(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    public DuckDBClient(DirectFlux<TimeSeries> caller) {
        this.conn = getConnection();
        this.caller = caller;
    }

    private static @Nullable Class<?> getDriverClass() {
        Class<?> driverClass = null;
        try {
            driverClass = Class.forName("org.duckdb.DuckDBDriver");
        } catch (ClassNotFoundException e) {
            log.error(e.getMessage(), e);
        }
        return driverClass;
    }

    private static @Nullable Connection getConnection() {
        Connection connection = null;
        try {
            final Class<?> driverClass = getDriverClass();
            if (driverClass != null) {
                Properties props = new Properties();
                props.setProperty(DuckDBDriver.JDBC_STREAM_RESULTS, String.valueOf(true));
                connection = DriverManager.getConnection("jdbc:duckdb:", props);
            }
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
        return connection;
    }

    @Override
    public DuckDBClient setParams(JsonNode params) {
        String accountTemp = null;
        String secretTemp = null;
        for (var param: params.properties()) {
            String key = param.getKey();
            JsonNode value = param.getValue();
            if (DuckDBKey.ACCOUNT.key().equals(key) && value.isString()) {
                accountTemp = value.asString();
            }
            if (DuckDBKey.SECRET.key().equals(key) && value.isString()) {
                secretTemp = value.asString();
            }
            if (DuckDBKey.DELAY_ELEMENTS_NANO.key().equals(key) && value.isLong()) {
                delayElementsNano = value.asLong();
            }
            if (DuckDBKey.OTHER_PROPERTIES.key().equals(key) && value.isObject()) {
                otherProperties = value.asObject();
            }
            if (DuckDBKey.QUERY.key().equals(key) && value.isString()) {
                query = value.asString();
            }
        }
        account = accountTemp == null ? System.getenv("DUCKDB_ACCOUNT") : accountTemp;
        secret = secretTemp == null ? System.getenv("DUCKDB_SECRET") : secretTemp;
        return this;
    }

    public DuckDBClient onConnect(Runnable runnable) {
        this.onConnect = runnable;
        return this;
    }

    public DuckDBClient onDisconnect(Runnable runnable) {
        this.onDisconnect = runnable;
        return this;
    }

    @Override
    public boolean connect() {
        if (conn == null) {
            log.error("java.sql.Connection not defined: Driver not found");
            return false;
        }

        try {
            onConnect.run();
            connected = true;
        } catch (Exception e) {
            log.error("onConnect error", e);
        }
        return connected;
    }

    public void subscribe() {
        if (conn == null) {
            //noinspection LoggingSimilarMessage
            log.error("java.sql.Connection not defined: Driver not found");
            return;
        }
        thread = Thread.startVirtualThread(() -> {
            try (Statement stmt = conn.createStatement()) {
                if (query.contains("s3://") || query.contains("S3://")) {
                    String endpoint = otherProperties.hasNonNull("s3_endpoint") && otherProperties.get("s3_endpoint").isString() ?
                            otherProperties.get("s3_endpoint").asString() : "localhost:9000";
                    String urlStyle = otherProperties.hasNonNull("s3_url_style") && otherProperties.get("s3_url_style").isBoolean() ?
                            otherProperties.get("s3_url_style").asString() : "path";
                    String useSsl = otherProperties.hasNonNull("s3_use_ssl") && otherProperties.get("s3_use_ssl").isBoolean() ?
                            otherProperties.get("s3_use_ssl").asString() : "false";
                    String region = otherProperties.hasNonNull("s3_region") && otherProperties.get("s3_region").isBoolean() ?
                            otherProperties.get("s3_region").asString() : "us-east-1";
                    stmt.addBatch("INSTALL httpfs;");
                    stmt.addBatch("LOAD httpfs;");
                    String createSecret = """
                                CREATE SECRET s3_secret (
                                    TYPE S3,
                                    KEY_ID '%s',
                                    SECRET '%s',
                                    ENDPOINT '%s',
                                    URL_STYLE '%s',
                                    USE_SSL '%s',
                                    REGION '%s'
                                );
                            """.formatted(account, secret, endpoint, urlStyle, useSsl, region);
                    stmt.addBatch(createSecret);
                }
                if (otherProperties != NullNode.getInstance()) {
                    if (otherProperties.isArray()) {
                        for (var property : otherProperties.asArray()) {
                            if (property.isString()) {
                                stmt.addBatch(property.asString());
                            }
                        }
                    } else {
                        for (var property : otherProperties.properties()) {
                            String singleQuote = property.getValue().isString() ? "'" : "";
                            String setProperty = "SET %s=%s%s%s".formatted(
                                    property.getKey(),
                                    singleQuote,
                                    property.getValue().asString(),
                                    singleQuote);
                            stmt.addBatch(setProperty);
                        }
                    }
                }

                stmt.executeBatch();
                ResultSet resultSet = stmt.executeQuery(query);
                try {
                    while (resultSet.next()) {
                        TimeSeries timeseries = decode(resultSet);
                        caller.emitNext(timeseries);
                        TimeUnit.NANOSECONDS.sleep(delayElementsNano);
                    }
                    if (!resultSet.isClosed()) {
                        resultSet.close();
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        });
        subscribed = true;
    }

    private TimeSeries decode(ResultSet rs) {
        TimeSeries timeSeries = EmptyTimeSeries.INSTANCE;
        try {
            // HINT: ResultSetMetaData metaData = rs.getMetaData();

            String id = rs.getString("id");
            String side = rs.getString("side");
            double price = rs.getDouble("price");
            double amount = rs.getDouble("volume");
            long timestamp = rs.getLong("timestamp");

            Double2TimeSeriesBuilder builder = new Double2TimeSeriesBuilder().setId(query);
            builder.add(timestamp, price, amount);
            timeSeries = builder.build();
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
        return timeSeries;
    }

    @Override
    public boolean disconnect() {
        if (connected) {
            try {
                Objects.requireNonNull(conn).close();
                onDisconnect.run();
                connected = false;
                subscribed = false;
                if (thread != null) thread.interrupt();
            } catch (Exception e) {
                if (!(e instanceof InterruptedException)) {
                    log.error("disconnect error", e);
                }
            }
        }
        return !connected;
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    public boolean isSubscribed() {
        return subscribed;
    }
}
