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
package de.dkfz.tbi.otp.qcTrafficLight

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.CommentService
import de.dkfz.tbi.otp.dataprocessing.LinkFilesToFinalDestinationService
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerQualityAssessment
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.domainFactory.pipelines.AlignmentPipelineFactory
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.QcThresholdHandling
import de.dkfz.tbi.otp.security.SecurityService
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.utils.CollectionUtils

import static de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile.QcTrafficLightStatus.*

@Rollback
@Integration
class QcTrafficLightServiceIntegrationSpec extends Specification implements DomainFactoryCore {

    QcTrafficLightService qcTrafficLightService

    SingleCellBamFile bamFile
    CellRangerQualityAssessment cellRangerQualityAssessment

    void setupData() {
        qcTrafficLightService = new QcTrafficLightService()
        qcTrafficLightService.commentService = new CommentService()
        qcTrafficLightService.commentService.securityService = Mock(SecurityService) {
            getCurrentUser() >> { new User(username: "dummy") }
        }
        qcTrafficLightService.linkFilesToFinalDestinationService = new LinkFilesToFinalDestinationService()
        qcTrafficLightService.qcThresholdService = new QcThresholdService()

        bamFile = AlignmentPipelineFactory.CellRangerFactoryInstance.INSTANCE.createBamFile()
        ["totalReadCounter", "qcBasesMapped", "allBasesMapped", "onTargetMappedBases"].each { String property ->
            DomainFactory.createQcThreshold(
                    qcProperty1: property,
                    seqType: bamFile.seqType,
                    qcClass: CellRangerQualityAssessment.name,
                    errorThresholdLower: 2,
                    warningThresholdLower: 4,
                    warningThresholdUpper: 6,
                    errorThresholdUpper: 8,
            )
        }
        //
        cellRangerQualityAssessment = AlignmentPipelineFactory.CellRangerFactoryInstance.INSTANCE.createQa(bamFile, [
                totalReadCounter: 5, allBasesMapped: 5,
        ])
    }

    @Unroll
    void "setQcTrafficLightStatusBasedOnThresholdAndProjectSpecificHandling (qcBasesMapped = #qcBasesMapped & onTargetMappedBases = #onTargetMappedBases --> #resultStatus)"() {
        given:
        setupData()

        cellRangerQualityAssessment.qcBasesMapped = qcBasesMapped
        cellRangerQualityAssessment.onTargetMappedBases = onTargetMappedBases

        when:
        qcTrafficLightService.setQcTrafficLightStatusBasedOnThresholdAndProjectSpecificHandling(bamFile, cellRangerQualityAssessment)

        then:
        bamFile.qcTrafficLightStatus == resultStatus

        where:
        qcBasesMapped | onTargetMappedBases || resultStatus
        5             | 5                   || QC_PASSED
        7             | 5                   || QC_PASSED
        7             | 7                   || QC_PASSED
        9             | 5                   || WARNING
        9             | 9                   || WARNING
    }

    @Unroll
    void "setQcTrafficLightStatusBasedOnThresholdAndProjectSpecificHandling, use project specific (qcBasesMapped = #qcBasesMapped --> #resultStatus)"() {
        given:
        setupData()

        cellRangerQualityAssessment.qcBasesMapped = qcBasesMapped
        cellRangerQualityAssessment.onTargetMappedBases = 5

        DomainFactory.createQcThreshold(
                qcProperty1: 'qcBasesMapped',
                seqType: bamFile.seqType,
                qcClass: CellRangerQualityAssessment.name,
                errorThresholdLower: 12,
                warningThresholdLower: 14,
                warningThresholdUpper: 16,
                errorThresholdUpper: 18,
                project: bamFile.project,
        )
        when:
        qcTrafficLightService.setQcTrafficLightStatusBasedOnThresholdAndProjectSpecificHandling(bamFile, cellRangerQualityAssessment)

        then:
        bamFile.qcTrafficLightStatus == resultStatus

        where:
        qcBasesMapped || resultStatus
        3             || WARNING
        5             || WARNING
        7             || WARNING
        11            || WARNING
        13            || QC_PASSED
        15            || QC_PASSED
        17            || QC_PASSED
        19            || WARNING
    }

