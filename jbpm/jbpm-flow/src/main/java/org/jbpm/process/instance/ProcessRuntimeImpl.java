/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jbpm.process.instance;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.drools.core.common.InternalWorkingMemory;
import org.drools.core.common.WorkingMemoryAction;
import org.drools.core.definitions.rule.impl.RuleImpl;
import org.drools.core.event.DefaultAgendaEventListener;
import org.drools.core.event.KogitoProcessEventSupport;
import org.drools.core.impl.InternalKnowledgeBase;
import org.drools.core.marshalling.impl.MarshallerReaderContext;
import org.drools.core.marshalling.impl.MarshallerWriteContext;
import org.drools.core.phreak.PropagationEntry;
import org.drools.core.time.TimeUtils;
import org.drools.core.time.TimerService;
import org.drools.core.time.impl.CommandServiceTimerJobFactoryManager;
import org.drools.core.time.impl.ThreadSafeTrackableTimeJobFactoryManager;
import org.drools.kogito.core.common.InternalKnowledgeRuntime;
import org.drools.kogito.core.event.ProcessEventSupport;
import org.drools.serialization.protobuf.ProtobufMessages.ActionQueue.Action;
import org.jbpm.process.core.event.EventFilter;
import org.jbpm.process.core.event.EventTransformer;
import org.jbpm.process.core.event.EventTypeFilter;
import org.jbpm.process.core.timer.BusinessCalendar;
import org.jbpm.process.core.timer.DateTimeUtils;
import org.jbpm.process.core.timer.Timer;
import org.jbpm.process.instance.event.DefaultSignalManagerFactory;
import org.jbpm.process.instance.impl.DefaultProcessInstanceManagerFactory;
import org.jbpm.ruleflow.core.RuleFlowProcess;
import org.jbpm.workflow.core.node.EventTrigger;
import org.jbpm.workflow.core.node.StartNode;
import org.jbpm.workflow.core.node.Trigger;
import org.kie.kogito.internal.KieBase;
import org.kie.kogito.internal.definition.process.Node;
import org.kie.kogito.internal.definition.process.Process;
import org.kie.kogito.internal.event.process.ProcessEventListener;
import org.kie.kogito.internal.event.rule.MatchCreatedEvent;
import org.kie.kogito.internal.process.CorrelationKey;
import org.kie.kogito.internal.runtime.process.EventListener;
import org.kie.kogito.internal.runtime.process.ProcessInstance;
import org.kie.kogito.internal.runtime.process.WorkItemManager;
import org.kie.kogito.internal.runtime.rule.AgendaFilter;
import org.kie.kogito.internal.utils.CompositeClassLoader;
import org.kie.kogito.jobs.DurationExpirationTime;
import org.kie.kogito.jobs.ExactExpirationTime;
import org.kie.kogito.jobs.ExpirationTime;
import org.kie.kogito.jobs.JobsService;
import org.kie.kogito.jobs.ProcessJobDescription;
import org.kie.kogito.services.uow.CollectingUnitOfWorkFactory;
import org.kie.kogito.services.uow.DefaultUnitOfWorkManager;
import org.kie.kogito.signal.SignalManager;
import org.kie.kogito.uow.UnitOfWorkManager;
import org.kie.services.jobs.impl.InMemoryJobService;

public class ProcessRuntimeImpl implements InternalProcessRuntime {

    private InternalKnowledgeRuntime kruntime;
    private ProcessInstanceManager processInstanceManager;
    private SignalManager signalManager;
    private JobsService jobService;
    private ProcessEventSupport processEventSupport;
    private UnitOfWorkManager unitOfWorkManager;

    public ProcessRuntimeImpl(InternalKnowledgeRuntime kruntime) {
        this.kruntime = kruntime;
        TimerService timerService = kruntime.getTimerService();
        if (!(timerService.getTimerJobFactoryManager() instanceof CommandServiceTimerJobFactoryManager)) {
            timerService.setTimerJobFactoryManager(new ThreadSafeTrackableTimeJobFactoryManager());
        }

        ((CompositeClassLoader) getRootClassLoader()).addClassLoader(getClass().getClassLoader());
        initProcessInstanceManager();
        initSignalManager();
        unitOfWorkManager = new DefaultUnitOfWorkManager(new CollectingUnitOfWorkFactory());
        jobService = new InMemoryJobService(this, unitOfWorkManager);
        processEventSupport = new KogitoProcessEventSupport(unitOfWorkManager);
        if (isActive()) {
            initProcessEventListeners();
            initStartTimers();
        }
        initProcessActivationListener();
    }

