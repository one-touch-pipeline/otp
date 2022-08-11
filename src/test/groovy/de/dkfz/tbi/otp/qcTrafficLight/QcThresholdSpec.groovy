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

import grails.testing.gorm.DataTest
import grails.validation.ValidationException
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerQualityAssessment
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.domainFactory.pipelines.AlignmentPipelineFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project

import static de.dkfz.tbi.otp.qcTrafficLight.QcThreshold.ThresholdStrategy.*

class QcThresholdSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                AbstractMergedBamFile,
                CellRangerQualityAssessment,
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
                FastqImportInstance,
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
                QcThreshold,
                SeqType,
                Realm,
        ]
    }

    void "test saving QcThreshold with invalid threshold values"() {
        when:
        DomainFactory.createQcThreshold(
                warningThresholdLower: wtl,
                warningThresholdUpper: wtu,
                errorThresholdLower: etl,
                errorThresholdUpper: etu,
                qcClass: CellRangerQualityAssessment.name,
        )

        then:
        ValidationException e = thrown()
        e.message.contains(message)

        where:
        wtl  | wtu  | etl  | etu  || message
        40   | 30   | 10   | 50   || 'lower.warning.too.big'
        0    | 30   | 10   | 50   || 'lower.warning.too.small'
        20   | 60   | 10   | 50   || 'upper.warning.too.big'
        null | 60   | 10   | 50   || 'lower.warning.missing'
        20   | null | 10   | 50   || 'upper.warning.missing'
        null | null | 10   | 50   || 'lower.warning.missing'
        20   | 60   | null | 50   || 'lower.error.missing'
        20   | null | null | 50   || 'lower.error.missing'
        null | null | null | 50   || 'upper.missing'
        20   | 60   | 10   | null || 'upper.error.missing'
        null | 60   | 10   | null || 'lower.warning.missing'
        null | null | 10   | null || 'lower.warning.missing'
        20   | 60   | null | null || 'lower.error.missing'
        null | 60   | null | null || 'upper.missing'
        20   | null | null | null || 'lower.error.missing'
        null | null | null | null || 'upper.missing'
        null | 1    | 1    | 1    || 'lower.warning.missing'
        1    | 1    | null | 1    || 'lower.error.missing'
        1    | null | 1    | 1    || 'lower.warning.too.small'
        1    | 1    | null | 1    || 'lower.error.missing'
        1    | 1    | 1    | null || 'lower.warning.too.big'
        1    | 1    | 1    | 1    || 'lower.warning.too.big'
        null | 1    | null | 1    || 'upper.warning.too.big'
        1    | null | 1    | null || 'lower.warning.too.small'
    }

    @Unroll
    void "test saving QcThreshold when comparing to other value"() {
        when:
        QcThreshold threshold = DomainFactory.createQcThreshold([
                qcProperty1          : "estimatedNumberOfCells",
                compare              : compare,
                warningThresholdLower: 30,
                warningThresholdUpper: 40,
                errorThresholdLower  : 10,
                errorThresholdUpper  : 50,
                qcProperty2          : property2,
                qcClass              : CellRangerQualityAssessment.name,
        ], false)

        then:
        threshold.validate() == valid

        where:
        compare                        | property2                || valid
        DIFFERENCE_WITH_OTHER_PROPERTY | "medianGenesPerCell"     || true
        DIFFERENCE_WITH_OTHER_PROPERTY | "estimatedNumberOfCells" || false
        DIFFERENCE_WITH_OTHER_PROPERTY | null                     || false
    }

    @SuppressWarnings('SpaceAfterOpeningBrace')
    void "test saving QcThreshold duplicated"() {
        given:
        SeqType seqType = DomainFactory.createSeqType()

        when:
        QcThreshold qcThreshold1 = DomainFactory.createQcThreshold(
                qcClass: CellRangerQualityAssessment.name,
                project: project(),
                seqType: seqType,
                qcProperty1: "estimatedNumberOfCells",
        )
        DomainFactory.createQcThreshold(
                project: qcThreshold1.project,
                seqType: seqType,
                qcProperty1: qcThreshold1.qcProperty1,
                qcClass: qcThreshold1.qcClass,
        )

        then:
        ValidationException e = thrown()
        e.message.contains("unique")

        where:
        project                             | _
        ({ DomainFactory.createProject() }) | _
        ({ null })                          | _
    }

    @Unroll
    void "test qcPassed method with compare mode toThreshold"() {
        given:
        CellRangerQualityAssessment qc = AlignmentPipelineFactory.CellRangerFactoryInstance.INSTANCE.createQa(null, [
                estimatedNumberOfCells: 25,
        ], false)

        QcThreshold threshold = DomainFactory.createQcThreshold(
                qcProperty1: "estimatedNumberOfCells",
                warningThresholdLower: wtl,
                warningThresholdUpper: wtu,
                errorThresholdLower: etl,
                errorThresholdUpper: etu,
                compare: ABSOLUTE_LIMITS,
                qcClass: CellRangerQualityAssessment.name,
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
        CellRangerQualityAssessment qc = AlignmentPipelineFactory.CellRangerFactoryInstance.INSTANCE.createQa(null, [
                estimatedNumberOfCells: control,
                meanReadsPerCell: tumor,
        ], false)

        QcThreshold threshold = DomainFactory.createQcThreshold(
                qcProperty1: "estimatedNumberOfCells",
                qcProperty2: "meanReadsPerCell",
                warningThresholdLower: 20,
                warningThresholdUpper: 30,
                errorThresholdLower: 10,
                errorThresholdUpper: 40,
                compare: DIFFERENCE_WITH_OTHER_PROPERTY,
                qcClass: CellRangerQualityAssessment.name,
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
        CellRangerQualityAssessment qc = AlignmentPipelineFactory.CellRangerFactoryInstance.INSTANCE.createQa(null, [
                estimatedNumberOfCells: 50,
        ], false)

        QcThreshold threshold = DomainFactory.createQcThreshold(
                qcProperty1: "estimatedNumberOfCells",
                warningThresholdLower: wtl,
                warningThresholdUpper: wtu,
                errorThresholdLower: etl,
                errorThresholdUpper: etu,
                compare: RATIO_TO_EXTERNAL_VALUE,
                qcClass: CellRangerQualityAssessment.name,
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
    void "test qcPassed method with compare mode toThresholdFactorExternalValue if factor is #extValue"() {
        given:
        CellRangerQualityAssessment qc = AlignmentPipelineFactory.CellRangerFactoryInstance.INSTANCE.createQa(null, [
                estimatedNumberOfCells: 50,
        ], false)

        QcThreshold threshold = DomainFactory.createQcThreshold(
                qcProperty1: "estimatedNumberOfCells",
                warningThresholdLower: 20,
                warningThresholdUpper: 30,
                errorThresholdLower: 10,
                errorThresholdUpper: 40,
                compare: RATIO_TO_EXTERNAL_VALUE,
                qcClass: CellRangerQualityAssessment.name,
        )

        expect:
        threshold.qcPassed(qc, extValue) == QcThreshold.ThresholdLevel.OKAY

        where:
        extValue << [
                0,
                null,
        ]
    }
}
