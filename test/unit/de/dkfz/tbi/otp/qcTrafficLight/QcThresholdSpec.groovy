package de.dkfz.tbi.otp.qcTrafficLight

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.dataprocessing.sophia.*
import de.dkfz.tbi.otp.ngsdata.*
import grails.test.mixin.*
import spock.lang.*
import grails.validation.ValidationException

@Mock([
        DataFile,
        FileType,
        Individual,
        LibraryPreparationKit,
        MergingCriteria,
        MergingWorkPackage,
        Pipeline,
        Project,
        ReferenceGenome,
        RoddyBamFile,
        RoddyWorkflowConfig,
        Run,
        RunSegment,
        Sample,
        SamplePair,
        SampleType,
        SampleTypePerProject,
        SeqCenter,
        SeqPlatform,
        SeqPlatformModelLabel,
        SeqPlatformGroup,
        SeqPlatformModelLabel,
        SeqTrack,
        SoftwareTool,
        SophiaInstance,
        SophiaQc,
        QcThreshold,
        SeqType,
        Realm,
])
class QcThresholdSpec extends Specification {

    final static String UNDIFNED_THRESHOLDS = 'At least both lower or upper thresholds must be defined'

    @Unroll
    void "test saving QcThreshold with invalid threshold values"() {
        when:
        DomainFactory.createQcThreshold(
                warningThresholdLower: wtl,
                warningThresholdUpper: wtu,
                errorThresholdLower: etl,
                errorThresholdUpper: etu,
                qcClass: "SophiaQc"
        )

        then:
        ValidationException e = thrown()
        e.message.contains(message)

        where:
        wtl  | wtu  | etl  | etu  || message
        40   | 30   | 10   | 50   || 'warningThresholdLower must be smaller than warningThresholdUpper'
        0    | 30   | 10   | 50   || 'warningThresholdLower must be bigger than errorThresholdLower'
        20   | 60   | 10   | 50   || 'warningThresholdUpper must be smaller than errorThresholdUpper'
        null | 60   | 10   | 50   || UNDIFNED_THRESHOLDS
        20   | null | 10   | 50   || UNDIFNED_THRESHOLDS
        null | null | 10   | 50   || UNDIFNED_THRESHOLDS
        20   | 60   | null | 50   || UNDIFNED_THRESHOLDS
        20   | null | null | 50   || UNDIFNED_THRESHOLDS
        null | null | null | 50   || UNDIFNED_THRESHOLDS
        20   | 60   | 10   | null || UNDIFNED_THRESHOLDS
        null | 60   | 10   | null || UNDIFNED_THRESHOLDS
        null | null | 10   | null || UNDIFNED_THRESHOLDS
        20   | 60   | null | null || UNDIFNED_THRESHOLDS
        null | 60   | null | null || UNDIFNED_THRESHOLDS
        20   | null | null | null || UNDIFNED_THRESHOLDS
        null | null | null | null || UNDIFNED_THRESHOLDS
    }

    void "test saving QcThreshold duplicated"() {
        when:
        QcThreshold qcThreshold1 = DomainFactory.createQcThreshold(
                qcClass: "SophiaQc",
                project: project(),
                seqType: seqType(),
        )
        QcThreshold qcThreshold2 = DomainFactory.createQcThreshold(
                project: qcThreshold1.project,
                seqType: qcThreshold1.seqType,
                qcProperty1: qcThreshold1.qcProperty1,
                qcClass: qcThreshold1.qcClass,
        )

        then:
        ValidationException e = thrown()
        e.message.contains("QcThreshold for Project: '${qcThreshold1.project}', SeqType: '${qcThreshold1.seqType}', QcClass: '${qcThreshold1.qcClass}' and QcProperty1: '${qcThreshold1.qcProperty1}' already exists")

        where:
        project                             | seqType
        ({ DomainFactory.createProject() }) | ({ DomainFactory.createSeqType() })
        ({ null })                          | ({ DomainFactory.createSeqType() })
        ({ DomainFactory.createProject() }) | ({ null })
        ({ null })                          | ({ null })
    }

