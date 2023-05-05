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
package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification
import spock.lang.TempDir
import spock.lang.Unroll

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.qcTrafficLight.QcThreshold
import de.dkfz.tbi.otp.qcTrafficLight.QcThresholdService
import de.dkfz.tbi.otp.qcTrafficLight.QcTrafficLightService
import de.dkfz.tbi.otp.utils.CollectionUtils

import java.nio.file.Path

@Rollback
@Integration
class ParsePanCanQcJobIntegrationSpec extends Specification {

    @Autowired
    AbstractQualityAssessmentService abstractQualityAssessmentService

    @TempDir
    Path tempDir

    @Unroll
    void "test execute ParsePanCanQcJob (percentageMatesOnDifferentChr: #percentageMatesOnDifferentChr"() {
        given:
        File qaFile = tempDir.resolve(RoddyBamFile.QUALITY_CONTROL_JSON_FILE_NAME).toFile()
        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile()
        SeqTrack seqTrack = CollectionUtils.exactlyOneElement(roddyBamFile.seqTracks)
        roddyBamFile.metaClass.getWorkMergedQAJsonFile = { -> qaFile }
        roddyBamFile.metaClass.getWorkSingleLaneQAJsonFiles = { -> [(seqTrack): qaFile] }

        ReferenceGenome referenceGenome = DomainFactory.createReferenceGenome()
        DomainFactory.createReferenceGenomeEntries(referenceGenome, ['7', '8'])
        DomainFactory.createQaFileOnFileSystem(qaFile, [percentageMatesOnDifferentChr: percentageMatesOnDifferentChr])

        ParsePanCanQcJob job = Spy(ParsePanCanQcJob)
        job.abstractQualityAssessmentService = abstractQualityAssessmentService
        job.qcTrafficLightService = new QcTrafficLightService()
        job.qcTrafficLightService.qcThresholdService = Mock(QcThresholdService) {
            getThresholds(_, _, _) >> [new QcThreshold()]
        }

        when:
        job.execute()

        then:
        CollectionUtils.containSame(["8", "all", "7"], RoddySingleLaneQa.list()*.chromosome)
        CollectionUtils.containSame(["8", "all", "7"], RoddyMergedBamQa.list()*.chromosome)
        roddyBamFile.coverage != null
        roddyBamFile.coverageWithN != null
        roddyBamFile.qualityAssessmentStatus == AbstractBamFile.QaProcessingStatus.FINISHED
        roddyBamFile.qcTrafficLightStatus == AbstractBamFile.QcTrafficLightStatus.QC_PASSED
        roddyBamFile.qcTrafficLightStatus == AbstractBamFile.QcTrafficLightStatus.QC_PASSED

        and:
        1 * job.processParameterObject >> roddyBamFile

        where:
        percentageMatesOnDifferentChr << [
                '123456',
                'NA',
        ]
    }
}
