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
package de.dkfz.tbi.otp.workflow.analysis.indel

import grails.converters.JSON
import groovy.transform.CompileDynamic
import groovy.util.logging.Slf4j
import org.grails.web.json.JSONObject
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.indelcalling.IndelCallingInstance
import de.dkfz.tbi.otp.dataprocessing.indelcalling.IndelQualityControl
import de.dkfz.tbi.otp.dataprocessing.indelcalling.IndelSampleSwapDetection
import de.dkfz.tbi.otp.dataprocessing.indelcalling.IndelWorkFileService
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.workflow.jobs.AbstractParseJob
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path

@Component
@Slf4j
class IndelParseJob extends AbstractParseJob implements IndelWorkflowShared {

    @Autowired
    IndelWorkFileService indelWorkFileService

    @Override
    void parseOutputs(WorkflowStep workflowStep) {
        IndelCallingInstance instance = getIndelInstance(workflowStep)

        parseIndelQcJson(instance)
        parseSampleSwapJson(instance)
    }

    @CompileDynamic
    private void parseIndelQcJson(IndelCallingInstance instance) {
        Path indelQcFile = indelWorkFileService.getIndelQcJsonFile(instance)

        JSONObject qcJson = JSON.parse(indelQcFile.text) as JSONObject

        Map<String, String> qcValues = qcJson.values()[0] as Map<String, String>
        qcValues.file = qcValues.file.replace('./', '')

        IndelQualityControl indelQc = CollectionUtils.atMostOneElement(IndelQualityControl.findAllByIndelCallingInstance(instance))
        if (indelQc) {
            qcValues.each { key, value ->
                indelQc.setProperty(key, value)
            }
        } else {
            indelQc = qcValues as IndelQualityControl
            indelQc.indelCallingInstance = instance
        }
        indelQc.save(flush: true)
    }

    @CompileDynamic
    private void parseSampleSwapJson(IndelCallingInstance instance) {
        Path sampleSwapFile = indelWorkFileService.getSampleSwapJsonFile(instance)
        Map<String, String> sampleSwapJson = JSON.parse(sampleSwapFile.text) as Map<String, String>

        IndelSampleSwapDetection sampleSwap = CollectionUtils.atMostOneElement(IndelSampleSwapDetection.findAllByIndelCallingInstance(instance))
        if (sampleSwap) {
            sampleSwapJson.each { key, value ->
                sampleSwap.setProperty(key, value)
            }
        } else {
            sampleSwap = sampleSwapJson as IndelSampleSwapDetection
            sampleSwap.indelCallingInstance = instance
        }
        sampleSwap.save(flush: true)
    }
}
