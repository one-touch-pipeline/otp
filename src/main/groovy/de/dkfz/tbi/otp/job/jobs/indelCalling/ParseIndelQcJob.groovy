/*
 * Copyright 2011-2019 The OTP authors
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
package de.dkfz.tbi.otp.job.jobs.indelCalling

import grails.converters.JSON
import groovy.transform.CompileDynamic
import groovy.util.logging.Slf4j
import org.grails.web.json.JSONObject
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.jobs.AutoRestartableJob
import de.dkfz.tbi.otp.job.processing.AbstractEndStateAwareJobImpl
import de.dkfz.tbi.otp.qcTrafficLight.QcTrafficLightService

import java.nio.file.Path

@CompileDynamic
@Component
@Scope("prototype")
@Slf4j
class ParseIndelQcJob extends AbstractEndStateAwareJobImpl implements AutoRestartableJob {

    @Autowired
    IndelCallingService indelCallingService

    @Autowired
    QcTrafficLightService qcTrafficLightService

    @SuppressWarnings('JavaIoPackageAccess')
    @Override
    void execute() throws Exception {
        final IndelCallingInstance instance = processParameterObject

        Path indelQcFile = indelCallingService.getIndelQcJsonFile(instance)
        FileService.ensureFileIsReadableAndNotEmpty(indelQcFile)
        Path sampleSwapFile = indelCallingService.getSampleSwapJsonFile(instance)
        FileService.ensureFileIsReadableAndNotEmpty(sampleSwapFile)
        JSONObject qcJson = JSON.parse(indelQcFile.text)
        JSONObject sampleSwapJson = JSON.parse(sampleSwapFile.text)
        IndelCallingInstance.withTransaction {
            IndelQualityControl indelQc = qcJson.values()
            indelQc.file = new File(indelQc.file.replace('./', '')).path
            indelQc.indelCallingInstance = instance
            assert indelQc.save(flush: true)

            IndelSampleSwapDetection sampleSwap = sampleSwapJson
            sampleSwap.indelCallingInstance = instance
            assert sampleSwap.save(flush: true)

            instance.processingState = AnalysisProcessingStates.FINISHED
            instance.save(flush: true)

            succeed()
        }
    }
}
