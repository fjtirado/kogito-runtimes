/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
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
package org.kie.kogito.addon.quarkus.messaging.common;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.kie.kogito.event.CloudEventUnmarshallerFactory;
import org.kie.kogito.event.EventUnmarshaller;
import org.kie.kogito.event.impl.JacksonEventDataUnmarshaller;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.arc.DefaultBean;

@ApplicationScoped
public class EventUnmarshallerProducer {

    @Inject
    ObjectMapper objectMapper;

    @Produces
    @DefaultBean
    public EventUnmarshaller<Object> objectEventDataConverter() {
        return new JacksonEventDataUnmarshaller<>(objectMapper);
    }

    @Produces
    @DefaultBean
    public EventUnmarshaller<String> stringEventDataConverter() {
        return new JacksonEventDataUnmarshaller<>(objectMapper);
    }

    @Produces
    @DefaultBean
    public EventUnmarshaller<byte[]> bytesEventDataConverter() {
        return new JacksonEventDataUnmarshaller<>(objectMapper);
    }

    @Produces
    @DefaultBean
    public CloudEventUnmarshallerFactory<Message<Object>> objectCloudEventConverter() {
        return new ObjectQuarkusCloudEventUnmarshallerFactory(objectMapper);
    }

    @Produces
    @DefaultBean
    public CloudEventUnmarshallerFactory<Message<String>> stringCloudEventConverter() {
        return new StringQuarkusCloudEventUnmarshallerFactory(objectMapper);
    }

    @Produces
    @DefaultBean
    public CloudEventUnmarshallerFactory<Message<byte[]>> bytesCloudEventConverter() {
        return new ByteArrayQuarkusCloudEventUnmarshallerFactory(objectMapper);
    }
}
