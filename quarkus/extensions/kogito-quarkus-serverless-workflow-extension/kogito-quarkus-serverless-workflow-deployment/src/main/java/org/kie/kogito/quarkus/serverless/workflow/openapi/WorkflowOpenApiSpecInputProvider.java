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
package org.kie.kogito.quarkus.serverless.workflow.openapi;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import org.kie.kogito.quarkus.serverless.workflow.WorkflowCodeGenUtils;
import org.kie.kogito.quarkus.serverless.workflow.WorkflowOperationResource;
import org.kie.kogito.serverless.workflow.utils.ServerlessWorkflowUtils;

import io.quarkiverse.openapi.generator.deployment.codegen.OpenApiSpecInputProvider;
import io.quarkiverse.openapi.generator.deployment.codegen.SpecInputModel;
import io.quarkus.deployment.CodeGenContext;

public class WorkflowOpenApiSpecInputProvider implements OpenApiSpecInputProvider {

    private static final String KOGITO_PACKAGE_PREFIX = "org.kie.kogito.openapi.";

    @Override
    public List<SpecInputModel> read(CodeGenContext context) {
        Path inputDir = context.inputDir();
        while (!Files.exists(inputDir)) {
            inputDir = inputDir.getParent();
        }
        return WorkflowCodeGenUtils.operationResources(inputDir, ServerlessWorkflowUtils::isOpenApiOperation).map(this::getSpecInput).collect(Collectors.toList());
    }

    private SpecInputModel getSpecInput(WorkflowOperationResource resource) {
        return new SpecInputModel(resource.getOperationId().getFileName(), resource.getInputStream(), KOGITO_PACKAGE_PREFIX + resource.getOperationId().getPackageName());
    }
}
