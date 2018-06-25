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
package com.ibm.spectrumcomputing.cwl.exec.executor.lsf;

/*
 * The event is used to drive a Workflow to run.
 * - If a unterminated step is done, the START event will be send.
 * - If a terminated step is done, the DONE event will be send.
 * - If a step is exit, the EXIT event will be send.
 */
final class LSFJobEvent {

    private final LSFJobEventType type;
    private final String instanceId;
    private final String instanceName;

    private int exitCode;

    protected LSFJobEvent(LSFJobEventType type, String instanceId, String instanceName) {
        this.type = type;
        this.instanceId = instanceId;
        this.instanceName = instanceName;
    }

    protected LSFJobEventType getType() {
        return type;
    }

    protected String getInstanceId() {
        return instanceId;
    }

    protected String getInstanceName() {
        return instanceName;
    }

    protected int getExitCode() {
        return exitCode;
    }

    protected void setExitCode(int exitCode) {
        this.exitCode = exitCode;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((instanceId == null) ? 0 : instanceId.hashCode());
        result = prime * result + ((instanceName == null) ? 0 : instanceName.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        LSFJobEvent other = (LSFJobEvent) obj;
        if (instanceId == null) {
            if (other.instanceId != null)
                return false;
        } else if (!instanceId.equals(other.instanceId))
            return false;
        if (instanceName == null) {
            if (other.instanceName != null)
                return false;
        } else if (!instanceName.equals(other.instanceName))
            return false;
        return type != other.type;
    }
}
