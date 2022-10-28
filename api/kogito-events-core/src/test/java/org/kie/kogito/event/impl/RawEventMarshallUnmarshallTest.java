/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kie.kogito.event.impl;

import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kie.kogito.jackson.utils.ObjectMapperFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.kie.kogito.event.impl.DataEventTestUtils.testEventMarshalling;

class RawEventMarshallUnmarshallTest {

    private static ObjectMapper mapper;

    @BeforeAll
    static void init() {
        mapper = ObjectMapperFactory.get();
    }

    @Test
    void testStringMarshaller() throws IOException {
        testEventMarshalling(new TestEvent("pepe"), new StringEventMarshaller(mapper), new JacksonEventDataUnmarshaller<>(mapper));
    }

    @Test
    void testObjectMarshaller() throws IOException {
        testEventMarshalling(new TestEvent("pepe"), new NoOpEventMarshaller(), new JacksonEventDataUnmarshaller<>(mapper));
    }

    @Test
    void testByteArrayMarshaller() throws IOException {
        testEventMarshalling(new TestEvent("pepe"), new ByteArrayEventMarshaller(mapper), new JacksonEventDataUnmarshaller<>(mapper));
    }
}