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
package de.dkfz.tbi.otp.workflow.analysis.aceseq

import grails.converters.JSON
import groovy.transform.CompileDynamic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.aceseq.AceseqInstance
import de.dkfz.tbi.otp.dataprocessing.aceseq.AceseqQc
import de.dkfz.tbi.otp.dataprocessing.aceseq.AceseqWorkFileService
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.workflow.jobs.AbstractParseJob
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path

@Component
@Slf4j
class AceseqParseJob extends AbstractParseJob implements AceseqWorkflowShared {

    @Autowired
    AceseqWorkFileService aceseqWorkFileService

    @Override
    @CompileDynamic
    void parseOutputs(WorkflowStep workflowStep) {
        AceseqInstance aceseqInstance = getAceseqInstance(workflowStep)

        Path qcFile = aceseqWorkFileService.getQcJsonFile(aceseqInstance)
        Map<String, Map> qcJson = JSON.parse(qcFile.text) as Map<String, Map>

        qcJson.collect { String number, Map<String, String> values ->
            Map<String, Object> aceseqValues = values
            aceseqValues.number = Integer.parseInt(number)
            aceseqValues.solutionPossible = Double.parseDouble(values.solutionPossible)
            aceseqValues.tcc = Double.parseDouble(values.tcc)
            aceseqValues.goodnessOfFit = Double.parseDouble(values.goodnessOfFit)
            return aceseqValues as Map<String, Object>
        }.collect {
            createOrUpdateQc(aceseqInstance, it)
        }
    }

    @CompileDynamic
    private void createOrUpdateQc(AceseqInstance aceseqInstance, Map<String, Object> properties) {
        AceseqQc aceseqQc = CollectionUtils.atMostOneElement(AceseqQc.findAllByNumberAndAceseqInstance(properties.number as Integer, aceseqInstance))
        if (aceseqQc) {
            properties.each { key, value ->
                aceseqQc.setProperty(key, value)
            }
        } else {
            aceseqQc = properties as AceseqQc
            aceseqQc.aceseqInstance = aceseqInstance
        }
        aceseqQc.save(flush: true)
    }
}
