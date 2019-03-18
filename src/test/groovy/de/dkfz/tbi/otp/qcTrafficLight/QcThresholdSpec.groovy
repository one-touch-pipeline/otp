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

import grails.test.mixin.Mock
import grails.validation.ValidationException
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaQc
import de.dkfz.tbi.otp.ngsdata.*

import static de.dkfz.tbi.otp.qcTrafficLight.QcThreshold.ThresholdStrategy.*

@Mock([
        AbstractMergedBamFile,
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

    void "test saving QcThreshold with invalid threshold values"() {
        when:
        DomainFactory.createQcThreshold(
                warningThresholdLower: wtl,
                warningThresholdUpper: wtu,
                errorThresholdLower: etl,
                errorThresholdUpper: etu,
                qcClass: SophiaQc.name,
        )

        then:
        ValidationException e = thrown()
        e.message.contains(message)

        where:
        wtl  | wtu  | etl  | etu  || message
        40   | 30   | 10   | 50   || 'LOWER warning threshold must be smaller than UPPER warning threshold'
        0    | 30   | 10   | 50   || 'Lower WARNING threshold must be bigger than lower ERROR threshold'
        20   | 60   | 10   | 50   || 'Upper WARNING threshold must be smaller than upper ERROR threshold'
        null | 60   | 10   | 50   || 'Please also set a lower WARNING threshold when defining a lower ERROR threshold'
        20   | null | 10   | 50   || 'Please also set a upper WARNING threshold when defining a upper ERROR threshold'
        null | null | 10   | 50   || 'Please also set a lower WARNING threshold when defining a lower ERROR threshold'
        20   | 60   | null | 50   || 'When setting a lower WARNING threshold, please also define the lower ERROR threshold'
        20   | null | null | 50   || 'When setting a lower WARNING threshold, please also define the lower ERROR threshold'
        null | null | null | 50   || 'When leaving the lower thresholds empty, please define BOTH upper warning and upper error thresholds'
        20   | 60   | 10   | null || 'When setting an upper WARNING threshold, please also define an upper ERROR threshold'
        null | 60   | 10   | null || 'Please also set a lower WARNING threshold when defining a lower ERROR threshold'
        null | null | 10   | null || 'Please also set a lower WARNING threshold when defining a lower ERROR threshold'
        20   | 60   | null | null || 'When setting a lower WARNING threshold, please also define the lower ERROR threshold'
        null | 60   | null | null || 'When leaving the lower thresholds empty, please define BOTH upper warning and upper error thresholds'
        20   | null | null | null || 'When setting a lower WARNING threshold, please also define the lower ERROR threshold'
        null | null | null | null || 'When leaving the lower thresholds empty, please define BOTH upper warning and upper error thresholds'
        null | 1    | 1    | 1    || 'Please also set a lower WARNING threshold when defining a lower ERROR threshold'
        1    | 1    | null | 1    || 'When setting a lower WARNING threshold, please also define the lower ERROR threshold'
        1    | null | 1    | 1    || 'Lower WARNING threshold must be bigger than lower ERROR threshold'
        1    | 1    | null | 1    || 'When setting a lower WARNING threshold, please also define the lower ERROR threshold'
        1    | 1    | 1    | null || 'LOWER warning threshold must be smaller than UPPER warning threshold'
        1    | 1    | 1    | 1    || 'LOWER warning threshold must be smaller than UPPER warning threshold'
        null | 1    | null | 1    || 'Upper WARNING threshold must be smaller than upper ERROR threshold'
        1    | null | 1    | null || 'Lower WARNING threshold must be bigger than lower ERROR threshold'
    }

    @Unroll
    void "test saving QcThreshold when comparing to other value"() {
        when:
        QcThreshold threshold = DomainFactory.createQcThreshold([
                qcProperty1          : "controlMassiveInvPrefilteringLevel",
                compare              : compare,
                warningThresholdLower: 30,
                warningThresholdUpper: 40,
                errorThresholdLower  : 10,
                errorThresholdUpper  : 50,
                qcProperty2          : property2,
                qcClass              : SophiaQc.name,
        ], false)

        then:
        threshold.validate() == valid

        where:
        compare                        | property2                            || valid
        DIFFERENCE_WITH_OTHER_PROPERTY | "rnaContaminatedGenesCount"          || true
        DIFFERENCE_WITH_OTHER_PROPERTY | "controlMassiveInvPrefilteringLevel" || false
        DIFFERENCE_WITH_OTHER_PROPERTY | null                                 || false
    }

    @SuppressWarnings('SpaceAfterOpeningBrace')
    void "test saving QcThreshold duplicated"() {
        given:
        SeqType seqType = DomainFactory.createSeqType()

        when:
        QcThreshold qcThreshold1 = DomainFactory.createQcThreshold(
                qcClass: SophiaQc.name,
                project: project(),
                seqType: seqType,
                qcProperty1: "controlMassiveInvPrefilteringLevel",
        )
        DomainFactory.createQcThreshold(
                project: qcThreshold1.project,
                seqType: seqType,
                qcProperty1: qcThreshold1.qcProperty1,
                qcClass: qcThreshold1.qcClass,
        )

        then:
        ValidationException e = thrown()
        e.message.contains("QcThreshold for")
        e.message.contains("already exists")

        where:
        project                             | _
        ({ DomainFactory.createProject() }) | _
        ({ null })                          | _
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
                compare: ABSOLUTE_LIMITS,
                qcClass: SophiaQc.name,
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
                compare: DIFFERENCE_WITH_OTHER_PROPERTY,
                qcClass: SophiaQc.name,
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
                compare: RATIO_TO_EXTERNAL_VALUE,
                qcClass: SophiaQc.name,
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
                compare: RATIO_TO_EXTERNAL_VALUE,
                qcClass: SophiaQc.name,
        )

        expect:
        threshold.qcPassed(qc, externalValue) == QcThreshold.ThresholdLevel.OKAY

        where:
        externalValue << [
                0,
                null,
        ]
    }
}
