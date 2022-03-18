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

package org.kogito.workitem.rest.decorators;

import java.util.Map;

import org.kie.kogito.internal.process.runtime.KogitoWorkItem;

import io.vertx.mutiny.ext.web.client.HttpRequest;

import static org.kogito.workitem.rest.RestWorkItemHandlerUtils.getParam;
import static org.kogito.workitem.rest.RestWorkItemHandlerUtils.isEmpty;

public class ApiKeyAuthDecorator implements RequestDecorator {

    public final static String LOCATION = "_apiKeyLocation";
    public final static String PARAMETER = "_apiKeyParameter";
    public final static String KEY = "_apiKey";
    public final static String KEY_PREFIX = "_apiKeyPrefix";

    @Override
    public void decorate(KogitoWorkItem item, Map<String, Object> parameters, HttpRequest<?> request) {
        String apiKey = getApiKey(getParam(parameters, KEY_PREFIX, String.class, null), getParam(parameters, KEY, String.class, ""));
        if (!isEmpty(apiKey)) {
            String param = getParam(parameters, PARAMETER, String.class, "X-API-KEY");
            String location = getParam(parameters, LOCATION, String.class, "header");
            switch (location.trim().toLowerCase()) {
                case "query":
                    request.addQueryParam(param, apiKey);
                    break;
                default:
                case "header":
                    request.putHeader(param, apiKey);
            }
        }
    }

    private static String getApiKey(String apiKeyPrefix, String apiKey) {
        return apiKeyPrefix != null ? apiKeyPrefix + " " + apiKey : apiKey;
    }
}
