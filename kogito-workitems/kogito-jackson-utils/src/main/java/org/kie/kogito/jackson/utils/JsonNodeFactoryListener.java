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
package org.kie.kogito.jackson.utils;

import java.util.Collection;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JsonNodeFactoryListener extends JsonNodeFactory {

    private static final long serialVersionUID = 1L;
    private Collection<JsonNodeListener> listeners;

    public JsonNodeFactoryListener(Collection<JsonNodeListener> listeners) {
        this.listeners = listeners;
    }

    @Override
    public ObjectNode objectNode() {
        return new ObjectNodeListenerAware(this, listeners);
    }
}
