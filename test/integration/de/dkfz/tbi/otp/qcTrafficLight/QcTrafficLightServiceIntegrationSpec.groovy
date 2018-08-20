package de.dkfz.tbi.otp.qcTrafficLight

import de.dkfz.tbi.otp.CommentService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.utils.Principal
import grails.plugin.springsecurity.SpringSecurityService
import spock.lang.Specification

import static de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile.QcTrafficLightStatus.*

class QcTrafficLightServiceIntegrationSpec extends Specification {

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

    void "setQcTrafficLightStatusBasedOnThreshold"() {
        given:
        aceseqQc.tcc = tcc
        aceseqQc.ploidy = ploidy

        when:
        qcTrafficLightService.setQcTrafficLightStatusBasedOnThreshold(instance.sampleType1BamFile, aceseqQc)

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

    void "setQcTrafficLightStatusBasedOnThreshold, once blocked files do not get unblocked"() {
        given:
        aceseqQc.tcc = tcc
        aceseqQc.ploidy = ploidy
        AbstractMergedBamFile bamFile = instance.sampleType1BamFile

        when:
        instance.sampleType1BamFile.qcTrafficLightStatus = BLOCKED
        qcTrafficLightService.setQcTrafficLightStatusBasedOnThreshold(bamFile, aceseqQc)
        boolean thresholdExceeded = qcTrafficLightService.qcValuesExceedErrorThreshold(bamFile, aceseqQc)

        then:
        thresholdExceeded == exceedingExpected
        instance.sampleType1BamFile.qcTrafficLightStatus == BLOCKED

        where:
        tcc | ploidy || exceedingExpected
        5   | 5      || false
        9   | 9      || true
    }

    void "setQcTrafficLightStatusBasedOnThreshold, checks and sets both bam files of a given instance"() {
        given:
        aceseqQc.tcc = 9
        aceseqQc.ploidy = 9

        expect:
        instance.sampleType1BamFile.qcTrafficLightStatus == null
        instance.sampleType2BamFile.qcTrafficLightStatus == null

        when:
        qcTrafficLightService.setQcTrafficLightStatusBasedOnThreshold(instance, aceseqQc)

        then:
        instance.sampleType1BamFile.qcTrafficLightStatus == BLOCKED
        instance.sampleType2BamFile.qcTrafficLightStatus == BLOCKED
    }
}
