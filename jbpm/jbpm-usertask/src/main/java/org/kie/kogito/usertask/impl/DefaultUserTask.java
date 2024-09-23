/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.kie.kogito.usertask.impl;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.kie.kogito.Application;
import org.kie.kogito.uow.events.UnitOfWorkUserTaskEventListener;
import org.kie.kogito.usertask.UserTask;
import org.kie.kogito.usertask.UserTaskConfig;
import org.kie.kogito.usertask.UserTaskInstance;
import org.kie.kogito.usertask.UserTaskInstances;
import org.kie.kogito.usertask.impl.model.DeadlineHelper;
import org.kie.kogito.usertask.model.DeadlineInfo;
import org.kie.kogito.usertask.model.Reassignment;

public class DefaultUserTask implements UserTask {

    private String separator = System.getProperty("org.jbpm.ht.user.separator", ",");

    private Application application;
    private String id;
    private UserTaskInstances userTaskInstances;
    private String name;
    private String taskName;
    private String taskDescription;
    private String referenceName;
    private Integer taskPriority;
    private Boolean skippable;
    private Set<String> potentialUsers;
    private Set<String> potentialGroups;
    private Set<String> adminUsers;
    private Set<String> adminGroups;
    private Set<String> excludedUsers;
    private Collection<DeadlineInfo<Map<String, Object>>> startDeadlines;
    private Collection<DeadlineInfo<Map<String, Object>>> endDeadlines;
    private Collection<DeadlineInfo<Reassignment>> startReassigments;
    private Collection<DeadlineInfo<Reassignment>> endReassigments;

    public DefaultUserTask() {
        // nothing
    }

    public DefaultUserTask(String id, String name) {
        this(null, id, name, new InMemoryUserTaskInstances());
    }

    public DefaultUserTask(Application application, String id, String name) {
        this(application, id, name, new InMemoryUserTaskInstances());
    }

    public DefaultUserTask(Application application, String id, String name, UserTaskInstances userTaskInstances) {
        this.application = application;
        this.id = id;
        this.name = name;
        this.userTaskInstances = userTaskInstances;
        this.userTaskInstances.setReconnectUserTaskInstance(this::connect);
        this.userTaskInstances.setDisconnectUserTaskInstance(this::disconnect);
        this.skippable = Boolean.FALSE;
        this.potentialUsers = new HashSet<>();
        this.potentialGroups = new HashSet<>();
        this.adminUsers = new HashSet<>();
        this.adminGroups = new HashSet<>();
        this.excludedUsers = new HashSet<>();
        this.startDeadlines = new HashSet<>();
        this.endDeadlines = new HashSet<>();
        this.startReassigments = new HashSet<>();
        this.endReassigments = new HashSet<>();

    }

    public void setApplication(Application application) {
        this.application = application;
    }

    @Override
    public UserTaskInstance createInstance() {
        DefaultUserTaskInstance instance = new DefaultUserTaskInstance(this);
        instance.setTaskName(getTaskName());
        instance.setTaskDescription(getTaskDescription());
        instance.setTaskPriority(getTaskPriority());
        instance.setPotentialUsers(getPotentialUsers());
        instance.setPotentialGroups(getPotentialGroups());
        instance.setAdminUsers(getAdminUsers());
        instance.setPotentialGroups(getPotentialGroups());
        instance.setExcludedUsers(getExcludedUsers());
        connect(instance);
        return instance;
    }

    private UserTaskInstance disconnect(UserTaskInstance userTaskInstance) {
        DefaultUserTaskInstance instance = (DefaultUserTaskInstance) userTaskInstance;
        instance.setUserTask(null);
        instance.setUserTaskEventSupport(null);
        instance.setUserTaskLifeCycle(null);
        instance.setInstances(null);
        return instance;
    }

    public UserTaskInstance connect(UserTaskInstance userTaskInstance) {
        DefaultUserTaskInstance instance = (DefaultUserTaskInstance) userTaskInstance;
        UserTaskConfig userTaskConfig = application.config().get(UserTaskConfig.class);
        KogitoUserTaskEventSupportImpl impl = new KogitoUserTaskEventSupportImpl(userTaskConfig.identityProvider());
        userTaskConfig.userTaskEventListeners().listeners().forEach(impl::addEventListener);
        impl.addEventListener(new UnitOfWorkUserTaskEventListener(application.unitOfWorkManager()));
        instance.setUserTask(this);
        instance.setUserTaskEventSupport(impl);
        instance.setUserTaskLifeCycle(userTaskConfig.userTaskLifeCycle());
        instance.setInstances(userTaskInstances);
        return instance;
    }

