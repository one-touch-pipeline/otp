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

import grails.plugin.springsecurity.SpringSecurityService
import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.CommentService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.QcThresholdHandling
import de.dkfz.tbi.otp.utils.Principal

import static de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile.QcTrafficLightStatus.*

@Rollback
@Integration
class QcTrafficLightServiceIntegrationSpec extends Specification implements DomainFactoryCore {

    QcTrafficLightService qcTrafficLightService

    AceseqInstance instance
    AceseqQc aceseqQc

    void setupData() {
        qcTrafficLightService = new QcTrafficLightService()
        qcTrafficLightService.commentService = new CommentService()
        qcTrafficLightService.commentService.springSecurityService = Mock(SpringSecurityService) {
            getPrincipal() >> { new Principal(username: "dummy") }
        }
        qcTrafficLightService.linkFilesToFinalDestinationService = new LinkFilesToFinalDestinationService()

        instance = DomainFactory.createAceseqInstanceWithRoddyBamFiles()
        ["solutionPossible", "tcc", "goodnessOfFit", "ploidy"].each { String property ->
            DomainFactory.createQcThreshold(
                    qcProperty1: property,
                    seqType: instance.seqType,
                    qcClass: AceseqQc.name,
                    errorThresholdLower: 2,
                    warningThresholdLower: 4,
                    warningThresholdUpper: 6,
                    errorThresholdUpper: 8,
            )
        }
        aceseqQc = DomainFactory.createAceseqQc([solutionPossible: 5, goodnessOfFit: 5], [:], [:], instance)
    }

    @Unroll
    void "setQcTrafficLightStatusBasedOnThresholdAndProjectSpecificHandling (tcc = #tcc & ploidy = #ploidy --> #resultStatus)"() {
        given:
        setupData()

        aceseqQc.tcc = tcc
        aceseqQc.ploidy = ploidy

        when:
        qcTrafficLightService.setQcTrafficLightStatusBasedOnThresholdAndProjectSpecificHandling(instance.sampleType1BamFile, aceseqQc)

        then:
        instance.sampleType1BamFile.qcTrafficLightStatus == resultStatus

        where:
        tcc | ploidy || resultStatus
        5   | 5      || QC_PASSED
        7   | 5      || QC_PASSED
        7   | 7      || QC_PASSED
        9   | 5      || BLOCKED
        9   | 9      || BLOCKED
    }

    @Unroll
    void "setQcTrafficLightStatusBasedOnThresholdAndProjectSpecificHandling, use project specific (tcc = #tcc --> #resultStatus)"() {
        given:
        setupData()

        aceseqQc.tcc = tcc
        aceseqQc.ploidy = 5

        DomainFactory.createQcThreshold(
                qcProperty1: 'tcc',
                seqType: instance.seqType,
                qcClass: AceseqQc.name,
                errorThresholdLower: 12,
                warningThresholdLower: 14,
                warningThresholdUpper: 16,
                errorThresholdUpper: 18,
                project: instance.project,
        )
        when:
        qcTrafficLightService.setQcTrafficLightStatusBasedOnThresholdAndProjectSpecificHandling(instance.sampleType1BamFile, aceseqQc)

        then:
        instance.sampleType1BamFile.qcTrafficLightStatus == resultStatus

        where:
        tcc || resultStatus
        3   || BLOCKED
        5   || BLOCKED
        7   || BLOCKED
        11  || BLOCKED
        13  || QC_PASSED
        15  || QC_PASSED
        17  || QC_PASSED
        19  || BLOCKED
    }

    @Unroll
    void "setQcTrafficLightStatusBasedOnThresholdAndProjectSpecificHandling, do not use project specific of other project (tcc = #tcc --> #resultStatus)"() {
        given:
        setupData()

        aceseqQc.tcc = tcc
        aceseqQc.ploidy = 5

        QcThreshold qcThreshold = QcThreshold.findByQcProperty1('tcc')
        qcThreshold.project = createProject()
        qcThreshold.save(flush: true)

        when:
        qcTrafficLightService.setQcTrafficLightStatusBasedOnThresholdAndProjectSpecificHandling(instance.sampleType1BamFile, aceseqQc)

        then:
        instance.sampleType1BamFile.qcTrafficLightStatus == resultStatus

        where:
        tcc || resultStatus
        0   || QC_PASSED
        5   || QC_PASSED
        9   || QC_PASSED
    }

    @Unroll
    void "setQcTrafficLightStatusBasedOnThresholdAndProjectSpecificHandling, once blocked files do not get unblocked (tcc = #tcc & ploidy = #ploidy --> #resultStatus)"() {
        given:
        setupData()

        aceseqQc.tcc = tcc
        aceseqQc.ploidy = ploidy
        AbstractMergedBamFile bamFile = instance.sampleType1BamFile

        when:
        instance.sampleType1BamFile.qcTrafficLightStatus = BLOCKED
        qcTrafficLightService.setQcTrafficLightStatusBasedOnThresholdAndProjectSpecificHandling(bamFile, aceseqQc)
        boolean thresholdExceeded = qcTrafficLightService.qcValuesExceedErrorThreshold(bamFile, aceseqQc)

        then:
        thresholdExceeded == exceedingExpected
        instance.sampleType1BamFile.qcTrafficLightStatus == BLOCKED

        where:
        tcc | ploidy || exceedingExpected
        5   | 5      || false
        9   | 9      || true
    }

    void "setQcTrafficLightStatusBasedOnThresholdAndProjectSpecificHandling, NO_CHECK causes UNCHECKED regardless of thresholds"() {
        given:
        setupData()

        [instance.sampleType1BamFile, instance.sampleType2BamFile].each {
            it.individual.project = createProject(qcThresholdHandling: QcThresholdHandling.NO_CHECK)
        }
        aceseqQc.tcc = tcc
        aceseqQc.ploidy = 5

        when:
        qcTrafficLightService.setQcTrafficLightStatusBasedOnThresholdAndProjectSpecificHandling(instance.sampleType1BamFile, aceseqQc)

        then:
        instance.sampleType1BamFile.qcTrafficLightStatus == UNCHECKED

        where:
        tcc << [0, 2, 4, 5, 6, 8, 10]
    }

    void "setQcTrafficLightStatusBasedOnThresholdAndProjectSpecificHandling, CHECK_AND_NOTIFY only causes AUTO_ACCEPTED if the threshold would fail"() {
        given:
        setupData()

        [instance.sampleType1BamFile, instance.sampleType2BamFile].each {
            it.individual.project = createProject(qcThresholdHandling: QcThresholdHandling.CHECK_AND_NOTIFY)
        }
        aceseqQc.tcc = tcc
        aceseqQc.ploidy = 5

        when:
        qcTrafficLightService.setQcTrafficLightStatusBasedOnThresholdAndProjectSpecificHandling(instance.sampleType1BamFile, aceseqQc)

        then:
        instance.sampleType1BamFile.qcTrafficLightStatus == expectedStatus

        where:
        tcc || expectedStatus
        0   || AUTO_ACCEPTED
        3   || QC_PASSED
        5   || QC_PASSED
        7   || QC_PASSED
        10  || AUTO_ACCEPTED
    }
}
