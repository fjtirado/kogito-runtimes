/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.kie.kogito.event.process;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the CloudEvent {@code source} field of a {@link ProcessDefinitionDataEvent} carries the
 * plain service URL, while {@code getData().getEndpoint()} carries the versioned process path.
 *
 * <p>
 * These two values are consumed differently by the Data Index:
 * <ul>
 * <li>{@code source} → stored as {@code serviceUrl} in the ProcessDefinitions query</li>
 * <li>{@code body.endpoint} → stored as {@code endpoint} in the ProcessDefinitions query</li>
 * </ul>
 * They must be distinct; conflating them was the regression introduced in SRVLOGIC-1124.
 */
public class ProcessDefinitionDataEventTest {

    private static final String SERVICE_URL = "http://callbackstatetimeouts.usecase3-platform-persistence-oc";
    private static final String PROCESS_ID = "callbackstatetimeouts";
    private static final String VERSION = "0.0.1";
    private static final String VERSIONED_ENDPOINT = SERVICE_URL + "/" + PROCESS_ID + "/" + VERSION;

    @Test
    public void testSourceIsServiceUrlAndEndpointIsVersioned() {
        ProcessDefinitionEventBody body = ProcessDefinitionEventBody.builder()
                .setId(PROCESS_ID)
                .setVersion(VERSION)
                .setEndpoint(VERSIONED_ENDPOINT)
                .setNodes(Collections.emptyList())
                .build();

        ProcessDefinitionDataEvent event = new ProcessDefinitionDataEvent(SERVICE_URL, body);

        // CloudEvent source → stored by the DI as "serviceUrl" — must be the plain base URL
        assertThat(event.getSource().toString()).isEqualTo(SERVICE_URL);

        // Body endpoint → stored by the DI as "endpoint" — must include version
        assertThat(event.getData().getEndpoint()).isEqualTo(VERSIONED_ENDPOINT);

        // They must be different values
        assertThat(event.getSource().toString()).isNotEqualTo(event.getData().getEndpoint());
    }
}
