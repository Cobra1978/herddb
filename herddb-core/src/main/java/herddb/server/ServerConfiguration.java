/*
 Licensed to Diennea S.r.l. under one
 or more contributor license agreements. See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership. Diennea S.r.l. licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.

 */
package herddb.server;

import java.nio.file.Path;
import java.util.Properties;

/**
 * Server configuration
 *
 * @author enrico.olivelli
 */
public final class ServerConfiguration {

    private final Properties properties;

    public static final String PROPERTY_NODEID = "server.nodeId";
    public static final String PROPERTY_MODE = "server.mode";

    public static final String PROPERTY_MODE_LOCAL = "local";
    public static final String PROPERTY_MODE_STANDALONE = "standalone";
    public static final String PROPERTY_MODE_CLUSTER = "cluster";

    public static final String PROPERTY_BASEDIR = "server.baseDir";
    public static final String PROPERTY_HOST = "server.host";
    public static final String PROPERTY_HOST_DEFAULT = "localhost";
    public static final String PROPERTY_PORT = "server.port";
    public static final String PROPERTY_SSL = "server.ssl";

    public static final String PROPERTY_ZOOKEEPER_ADDRESS = "server.zookeeper.address";
    public static final String PROPERTY_ZOOKEEPER_SESSIONTIMEOUT = "server.zookeeper.sessiontimeout";
    public static final String PROPERTY_ZOOKEEPER_PATH = "server.zookeeper.path";
    
    public static final String PROPERTY_BOOKKEEPER_ENSEMBLE = "server.bookkeeper.ensemble";
    public static final int  PROPERTY_BOOKKEEPER_ENSEMBLE_DEFAULT = 1;
    public static final String PROPERTY_BOOKKEEPER_WRITEQUORUMSIZE = "server.bookkeeper.writequorumsize";
    public static final int  PROPERTY_BOOKKEEPER_WRITEQUORUMSIZE_DEFAULT = 1;
    public static final String PROPERTY_BOOKKEEPER_ACKQUORUMSIZE = "server.bookkeeper.ackquorumsize";
    public static final int  PROPERTY_BOOKKEEPER_ACKQUORUMSIZE_DEFAULT = 1;
    public static final String PROPERTY_BOOKKEEPER_LOGRETENTION_PERIOD = "server.bookkeeper.logretentionperiod";
    public static final String PROPERTY_BOOKKEEPER_MAX_LOGICAL_LOG_FILESIZE = "server.bookkeeper.maxlogfilesize";
    
    
    
    
     private int ensemble = 1;
    private int writeQuorumSize = 1;
    private int ackQuorumSize = 1;
    private long ledgersRetentionPeriod = 1000 * 60 * 60 * 24;
    private long maxLogicalLogFileSize = 1024 * 1024 * 256;
    
    
    public static final String PROPERTY_LOG_RETENTION_PERIOD = "server.log.retention.period";
    public static final int  PROPERTY_LOG_RETENTION_PERIOD_DEFAULT = 1000*10*60*24*2;

    public static final String PROPERTY_ZOOKEEPER_ADDRESS_DEFAULT = "localhost:1281";
    public static final String PROPERTY_ZOOKEEPER_PATH_DEFAULT = "/herd";
    public static final int PROPERTY_PORT_DEFAULT = 7000;
    public static final int PROPERTY_ZOOKEEPER_SESSIONTIMEOUT_DEFAULT = 40000;

    public ServerConfiguration(Properties properties) {
        this.properties = properties;
    }

    public ServerConfiguration(Path baseDir) {
        this();
        set(PROPERTY_BASEDIR, baseDir.toAbsolutePath());
    }

    public ServerConfiguration() {
        this.properties = new Properties();
    }

    public int getInt(String key, int defaultValue) {
        final String value = this.properties.getProperty(key);
        
        if ( value == null || value.isEmpty() )
            return defaultValue;
        
        return Integer.parseInt(value);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        final String value = this.properties.getProperty(key);
        
        if ( value == null || value.isEmpty() )
            return defaultValue;
        
        return Boolean.parseBoolean(value);
    }
    
    public long getLong(String key, long defaultValue) {
        final String value = this.properties.getProperty(key);
        
        if ( value == null || value.isEmpty() )
            return defaultValue;
        
        return Long.parseLong(value);
    }
    
    public String getString(String key, String defaultValue) {
        return this.properties.getProperty(key, defaultValue);
    }

    public ServerConfiguration set(String key, Object value) {
        if (value == null) {
            this.properties.remove(key);
        } else {
            this.properties.setProperty(key, value + "");
        }
        return this;
    }

}
