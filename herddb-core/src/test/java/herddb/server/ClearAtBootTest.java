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

import herddb.client.ClientConfiguration;
import herddb.client.HDBClient;
import herddb.client.HDBConnection;
import herddb.model.TableSpace;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Tests on clear at boot
 *
 * @author enrico.olivelli
 */
public class ClearAtBootTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void test() throws Exception {

        ServerConfiguration serverConfiguration = new ServerConfiguration(folder.newFolder().toPath());
        try (Server server = new Server(serverConfiguration)) {
            server.start();
            try (HDBClient client = new HDBClient(new ClientConfiguration(folder.newFolder().toPath()));
                    HDBConnection connection = client.openConnection()) {
                client.setClientSideMetadataProvider(new StaticClientSideMetadataProvider(server));
                server.waitForStandaloneBoot();
                long resultCreateTable = connection.executeUpdate(TableSpace.DEFAULT,
                        "CREATE TABLE mytable (id string primary key, n1 long, n2 integer)", 0, Collections.emptyList()).updateCount;
                Assert.assertEquals(1, resultCreateTable);

                Assert.assertEquals(1, connection.executeUpdate(TableSpace.DEFAULT, "INSERT INTO mytable (id,n1,n2) values(?,?,?)", 0, Arrays.asList("test_0", 1, 2)).updateCount);
                Map<String, Object> newValue = connection.executeUpdate(TableSpace.DEFAULT, "UPDATE mytable set n1= n1+1 where id=?", 0, Arrays.asList("test_0")).newvalue;
                assertEquals(Long.valueOf(2), newValue.get("n1"));

            }
        }
        serverConfiguration.set(ServerConfiguration.PROPERTY_CLEAR_AT_BOOT, true);
        try (Server server = new Server(serverConfiguration)) {
            server.start();
            try (HDBClient client = new HDBClient(new ClientConfiguration(folder.newFolder().toPath()));
                    HDBConnection connection = client.openConnection()) {
                client.setClientSideMetadataProvider(new StaticClientSideMetadataProvider(server));
                server.waitForStandaloneBoot();
                long resultCreateTable = connection.executeUpdate(TableSpace.DEFAULT,
                        "CREATE TABLE mytable (id string primary key, n1 long, n2 integer)", 0, Collections.emptyList()).updateCount;
                Assert.assertEquals(1, resultCreateTable);

                Assert.assertEquals(1, connection.executeUpdate(TableSpace.DEFAULT, "INSERT INTO mytable (id,n1,n2) values(?,?,?)", 0, Arrays.asList("test_0", 1, 2)).updateCount);
                Map<String, Object> newValue = connection.executeUpdate(TableSpace.DEFAULT, "UPDATE mytable set n1= n1+1 where id=?", 0, Arrays.asList("test_0")).newvalue;
                assertEquals(Long.valueOf(2), newValue.get("n1"));

            }
        }
    }
}