    @Override
    public UserTaskInstances instances() {
        return userTaskInstances;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String name() {
        return name;
    }

    public void setSkippable(String skippable) {
        this.skippable = Boolean.parseBoolean(skippable);
    }

    public Boolean getSkippable() {
        return skippable;
    }

    @Override
    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    @Override
    public String getTaskDescription() {
        return taskDescription;
    }

    public void setTaskDescription(String taskDescription) {
        this.taskDescription = taskDescription;
    }

    @Override
    public Integer getTaskPriority() {
        return this.taskPriority;
    }

    public void setTaskPriority(Integer taskPriority) {
        this.taskPriority = taskPriority;
    }

    public void setReferenceName(String referenceName) {
        this.referenceName = referenceName;
    }

    @Override
    public String getReferenceName() {
        return referenceName;
    }

    @Override
    public Set<String> getPotentialUsers() {
        return this.potentialUsers;
    }

    public void setPotentialUsers(String potentialUsers) {
        this.setPotentialUsers(toSet(potentialUsers));
    }

    public void setPotentialUsers(Set<String> potentialUsers) {
        this.potentialUsers = potentialUsers;
    }

    @Override
    public Set<String> getPotentialGroups() {
        return this.potentialGroups;
    }

    public void setPotentialGroups(String potentialGroups) {
        this.setPotentialGroups(toSet(potentialGroups));
    }

    public void setPotentialGroups(Set<String> potentialGroups) {
        this.potentialGroups = potentialGroups;
    }

    @Override
    public Set<String> getAdminUsers() {
        return this.adminUsers;
    }

    public void setAdminUsers(String adminUsers) {
        this.setAdminUsers(toSet(adminUsers));
    }

    public void setAdminUsers(Set<String> adminUsers) {
        this.adminUsers = adminUsers;
    }

    @Override
    public Set<String> getAdminGroups() {
        return this.adminGroups;
    }

    public void setAdminGroups(String adminGroups) {
        this.setAdminGroups(toSet(adminGroups));
    }

    public void setAdminGroups(Set<String> adminGroups) {
        this.adminGroups = adminGroups;
    }

    @Override
    public Set<String> getExcludedUsers() {
        return this.excludedUsers;
    }

    public void setExcludedUsers(String excludedUsers) {
        this.setExcludedUsers(toSet(excludedUsers));
    }

    public void setExcludedUsers(Set<String> excludedUsers) {
        this.excludedUsers = excludedUsers;
    }

    @Override
    public Collection<DeadlineInfo<Map<String, Object>>> getNotStartedDeadlines() {
        return startDeadlines;
    }

    public void setNotStartedDeadLines(String deadlines) {
        this.startDeadlines = DeadlineHelper.parseDeadlines(deadlines);
    }

    @Override
    public Collection<DeadlineInfo<Map<String, Object>>> getNotCompletedDeadlines() {
        return endDeadlines;
    }

    public void setNotCompletedDeadlines(String notStarted) {
        this.endDeadlines = DeadlineHelper.parseDeadlines(notStarted);
    }

    @Override
    public Collection<DeadlineInfo<Reassignment>> getNotStartedReassignments() {
        return startReassigments;
    }

    public void setNotStartedReassignments(String reassignments) {
        this.startReassigments = DeadlineHelper.parseReassignments(reassignments);
    }

    @Override
    public Collection<DeadlineInfo<Reassignment>> getNotCompletedReassigments() {
        return endReassigments;
    }

    public void setNotCompletedReassigments(String reassignments) {
        this.endReassigments = DeadlineHelper.parseReassignments(reassignments);
    }

    protected Set<String> toSet(String value) {
        if (value == null) {
            return new HashSet<>();
        }
        Set<String> store = new HashSet<>();
        for (String item : value.split(separator)) {
            store.add(item);
        }
        return store;
    }

}