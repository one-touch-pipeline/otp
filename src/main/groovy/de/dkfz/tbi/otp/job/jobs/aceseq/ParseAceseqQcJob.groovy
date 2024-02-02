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
package de.dkfz.tbi.otp.job.jobs.aceseq

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
class ParseAceseqQcJob extends AbstractEndStateAwareJobImpl implements AutoRestartableJob {

    @Autowired
    AceseqService aceseqService

    @Autowired
    QcTrafficLightService qcTrafficLightService

    @Override
    void execute() throws Exception {
        final AceseqInstance aceseqInstance = processParameterObject

        Path qcFile = aceseqService.getQcJsonFile(aceseqInstance)
        JSONObject qcJson = JSON.parse(qcFile.text)
        AceseqQc.withTransaction {
            AceseqQc qcOne = qcJson.collect { String number, Map values ->
                AceseqQc qc = new AceseqQc(values)
                qc.number = Integer.parseInt(number)
                qc.aceseqInstance = aceseqInstance
                assert qc.save(flush: true)
                return qc
            }.find {
                it.number == 1
            }

            qcOne // the instance to used for qc

            List<Path> files = aceseqService.getAllFiles(aceseqInstance)
            files.each {
                FileService.ensureFileIsReadableAndNotEmptyStatic(it)
            }

            aceseqInstance.processingState = AnalysisProcessingStates.FINISHED
            assert aceseqInstance.save(flush: true)
            succeed()
        }
    }
}
