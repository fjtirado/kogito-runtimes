package org.jbpm.process.instance;


public class ProcessInstanceHolder {
    
    private static ThreadLocal<ProcessInstance> processInstance = new ThreadLocal<>();
    
    public static void set(ProcessInstance instance) {
        processInstance.set(instance);
    }
    
    public static ProcessInstance get() {
        return processInstance.get();
    }
    
    public static void clear() {
        processInstance.remove();
    }
}
