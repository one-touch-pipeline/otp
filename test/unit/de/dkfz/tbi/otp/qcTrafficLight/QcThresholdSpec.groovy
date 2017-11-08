package de.dkfz.tbi.otp.qcTrafficLight

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.dataprocessing.sophia.*
import de.dkfz.tbi.otp.ngsdata.*
import grails.test.mixin.*
import spock.lang.*

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
        SeqPlatformGroup,
        SeqPlatformModelLabel,
        SeqTrack,
        SoftwareTool,
        SophiaInstance,
        SophiaQc,
        QcThreshold,
        SeqType,

])
class QcThresholdSpec extends Specification {

    void "test qcPassed method with compare mode biggerThanThreshold and smallerThanThreshold"() {
        given:
        SophiaQc qc = DomainFactory.createSophiaQc(controlMassiveInvPrefilteringLevel: 10)
        QcThreshold threshold = DomainFactory.createQcThreshold(
                qcProperty1: "controlMassiveInvPrefilteringLevel",
                warningThreshold: qc1,
                errorThreshold: qc2,
                compare: compare,
                qcClass: "SophiaQc"
        )

        expect:
        threshold.qcPassed(qc) == warning

        where:
        compare                                  | qc1 | qc2 | warning
        QcThreshold.Compare.biggerThanThreshold  | 15  | 20  | QcThreshold.WarningLevel.NO
        QcThreshold.Compare.biggerThanThreshold  | 9   | 15  | QcThreshold.WarningLevel.WarningLevel1
        QcThreshold.Compare.biggerThanThreshold  | 5   | 8   | QcThreshold.WarningLevel.WarningLevel2
        QcThreshold.Compare.smallerThanThreshold | 5   | 8   | QcThreshold.WarningLevel.NO
        QcThreshold.Compare.smallerThanThreshold | 15  | 9   | QcThreshold.WarningLevel.WarningLevel1
        QcThreshold.Compare.smallerThanThreshold | 20  | 15  | QcThreshold.WarningLevel.WarningLevel2
    }


    void "test qcPassed method with compare mode biggerThanQcValue2 and smallerThanQcValue2"() {
        given:
        SophiaQc qc = DomainFactory.createSophiaQc(controlMassiveInvPrefilteringLevel: control, tumorMassiveInvFilteringLevel: tumor)
        QcThreshold threshold = DomainFactory.createQcThreshold(
                qcProperty1: "controlMassiveInvPrefilteringLevel",
                qcProperty2: "tumorMassiveInvFilteringLevel",
                warningThreshold: 5,
                errorThreshold: 10,
                compare: compare,
                qcClass: "SophiaQc"
        )


        expect:
        threshold.qcPassed(qc) == warning

        where:
        compare                                    | control | tumor | warning
        QcThreshold.Compare.biggerThanQcProperty2  | 15      | 20    | QcThreshold.WarningLevel.NO
        QcThreshold.Compare.biggerThanQcProperty2  | 18      | 12    | QcThreshold.WarningLevel.WarningLevel1
        QcThreshold.Compare.biggerThanQcProperty2  | 30      | 8     | QcThreshold.WarningLevel.WarningLevel2
        QcThreshold.Compare.smallerThanQcProperty2 | 8       | 5     | QcThreshold.WarningLevel.NO
        QcThreshold.Compare.smallerThanQcProperty2 | 5       | 11    | QcThreshold.WarningLevel.WarningLevel1
        QcThreshold.Compare.smallerThanQcProperty2 | 5       | 20    | QcThreshold.WarningLevel.WarningLevel2
    }


    void "test qcPassed method with compare mode biggerThanThresholdFactorExternalValue and smallerThanThresholdFactorExternalValue"() {
        given:
        SophiaQc qc = DomainFactory.createSophiaQc(controlMassiveInvPrefilteringLevel: 10)
        QcThreshold threshold = DomainFactory.createQcThreshold(
                qcProperty1: "controlMassiveInvPrefilteringLevel",
                warningThreshold: qc1,
                errorThreshold: qc2,
                compare: compare,
                qcClass: "SophiaQc"
        )
        double externalValue = 2

        expect:
        threshold.qcPassed(qc, externalValue) == warning

        where:
        compare                                                     | qc1 | qc2 | warning
        QcThreshold.Compare.biggerThanThresholdFactorExternalValue  | 15  | 20  | QcThreshold.WarningLevel.NO
        QcThreshold.Compare.biggerThanThresholdFactorExternalValue  | 4   | 15  | QcThreshold.WarningLevel.WarningLevel1
        QcThreshold.Compare.biggerThanThresholdFactorExternalValue  | 2   | 4   | QcThreshold.WarningLevel.WarningLevel2
        QcThreshold.Compare.smallerThanThresholdFactorExternalValue | 4   | 2   | QcThreshold.WarningLevel.NO
        QcThreshold.Compare.smallerThanThresholdFactorExternalValue | 6   | 4   | QcThreshold.WarningLevel.WarningLevel1
        QcThreshold.Compare.smallerThanThresholdFactorExternalValue | 8   | 6   | QcThreshold.WarningLevel.WarningLevel2
    }
}