    @Unroll
    void "test qcPassed method with compare mode toThreshold"() {
        given:
        SophiaQc qc = DomainFactory.createSophiaQc(controlMassiveInvPrefilteringLevel: 25)
        QcThreshold threshold = DomainFactory.createQcThreshold(
                qcProperty1: "controlMassiveInvPrefilteringLevel",
                warningThresholdLower: wtl,
                warningThresholdUpper: wtu,
                errorThresholdLower: etl,
                errorThresholdUpper: etu,
                compare: QcThreshold.Compare.toThreshold,
                qcClass: "SophiaQc"
        )

        expect:
        threshold.qcPassed(qc) == warning

        where:
        wtl  | wtu  | etl  | etu  || warning
        20   | 30   | 0    | 50   || QcThreshold.ThresholdLevel.OKAY
        20   | null | 0    | null || QcThreshold.ThresholdLevel.OKAY
        null | 30   | null | 50   || QcThreshold.ThresholdLevel.OKAY
        30   | 40   | 0    | 50   || QcThreshold.ThresholdLevel.WARNING
        30   | null | 0    | null || QcThreshold.ThresholdLevel.WARNING
        10   | 20   | 0    | 50   || QcThreshold.ThresholdLevel.WARNING
        null | 20   | null | 50   || QcThreshold.ThresholdLevel.WARNING
        35   | null | 30   | null || QcThreshold.ThresholdLevel.ERROR
        35   | 45   | 30   | 50   || QcThreshold.ThresholdLevel.ERROR
        null | 15   | null | 20   || QcThreshold.ThresholdLevel.ERROR
        10   | 15   | 0    | 20   || QcThreshold.ThresholdLevel.ERROR
    }

    @Unroll
    void "test qcPassed method with compare mode toQcProperty2"() {
        given:
        SophiaQc qc = DomainFactory.createSophiaQc(controlMassiveInvPrefilteringLevel: control, tumorMassiveInvFilteringLevel: tumor)
        QcThreshold threshold = DomainFactory.createQcThreshold(
                qcProperty1: "controlMassiveInvPrefilteringLevel",
                qcProperty2: "tumorMassiveInvFilteringLevel",
                warningThresholdLower: 20,
                warningThresholdUpper: 30,
                errorThresholdLower: 10,
                errorThresholdUpper: 40,
                compare: QcThreshold.Compare.toQcProperty2,
                qcClass: "SophiaQc"
        )


        expect:
        threshold.qcPassed(qc) == warning

        where:
        control | tumor || warning
        50      | 25    || QcThreshold.ThresholdLevel.OKAY
        50      | 15    || QcThreshold.ThresholdLevel.WARNING
        50      | 35    || QcThreshold.ThresholdLevel.WARNING
        50      | 5     || QcThreshold.ThresholdLevel.ERROR
        50      | 45    || QcThreshold.ThresholdLevel.ERROR
    }

    @Unroll
    void "test qcPassed method with compare mode toThresholdFactorExternalValue"() {
        given:
        SophiaQc qc = DomainFactory.createSophiaQc(controlMassiveInvPrefilteringLevel: 50)
        QcThreshold threshold = DomainFactory.createQcThreshold(
                qcProperty1: "controlMassiveInvPrefilteringLevel",
                warningThresholdLower: wtl,
                warningThresholdUpper: wtu,
                errorThresholdLower: etl,
                errorThresholdUpper: etu,
                compare: QcThreshold.Compare.toThresholdFactorExternalValue,
                qcClass: "SophiaQc"
        )
        double externalValue = 2

        expect:
        threshold.qcPassed(qc, externalValue) == warning

        where:
        wtl | wtu | etl | etu || warning
        20  | 30  | 0   | 50  || QcThreshold.ThresholdLevel.OKAY
        30  | 40  | 0   | 50  || QcThreshold.ThresholdLevel.WARNING
        10  | 20  | 0   | 50  || QcThreshold.ThresholdLevel.WARNING
        35  | 45  | 30  | 50  || QcThreshold.ThresholdLevel.ERROR
        10  | 15  | 0   | 20  || QcThreshold.ThresholdLevel.ERROR
    }

    @Unroll
    void "test qcPassed method with compare mode toThresholdFactorExternalValue if factor is #externalValue"() {
        given:
        SophiaQc qc = DomainFactory.createSophiaQc(controlMassiveInvPrefilteringLevel: 50)
        QcThreshold threshold = DomainFactory.createQcThreshold(
                qcProperty1: "controlMassiveInvPrefilteringLevel",
                warningThresholdLower: 20,
                warningThresholdUpper: 30,
                errorThresholdLower: 10,
                errorThresholdUpper: 40,
                compare: QcThreshold.Compare.toThresholdFactorExternalValue,
                qcClass: "SophiaQc"
        )

        expect:
        threshold.qcPassed(qc, externalValue) == QcThreshold.ThresholdLevel.OKAY

        where:
        externalValue << [
                0,
                null
        ]
    }
}
