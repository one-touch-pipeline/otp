package de.dkfz.tbi.otp.qcTrafficLight

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.domainFactory.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import grails.plugin.springsecurity.*
import spock.lang.*

import static de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile.QcTrafficLightStatus.*

class QcTrafficLightServiceIntegrationSpec extends Specification implements DomainFactoryCore {

    QcTrafficLightService qcTrafficLightService

    AceseqInstance instance
    AceseqQc aceseqQc

    def setup() {
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

    void "setQcTrafficLightStatusBasedOnThresholdAndProjectSpecificHandling, checks and sets both bam files of a given instance"() {
        given:
        aceseqQc.tcc = 9
        aceseqQc.ploidy = 9

        expect:
        instance.sampleType1BamFile.qcTrafficLightStatus == null
        instance.sampleType2BamFile.qcTrafficLightStatus == null

        when:
        qcTrafficLightService.setQcTrafficLightStatusBasedOnThresholdAndProjectSpecificHandling(instance, aceseqQc)

        then:
        instance.sampleType1BamFile.qcTrafficLightStatus == BLOCKED
        instance.sampleType2BamFile.qcTrafficLightStatus == BLOCKED
    }

    void "setQcTrafficLightStatusBasedOnThresholdAndProjectSpecificHandling, NO_CHECK causes UNCHECKED regardless of thresholds"() {
        given:
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
