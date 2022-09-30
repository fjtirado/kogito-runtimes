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

import com.fasterxml.jackson.databind.ObjectMapper;

import io.cloudevents.CloudEvent;

public class JacksonCloudEventUnmarshaller<I, O> extends AbstractCloudEventUnmarshaller<I, O> {

    public JacksonCloudEventUnmarshaller(ObjectMapper objectMapper, Class<O> clazz) {
        super(objectMapper, clazz);
    }

    @Override
    public CloudEvent unmarshall(I event) throws IOException {
        return JacksonMarshallUtils.unmarshall(objectMapper, event, CloudEvent.class);
    }
}
