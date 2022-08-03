package org.kie.kogito.serverless.workflow.mapper;

import java.util.Collections;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jbpm.process.instance.ProcessInstanceHolder;
import org.jbpm.process.instance.event.KogitoProcessVariableChangedEventImpl;
import org.kie.kogito.jackson.utils.JsonNodeFactoryListener;
import org.kie.kogito.jackson.utils.JsonNodeListener;
import org.kie.kogito.process.ProcessConfig;
import org.kie.kogito.uow.WorkUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.jackson.ObjectMapperCustomizer;

@ApplicationScoped
public class ObjectMapperConfig implements ObjectMapperCustomizer {
    
    @Inject
    ProcessConfig processConfig;
    
    @Override
    public void customize(ObjectMapper objectMapper) {
        objectMapper.setNodeFactory(new JsonNodeFactoryListener(Collections.singletonList(new JsonNodeListener() {
            
            @Override
            public void onValueChanged(JsonNode container, String property, JsonNode oldValue, JsonNode newValue) {
                processConfig.unitOfWorkManager().currentUnitOfWork().
                    intercept(WorkUnit.create( new KogitoProcessVariableChangedEventImpl(property, property,  oldValue, newValue, Collections.emptyList(), ProcessInstanceHolder.get(), null, ProcessInstanceHolder.get().getKnowledgeRuntime())  , e -> {}));;
            }
        })));
    }
}
