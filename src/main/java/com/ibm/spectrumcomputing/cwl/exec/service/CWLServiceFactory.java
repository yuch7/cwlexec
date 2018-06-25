/*
 * Copyright International Business Machines Corp, 2018.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ibm.spectrumcomputing.cwl.exec.service;

import java.util.HashMap;
import java.util.Map;

import com.ibm.spectrumcomputing.cwl.exec.util.DatabaseManager;
import com.ibm.spectrumcomputing.cwl.model.RuntimeEnv;

/**
 * Factory for {@link CWLRuntimeService}, {@link CWLInstanceService} and
 * {@link CWLExecService}
 */
public final class CWLServiceFactory {

    private CWLServiceFactory() {
    }

    private static final Map<String, Object> services = new HashMap<>();

    /**
     * Creates {@link CWLRuntimeService}, {@link CWLInstanceService},
     * {@link CWLExecService} and {@link CWLLSFCommandServiceImpl} and hold them
     * to a {@link Map}
     * 
     * @param databaseMgr
     *            The database manager, {@link CWLInstanceService} will use it
     *            to operate database
     */
    public static void init(DatabaseManager databaseMgr) {
        CWLRuntimeService runtimeService = new CWLRuntimeService();
        CWLInstanceService instacneService = new CWLInstanceService(runtimeService, databaseMgr);
        // runtime services
        services.put(CWLRuntimeService.class.getName(), runtimeService);
        services.put(CWLInstanceService.class.getName(), instacneService);
        services.put(CWLExecService.class.getName(), new CWLExecService(instacneService));
        // command services
        services.put(RuntimeEnv.LSF.toString(), new CWLLSFCommandServiceImpl());
    }

    /**
     * Gets a service object by the service class
     * 
     * @param <T> clazz
     *            The service class
     * @return The service object
     */
    @SuppressWarnings("unchecked")
    public static <T> T getService(Class<T> clazz) {
        return (T) services.get(clazz.getName());
    }

    /*
     * Gets a command service object by runtime environment
     */
    protected static CWLCommandService getCommandService(RuntimeEnv runtimeEnv) {
        return (CWLCommandService) services.get(runtimeEnv.toString());
    }
}
