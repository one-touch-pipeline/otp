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

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.qcTrafficLight.QcTrafficLightService
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.CreateFileHelper

import java.nio.file.Path

@Rollback
@Integration
class ParseAceseqQcJobSpec extends Specification {

    TestConfigService configService

    @TempDir
    Path tempDir

    void "test execute"() {
        given:
        Path aceseqOutputFile = tempDir.resolve("aceseqOutputFile.txt")
        CreateFileHelper.createFile(aceseqOutputFile)

        Path qcJson = tempDir.resolve("qc_json_file")

        AceseqInstance instance = DomainFactory.createAceseqInstanceWithRoddyBamFiles()

        configService.addOtpProperty(OtpProperty.PATH_PROJECT_ROOT, tempDir.toString())

        DomainFactory.createAceseqQaFileOnFileSystem(qcJson)

        ParseAceseqQcJob job = [
                getProcessParameterObject: { -> instance },
        ] as ParseAceseqQcJob
        job.qcTrafficLightService = new QcTrafficLightService()
        job.aceseqService = Mock(AceseqService) {
            getQcJsonFile(_) >> qcJson
            getAllFiles(_) >> [aceseqOutputFile]
        }

        when:
        job.execute()

        then:
        AceseqQc.findAllByAceseqInstance(instance).size() == 2
        TestCase.assertContainSame(AceseqQc.findAllByAceseqInstance(instance)*.number, [1, 2])
        def qc1 = CollectionUtils.atMostOneElement(AceseqQc.findAllByAceseqInstanceAndNumber(instance, 1))
        qc1.tcc == 0.5d
        qc1.ploidyFactor == "2.27"
        qc1.ploidy == 2
        qc1.goodnessOfFit == 0.904231625835189d
        qc1.gender == "male"
        qc1.solutionPossible == 3
        def qc2 = CollectionUtils.atMostOneElement(AceseqQc.findAllByAceseqInstanceAndNumber(instance, 2))
        qc2.tcc == 0.7d
        qc2.ploidyFactor == "1.27"
        qc2.ploidy == 5
        qc2.goodnessOfFit == 0.12345d
        qc2.gender == "female"
        qc2.solutionPossible == 4

        instance.processingState == AnalysisProcessingStates.FINISHED

        cleanup:
        configService.clean()
    }
}
