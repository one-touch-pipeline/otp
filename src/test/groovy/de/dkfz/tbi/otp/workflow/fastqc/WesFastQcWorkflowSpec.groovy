/*
 * Copyright 2011-2024 The OTP authors
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
package de.dkfz.tbi.otp.workflow.fastqc

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.workflow.jobs.*
import de.dkfz.tbi.otp.workflowExecution.Artefact

class WesFastQcWorkflowSpec extends Specification implements DataTest, DomainFactoryCore {

    WesFastQcWorkflow wesFastQcWorkflow

    @Override
    Class[] getDomainClassesToMock() {
        return [
                SeqTrack
        ]
    }

    void setup() {
        wesFastQcWorkflow = new WesFastQcWorkflow()
    }

    void "getJobList, should return all WesFastQcJob bean names in correct order"() {
        expect:
        wesFastQcWorkflow.jobList == [
                FastqcFragmentJob,
                FastqcConditionalFailJob,
                AttachUuidJob,
                FastqcWesPrepareJob,
                FastqcExecuteWesPipelineJob,
                FastqcWesValidationJob,
                SetCorrectPermissionJob,
                FastqcParseJob,
                FastqcLinkJob,
                CalculateSizeJob,
                FastqcFinishJob,
        ]
    }

    void "createCopyOfArtefact, should only delegate the concrete artefact"() {
        given:
        Artefact artefact = createSeqTrack()

        expect:
        wesFastQcWorkflow.createCopyOfArtefact(artefact) == artefact
    }
}