    public ProcessRuntimeImpl(InternalWorkingMemory workingMemory) {
        TimerService timerService = workingMemory.getTimerService();
        if (!(timerService.getTimerJobFactoryManager() instanceof CommandServiceTimerJobFactoryManager)) {
            timerService.setTimerJobFactoryManager(new ThreadSafeTrackableTimeJobFactoryManager());
        }

        this.kruntime = (InternalKnowledgeRuntime) workingMemory.getKnowledgeRuntime();
        initProcessInstanceManager();
        initSignalManager();
        unitOfWorkManager = new DefaultUnitOfWorkManager(new CollectingUnitOfWorkFactory());
        jobService = new InMemoryJobService(this, unitOfWorkManager);
        processEventSupport = new KogitoProcessEventSupport(unitOfWorkManager);
        if (isActive()) {
            initProcessEventListeners();
            initStartTimers();
        }
        initProcessActivationListener();
    }

    public void initStartTimers() {
        KieBase kbase = kruntime.getKieBase();
        Collection<Process> processes = kbase.getProcesses();
        for (Process process : processes) {
            RuleFlowProcess p = (RuleFlowProcess) process;
            List<StartNode> startNodes = p.getTimerStart();
            if (startNodes != null && !startNodes.isEmpty()) {

                for (StartNode startNode : startNodes) {
                    if (startNode != null && startNode.getTimer() != null) {
                        jobService.scheduleProcessJob(ProcessJobDescription.of(createTimerInstance(startNode.getTimer(), kruntime), p.getId()));
                    }
                }
            }
        }
    }

    private void initProcessInstanceManager() {
        processInstanceManager = new DefaultProcessInstanceManagerFactory().createProcessInstanceManager(kruntime);
    }

    private void initSignalManager() {
        signalManager = new DefaultSignalManagerFactory().createSignalManager(kruntime);
    }

    private ClassLoader getRootClassLoader() {
        KieBase kbase = (kruntime.getKieBase());
        if (kbase != null) {
            return ((InternalKnowledgeBase) kbase).getRootClassLoader();
        }
        CompositeClassLoader result = new CompositeClassLoader();
        result.addClassLoader(Thread.currentThread().getContextClassLoader());
        return result;
    }

    @Override
    public ProcessInstance startProcess(String processId) {
        return startProcess(processId, null, null, null);
    }

    @Override
    public ProcessInstance startProcess(String processId, Map<String, Object> parameters) {
        return startProcess(processId, parameters, null, null);
    }

    public ProcessInstance startProcess(String processId, Map<String, Object> parameters, String trigger) {
        return startProcess(processId, parameters, trigger, null);
    }

    @Override
    public ProcessInstance startProcess(String processId, AgendaFilter agendaFilter) {
        return startProcess(processId, null, null, agendaFilter);
    }

    @Override
    public ProcessInstance startProcess(String processId, Map<String, Object> parameters, AgendaFilter agendaFilter) {
        return startProcess(processId, parameters, null, agendaFilter);
    }

    private ProcessInstance startProcess(String processId, Map<String, Object> parameters, String trigger, AgendaFilter agendaFilter) {
        ProcessInstance processInstance = createProcessInstance(processId, parameters);
        if ( processInstance != null ) {
            // start process instance
            return startProcessInstance(processInstance.getId(), trigger, agendaFilter);
        }
        return null;
    }

    @Override
    public ProcessInstance createProcessInstance(String processId,
                                                 Map<String, Object> parameters) {
        return createProcessInstance(processId, null, parameters);
    }

    @Override
    public ProcessInstance startProcessInstance(String processInstanceId, String trigger) {
        return startProcessInstance( processInstanceId, trigger, null );
    }

    private ProcessInstance startProcessInstance(String processInstanceId, String trigger, AgendaFilter agendaFilter) {
        try {
            kruntime.startOperation();

            ProcessInstance processInstance = getProcessInstance(processInstanceId);
            org.jbpm.process.instance.ProcessInstance jbpmProcessInstance = (org.jbpm.process.instance.ProcessInstance) processInstance;

            jbpmProcessInstance.configureSLA();
            getProcessEventSupport().fireBeforeProcessStarted(processInstance, kruntime);
            jbpmProcessInstance.setAgendaFilter( agendaFilter );
            jbpmProcessInstance.start(trigger);
            getProcessEventSupport().fireAfterProcessStarted(processInstance, kruntime);
            return processInstance;
        } finally {
            kruntime.endOperation();
        }
    }