    @Unroll
    void "setQcTrafficLightStatusBasedOnThresholdAndProjectSpecificHandling, do not use project specific of other project (qcBasesMapped = #qcBasesMapped --> #resultStatus)"() {
        given:
        setupData()

        cellRangerQualityAssessment.qcBasesMapped = qcBasesMapped
        cellRangerQualityAssessment.onTargetMappedBases = 5

        QcThreshold qcThreshold = CollectionUtils.atMostOneElement(QcThreshold.findAllByQcProperty1('qcBasesMapped'))
        qcThreshold.project = createProject()
        qcThreshold.save(flush: true)

        when:
        qcTrafficLightService.setQcTrafficLightStatusBasedOnThresholdAndProjectSpecificHandling(bamFile, cellRangerQualityAssessment)

        then:
        bamFile.qcTrafficLightStatus == resultStatus

        where:
        qcBasesMapped || resultStatus
        0             || QC_PASSED
        5             || QC_PASSED
        9             || QC_PASSED
    }

    @Unroll
    void "setQcTrafficLightStatusBasedOnThresholdAndProjectSpecificHandling, once blocked files do not get unblocked (qcBasesMapped = #qcBasesMapped & onTargetMappedBases = #onTargetMappedBases --> #exceedingExpected)"() {
        given:
        setupData()

        cellRangerQualityAssessment.qcBasesMapped = qcBasesMapped
        cellRangerQualityAssessment.onTargetMappedBases = onTargetMappedBases

        when:
        bamFile.comment = DomainFactory.createComment()
        bamFile.qcTrafficLightStatus = BLOCKED
        qcTrafficLightService.setQcTrafficLightStatusBasedOnThresholdAndProjectSpecificHandling(bamFile, cellRangerQualityAssessment)
        boolean thresholdExceeded = qcTrafficLightService.qcValuesExceedErrorThreshold(bamFile, cellRangerQualityAssessment)

        then:
        thresholdExceeded == exceedingExpected
        bamFile.qcTrafficLightStatus == BLOCKED

        where:
        qcBasesMapped | onTargetMappedBases || exceedingExpected
        5             | 5                   || false
        9             | 9                   || true
    }

    void "setQcTrafficLightStatusBasedOnThresholdAndProjectSpecificHandling, NO_CHECK causes UNCHECKED regardless of thresholds"() {
        given:
        setupData()

        bamFile.individual.project = createProject(qcThresholdHandling: QcThresholdHandling.NO_CHECK)

        cellRangerQualityAssessment.qcBasesMapped = qcBasesMapped
        cellRangerQualityAssessment.onTargetMappedBases = 5

        when:
        qcTrafficLightService.setQcTrafficLightStatusBasedOnThresholdAndProjectSpecificHandling(bamFile, cellRangerQualityAssessment)

        then:
        bamFile.qcTrafficLightStatus == UNCHECKED

        where:
        qcBasesMapped << [0, 2, 4, 5, 6, 8, 10]
    }

    @Unroll
    void "setQcTrafficLightStatusBasedOnThresholdAndProjectSpecificHandling, CHECK_AND_NOTIFY only causes AUTO_ACCEPTED if the threshold would fail"() {
        given:
        setupData()

        bamFile.individual.project = createProject(qcThresholdHandling: QcThresholdHandling.CHECK_AND_NOTIFY)

        cellRangerQualityAssessment.qcBasesMapped = qcBasesMapped
        cellRangerQualityAssessment.onTargetMappedBases = 5

        when:
        qcTrafficLightService.setQcTrafficLightStatusBasedOnThresholdAndProjectSpecificHandling(bamFile, cellRangerQualityAssessment)

        then:
        bamFile.qcTrafficLightStatus == expectedStatus

        where:
        qcBasesMapped || expectedStatus
        0             || WARNING
        3             || QC_PASSED
        5             || QC_PASSED
        7             || QC_PASSED
        10            || WARNING
    }
}
