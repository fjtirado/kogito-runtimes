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
package org.kie.kogito.quarkus.serverless.workflow.rpc;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import org.kie.kogito.quarkus.serverless.workflow.WorkflowCodeGenUtils;
import org.kie.kogito.quarkus.serverless.workflow.WorkflowOperationResource;
import org.kie.kogito.serverless.workflow.io.ClassPathContentLoader;
import org.kie.kogito.serverless.workflow.io.FileContentLoader;
import org.kie.kogito.serverless.workflow.io.URIContentLoader;

import io.quarkus.bootstrap.prebuild.CodeGenException;
import io.quarkus.deployment.CodeGenContext;
import io.quarkus.deployment.CodeGenProvider;
import io.serverlessworkflow.api.functions.FunctionDefinition;
import io.serverlessworkflow.api.functions.FunctionDefinition.Type;

public class WorkflowRPCCodeGenProvider implements CodeGenProvider {

    @Override
    public String providerId() {
        return "serverless-workflow-grpc";
    }

    @Override
    public String inputExtension() {
        return "json";
    }

    @Override
    public String inputDirectory() {
        return "resources";
    }

    @Override
    public boolean trigger(CodeGenContext context) throws CodeGenException {
        List<WorkflowOperationResource> resources = WorkflowCodeGenUtils.operationResources(context.inputDir(), this::isRPC).collect(Collectors.toList());
        boolean result = false;
        // write files to proto dir
        Path outputPath = context.inputDir().getParent().resolve("proto");
        for (WorkflowOperationResource resource : resources) {
            if (shouldWriteToProto(resource, outputPath)) {
                writeResource(outputPath, resource);
            }
        }
        return result;
    }

    private boolean shouldWriteToProto(WorkflowOperationResource resource, Path outputPath) {
        URIContentLoader contentLoader = resource.getContentLoader();
        switch (contentLoader.type()) {
            case FILE:
                return !((FileContentLoader) contentLoader).getPath().startsWith(outputPath);
            case CLASSPATH:
                try {
                    return !Path.of(((ClassPathContentLoader) contentLoader).getResource().toURI().getPath()).startsWith(outputPath);
                } catch (URISyntaxException e) {
                    throw new IllegalArgumentException(e);
                }
            case HTTP:
            default:
                return true;
        }
    }

    private boolean isRPC(FunctionDefinition function) {
        return function.getType() == Type.RPC;
    }

    private void writeResource(Path outputPath, WorkflowOperationResource resource) throws CodeGenException {
        try (InputStream is = resource.getContentLoader().getInputStream()) {
            Files.write(outputPath.resolve(resource.getOperationId().getFileName()), is.readAllBytes());
        } catch (IOException io) {
            throw new CodeGenException(io);
        }
    }
}
