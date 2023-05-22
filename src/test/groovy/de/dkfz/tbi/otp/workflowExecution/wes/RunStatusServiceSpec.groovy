/*
 * Copyright 2011-2023 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.workflowExecution.wes

import io.swagger.client.wes.model.RunStatus
import io.swagger.client.wes.model.State
import spock.lang.Specification
import spock.lang.Unroll

class RunStatusServiceSpec extends Specification {

    @Unroll
    void "isInEndState, if state is #state, then result is #endState"() {
        given:
        RunStatusService service = new RunStatusService()
        RunStatus runStatus = new RunStatus([
                state: state,
        ])

        expect:
        service.isInEndState(runStatus) == endState

        where:
        state                | endState
        State.UNKNOWN        | false
        State.QUEUED         | false
        State.INITIALIZING   | false
        State.RUNNING        | false
        State.PAUSED         | false
        State.COMPLETE       | true
        State.EXECUTOR_ERROR | true
        State.SYSTEM_ERROR   | true
        State.CANCELED       | true
        State.CANCELING      | false
    }
}
