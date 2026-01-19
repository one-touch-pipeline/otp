/*
 * Copyright 2011-2026 The OTP authors
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
package de.dkfz.tbi.otp.workflowExecution.decider.analysis

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.BamFilePairAnalysis
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.dataprocessing.indelcalling.IndelCallingInstance
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaInstance
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaWorkFileService
import de.dkfz.tbi.otp.dataprocessing.snvcalling.RoddySnvCallingInstance
import de.dkfz.tbi.otp.workflow.analysis.runyapsa.RunYapsaWorkflow
import de.dkfz.tbi.otp.workflowExecution.ArtefactType

@Component
@Transactional
@Slf4j
class RunYapsaDecider extends AbstractAnalysisDecider<RunYapsaInstance> {

    @Autowired
    RunYapsaWorkFileService runYapsaWorkFileService

    @Override
    RunYapsaWorkFileService getWorkFileService() {
        return runYapsaWorkFileService
    }

    final String workflowName = RunYapsaWorkflow.WORKFLOW

    final List<Class<RunYapsaInstance>> instanceClasses = [RunYapsaInstance].asImmutable()

    final Map<String, List<Class<? extends BamFilePairAnalysis>>> dependingAnalysisInstanceClasses = [
            (RunYapsaWorkflow.SNV_INPUT)  : [RoddySnvCallingInstance].asImmutable(),
            (RunYapsaWorkflow.INDEL_INPUT): [IndelCallingInstance].asImmutable(),
    ].asImmutable()

    final ArtefactType artefactType = ArtefactType.RUN_YAPSA

    final Pipeline.Name pipelineName = Pipeline.Name.RUN_YAPSA

    @Override
    BamFilePairAnalysis createAnalysisWithoutFlush(Map properties) {
        return new RunYapsaInstance(properties).save(flush: false, deepValidate: false)
    }
}