    @Override
    public ProcessInstance startProcessInstance(String processInstanceId) {
        return startProcessInstance(processInstanceId, null);
    }

    @Override
    public ProcessInstance startProcess(String processId, CorrelationKey correlationKey, Map<String, Object> parameters) {
        ProcessInstance processInstance = createProcessInstance(processId, correlationKey, parameters);
        if (processInstance != null) {
            return startProcessInstance(processInstance.getId());
        }
        return null;
    }

    @Override
    public ProcessInstance createProcessInstance(String processId, CorrelationKey correlationKey, Map<String, Object> parameters) {
        try {
            kruntime.startOperation();

            final Process process = kruntime.getKieBase().getProcess(processId);
            if (process == null) {
                throw new IllegalArgumentException("Unknown process ID: " + processId);
            }
            return startProcess(process, correlationKey, parameters);
        } finally {
            kruntime.endOperation();
        }
    }

    private org.jbpm.process.instance.ProcessInstance startProcess(Process process, CorrelationKey correlationKey, Map<String, Object> parameters) {
        ProcessInstanceFactory conf = ProcessInstanceFactoryRegistry.INSTANCE.getProcessInstanceFactory(process);
        if (conf == null) {
            throw new IllegalArgumentException("Illegal process type: " + process.getClass());
        }
        return conf.createProcessInstance(process,
                                          correlationKey,
                                          kruntime,
                                          parameters);
    }

    @Override
    public ProcessInstanceManager getProcessInstanceManager() {
        return processInstanceManager;
    }

    @Override
    public JobsService getJobsService() {
        return jobService;
    }

    @Override
    public SignalManager getSignalManager() {
        return signalManager;
    }

    @Override
    public Collection<ProcessInstance> getProcessInstances() {
        return processInstanceManager.getProcessInstances();
    }

    @Override
    public ProcessInstance getProcessInstance(String id) {
        return getProcessInstance(id, false);
    }

    @Override
    public ProcessInstance getProcessInstance(String id, boolean readOnly) {
        return processInstanceManager.getProcessInstance(id, readOnly);
    }

    public void removeProcessInstance(ProcessInstance processInstance) {
        processInstanceManager.removeProcessInstance(processInstance);
    }

    public void initProcessEventListeners() {
        for (Process process : kruntime.getKieBase().getProcesses()) {
            initProcessEventListener(process);
        }
    }

    public void removeProcessEventListeners() {
        for (Process process : kruntime.getKieBase().getProcesses()) {
            removeProcessEventListener(process);
        }
    }

    private void removeProcessEventListener(Process process) {
        if (process instanceof RuleFlowProcess) {
            String type = (String) ((RuleFlowProcess) process).getRuntimeMetaData().get("StartProcessEventType");
            StartProcessEventListener listener = (StartProcessEventListener) ((RuleFlowProcess) process).getRuntimeMetaData().get("StartProcessEventListener");
            if (type != null && listener != null) {
                signalManager.removeEventListener(type, listener);
            }
        }
    }

