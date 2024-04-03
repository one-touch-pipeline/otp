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
package de.dkfz.tbi.otp.workflow.analysis.sophia

import grails.converters.JSON
import groovy.transform.CompileDynamic
import groovy.util.logging.Slf4j
import org.grails.web.json.JSONObject
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaQc
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaWorkFileService
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.workflow.jobs.AbstractParseJob
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path

@Component
@Slf4j
class SophiaParseJob extends AbstractParseJob implements SophiaWorkflowShared {

    @Autowired
    SophiaWorkFileService sophiaWorkFileService

    @Override
    @CompileDynamic
    void parseOutputs(WorkflowStep workflowStep) {
        SophiaInstance instance = getSophiaInstance(workflowStep)
        Path qcFile = sophiaWorkFileService.getQcJsonFile(instance)
        JSONObject qcJson = JSON.parse(qcFile.text) as JSONObject
        Map<String, String> qcValues = qcJson.values()[0] as Map<String, String>

        SophiaQc sophiaQc = CollectionUtils.atMostOneElement(SophiaQc.findAllBySophiaInstance(instance))
        if (sophiaQc) {
            qcValues.each { key, value ->
                sophiaQc.setProperty(key, value)
            }
        } else {
            sophiaQc = qcValues as SophiaQc
            sophiaQc.sophiaInstance = instance
        }
        sophiaQc.save(flush: true)
    }
}
