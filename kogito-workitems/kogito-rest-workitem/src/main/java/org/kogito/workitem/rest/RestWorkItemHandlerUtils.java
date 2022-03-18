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
package org.kogito.workitem.rest;

import java.util.Map;

import io.vertx.mutiny.core.Vertx;

import static org.kie.kogito.internal.utils.ConversionUtils.convert;

public class RestWorkItemHandlerUtils {

    private RestWorkItemHandlerUtils() {
    }

    private static Vertx vertx;

    public static synchronized Vertx vertx() {
        if (vertx == null) {
            vertx = Vertx.vertx();
        }
        return vertx;
    }

    public static <T> T getParam(Map<String, Object> parameters, String paramName, Class<T> type, T defaultValue) {
        Object value = parameters.remove(paramName);
        return value == null ? defaultValue : convert(value, type, v -> v.toString().toUpperCase());
    }

    public static boolean isEmpty(String value) {
        return value == null || value.isBlank();
    }

}