    private void initProcessEventListener(Process process) {
        if (process instanceof RuleFlowProcess) {
            for (Node node : ((RuleFlowProcess) process).getNodes()) {
                if (node instanceof StartNode) {
                    StartNode startNode = (StartNode) node;
                    if (startNode != null) {
                        List<Trigger> triggers = startNode.getTriggers();
                        if (triggers != null) {
                            for (Trigger trigger : triggers) {
                                if (trigger instanceof EventTrigger) {
                                    final List<EventFilter> filters = ((EventTrigger) trigger).getEventFilters();
                                    String type = null;
                                    for (EventFilter filter : filters) {
                                        if (filter instanceof EventTypeFilter) {
                                            type = ((EventTypeFilter) filter).getType();
                                        }
                                    }
                                    StartProcessEventListener listener = new StartProcessEventListener(process.getId(),
                                                                                                       filters,
                                                                                                       trigger.getInMappings(),
                                                                                                       startNode.getEventTransformer());
                                    signalManager.addEventListener(type, listener);
                                    ((RuleFlowProcess) process).getRuntimeMetaData().put("StartProcessEventType", type);
                                    ((RuleFlowProcess) process).getRuntimeMetaData().put("StartProcessEventListener", listener);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public ProcessEventSupport getProcessEventSupport() {
        return processEventSupport;
    }

    @Override
    public void setProcessEventSupport(ProcessEventSupport processEventSupport) {
        this.processEventSupport = processEventSupport;
    }

    @Override
    public void addEventListener(final ProcessEventListener listener) {
        this.processEventSupport.addEventListener(listener);
    }

    @Override
    public void removeEventListener(final ProcessEventListener listener) {
        this.processEventSupport.removeEventListener(listener);
    }

    @Override
    public List<ProcessEventListener> getProcessEventListeners() {
        return processEventSupport.getEventListeners();
    }

    private void initProcessActivationListener() {
        kruntime.addEventListener(new DefaultAgendaEventListener() {
            @Override
            public void matchCreated(MatchCreatedEvent event) {
                String ruleFlowGroup = ((RuleImpl) event.getMatch().getRule()).getRuleFlowGroup();
                if ("DROOLS_SYSTEM".equals(ruleFlowGroup)) {
                    // new activations of the rule associate with a state node
                    // signal process instances of that state node
                    String ruleName = event.getMatch().getRule().getName();
                    if (ruleName.startsWith("RuleFlowStateNode-")) {
                        int index = ruleName.indexOf('-',
                                                     18);
                        index = ruleName.indexOf('-',
                                                 index + 1);
                        String eventType = ruleName.substring(0,
                                                              index);

                        kruntime.queueWorkingMemoryAction(new SignalManagerSignalAction(eventType, event));
                    } else if (ruleName.startsWith("RuleFlowStateEventSubProcess-")
                            || ruleName.startsWith("RuleFlowStateEvent-")
                            || ruleName.startsWith("RuleFlow-Milestone-")
                            || ruleName.startsWith("RuleFlow-AdHocComplete-")
                            || ruleName.startsWith("RuleFlow-AdHocActivate-")) {
                        kruntime.queueWorkingMemoryAction(new SignalManagerSignalAction(ruleName, event));
                    }
                } else {
                    String ruleName = event.getMatch().getRule().getName();
                    if (ruleName.startsWith("RuleFlow-Start-")) {
                        String processId = ruleName.replace("RuleFlow-Start-", "");

                        startProcessWithParamsAndTrigger(processId, null, "conditional", true);
                    }
                }
            }
        });

        kruntime.addEventListener(new DefaultAgendaEventListener() {
            @Override
            public void afterRuleFlowGroupDeactivated(final RuleFlowGroupDeactivatedEvent event) {
                if (kruntime instanceof StatefulKnowledgeSession) {
                    signalManager.signalEvent("RuleFlowGroup_" + event.getRuleFlowGroup().getName() + "_" + ((StatefulKnowledgeSession) kruntime).getIdentifier(),
                                              null);
                } else {
                    signalManager.signalEvent("RuleFlowGroup_" + event.getRuleFlowGroup().getName(), null);
                }
            }
        });
    }

    private void startProcessWithParamsAndTrigger(String processId, Map<String, Object> params, String type, boolean dispose) {

        startProcess(processId, params, type);
    }

    @Override
    public void abortProcessInstance(String processInstanceId) {
        ProcessInstance processInstance = getProcessInstance(processInstanceId);
        if (processInstance == null) {
            throw new IllegalArgumentException("Could not find process instance for id " + processInstanceId);
        }
        ((org.jbpm.process.instance.ProcessInstance) processInstance).setState(ProcessInstance.STATE_ABORTED);
    }

    @Override
    public WorkItemManager getWorkItemManager() {
        return kruntime.getWorkItemManager();
    }

    @Override
    public UnitOfWorkManager getUnitOfWorkManager() {
        return unitOfWorkManager;
    }

    @Override
    public void signalEvent(String type, Object event) {
        signalManager.signalEvent(type, event);
    }

    @Override
    public void signalEvent(String type, Object event, String processInstanceId) {
        signalManager.signalEvent(processInstanceId, type, event);
    }

    @Override
    public void dispose() {
        this.processEventSupport.reset();
        kruntime = null;
    }

    @Override
    public void clearProcessInstances() {
        this.processInstanceManager.clearProcessInstances();
    }

    @Override
    public void clearProcessInstancesState() {
        this.processInstanceManager.clearProcessInstancesState();
    }

    public boolean isActive() {
        Boolean active = (Boolean) kruntime.getEnvironment().get("Active");
        if (active == null) {
            return true;
        }

        return active.booleanValue();
    }

    protected ExpirationTime createTimerInstance(Timer timer, InternalKnowledgeRuntime kruntime) {
        if (kruntime != null && kruntime.getEnvironment().get("jbpm.business.calendar") != null) {
            BusinessCalendar businessCalendar = (BusinessCalendar) kruntime.getEnvironment().get("jbpm.business.calendar");

            long delay = businessCalendar.calculateBusinessTimeAsDuration(timer.getDelay());

            if (timer.getPeriod() == null) {
                return DurationExpirationTime.repeat(delay);
            } else {
                long period = businessCalendar.calculateBusinessTimeAsDuration(timer.getPeriod());

                return DurationExpirationTime.repeat(delay, period);
            }
        } else {
            return configureTimerInstance(timer);
        }
    }

    private ExpirationTime configureTimerInstance(Timer timer) {
        long duration = -1;
        switch (timer.getTimeType()) {
            case Timer.TIME_CYCLE:
                // when using ISO date/time period is not set
                long[] repeatValues = DateTimeUtils.parseRepeatableDateTime(timer.getDelay());
                if (repeatValues.length == 3) {
                    int parsedReapedCount = (int) repeatValues[0];

                    return DurationExpirationTime.repeat(repeatValues[1], repeatValues[2], parsedReapedCount);
                } else {
                    long delay = repeatValues[0];
                    long period = -1;
                    try {
                        period = TimeUtils.parseTimeString(timer.getPeriod());
                    } catch (RuntimeException e) {
                        period = repeatValues[0];
                    }

                    return DurationExpirationTime.repeat(delay, period);
                }

            case Timer.TIME_DURATION:

                duration = DateTimeUtils.parseDuration(timer.getDelay());
                return DurationExpirationTime.after(duration);

            case Timer.TIME_DATE:

                return ExactExpirationTime.of(timer.getDate());

            default:
                throw new UnsupportedOperationException("Not supported timer definition");
        }
    }

    @Override
    public InternalKnowledgeRuntime getInternalKieRuntime() {
        return this.kruntime;
    }

    private class StartProcessEventListener implements EventListener {

        private String processId;
        private List<EventFilter> eventFilters;
        private Map<String, String> inMappings;
        private EventTransformer eventTransformer;

        public StartProcessEventListener(String processId,
                                         List<EventFilter> eventFilters,
                                         Map<String, String> inMappings,
                                         EventTransformer eventTransformer) {
            this.processId = processId;
            this.eventFilters = eventFilters;
            this.inMappings = inMappings;
            this.eventTransformer = eventTransformer;
        }

        @Override
        public String[] getEventTypes() {
            return null;
        }

        @Override
        public void signalEvent(final String type,
                                Object event) {
            for (EventFilter filter : eventFilters) {
                if (!filter.acceptsEvent(type,
                                         event)) {
                    return;
                }
            }
            if (eventTransformer != null) {
                event = eventTransformer.transformEvent(event);
            }
            Map<String, Object> params = null;
            if (inMappings != null && !inMappings.isEmpty()) {
                params = new HashMap<String, Object>();

                if (inMappings.size() == 1) {
                    params.put(inMappings.keySet().iterator().next(), event);
                } else {
                    for (Map.Entry<String, String> entry : inMappings.entrySet()) {
                        if ("event".equals(entry.getValue())) {
                            params.put(entry.getKey(),
                                       event);
                        } else {
                            params.put(entry.getKey(),
                                       entry.getValue());
                        }
                    }
                }
            }
            startProcessWithParamsAndTrigger(processId, params, type, false);
        }
    }

  
    public class SignalManagerSignalAction extends PropagationEntry.AbstractPropagationEntry implements WorkingMemoryAction {

        private String type;
        private Object event;

        public SignalManagerSignalAction(String type, Object event) {
            this.type = type;
            this.event = event;
        }

        public SignalManagerSignalAction(MarshallerReaderContext context) throws IOException, ClassNotFoundException {
            type = context.readUTF();
            if (context.readBoolean()) {
                event = context.readObject();
            }
        }

        @Override
        public void execute(InternalWorkingMemory workingMemory) {

            signalEvent(type, event);
        }

        public void execute(InternalKnowledgeRuntime kruntime) {
            signalEvent(type, event);
        }

        public Action serialize(MarshallerWriteContext context) throws IOException {
            return null;
        }
    }
}
