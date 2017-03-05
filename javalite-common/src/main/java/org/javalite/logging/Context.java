/*
Copyright 2009-2016 Igor Polevoy

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/


package org.javalite.logging;

import org.javalite.common.JsonHelper;
import org.slf4j.MDC;

import java.util.Map;

import static org.javalite.common.Collections.map;

/**
 * Convenience wrapper over <a href="https://www.slf4j.org/api/org/slf4j/MDC.html">org.slf4j.MDC</code>.
 * <p></p>
 * Serves mostly to pass name and values from any code down to the logging system to be included on
 * every log line by a current thread.. till cleared.
 *
 * @author igor on 2/5/17.
 */
public class Context {

    /**
     * Add multiple values in a classical JavaLite style.
     *
     * @param namesAndValues names and values (must pass even number).
     */
    public static void put(String... namesAndValues) {
        Map<String, String> context = map(namesAndValues);
        for (String key : context.keySet()) {
            MDC.put(key, context.get(key));
        }
    }

    /**
     * Clears current context of any values. Usually this is called at the end of a web request.
     */
    public static void clear() {
        MDC.clear();
    }

    /**
     * @return JSON representation of context. Expect a JSON object <code>"{...}"</code> if values are present,
     * or <code>null</code> if no values were set.
     */
    public static String toJSON(){
        Map context = MDC.getCopyOfContextMap();
        return context == null || context.isEmpty() ? null : JsonHelper.toJsonString(MDC.getCopyOfContextMap());
    }
}
