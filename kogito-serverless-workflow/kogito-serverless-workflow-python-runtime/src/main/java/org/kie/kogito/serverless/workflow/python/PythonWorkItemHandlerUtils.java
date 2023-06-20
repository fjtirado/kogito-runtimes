/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates.
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
package org.kie.kogito.serverless.workflow.python;

import jep.Interpreter;
import jep.SharedInterpreter;

public class PythonWorkItemHandlerUtils {

    private PythonWorkItemHandlerUtils() {
    }

    private static ThreadLocal<Interpreter> interpreter = new ThreadLocal<>();

    protected static Interpreter interpreter() {
        Interpreter py = interpreter.get();
        if (py == null) {
            py = new SharedInterpreter();
            interpreter.set(py);
        }
        return py;
    }

    protected static void closeInterpreter() {
        Interpreter py = interpreter.get();
        if (py != null) {
            interpreter.remove();
            py.close();
        }
    }

    protected static Object getValue(String key) {
        return interpreter().getValue(key);
    }

}
