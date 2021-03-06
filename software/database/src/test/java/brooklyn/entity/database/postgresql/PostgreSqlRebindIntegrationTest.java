/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package brooklyn.entity.database.postgresql;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import brooklyn.entity.proxying.EntitySpec;
import brooklyn.entity.rebind.RebindTestFixtureWithApp;
import brooklyn.location.basic.LocalhostMachineProvisioningLocation;
import brooklyn.test.EntityTestUtils;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

public class PostgreSqlRebindIntegrationTest extends RebindTestFixtureWithApp {

    private LocalhostMachineProvisioningLocation loc;
    
    @BeforeMethod(alwaysRun=true)
    @Override
    public void setUp() throws Exception {
        super.setUp();
        loc = origApp.newLocalhostProvisioningLocation();
    }

    @Test(groups = {"Integration"})
    public void testRebind() throws Exception {
        origApp.createAndManageChild(EntitySpec.create(PostgreSqlNode.class));
        origApp.start(ImmutableList.of(loc));

        // rebind
        rebind();
        final PostgreSqlNode newEntity = (PostgreSqlNode) Iterables.find(newApp.getChildren(), Predicates.instanceOf(PostgreSqlNode.class));

        // confirm effectors still work on entity
        EntityTestUtils.assertAttributeEqualsEventually(newEntity, PostgreSqlNode.SERVICE_UP, true);
        newEntity.stop();
        EntityTestUtils.assertAttributeEqualsEventually(newEntity, PostgreSqlNode.SERVICE_UP, false);
    }
}
