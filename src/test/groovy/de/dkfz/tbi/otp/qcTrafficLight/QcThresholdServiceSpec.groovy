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
import org.grails.datastore.mapping.validation.ValidationErrors
import org.springframework.validation.Errors
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerQualityAssessment
import de.dkfz.tbi.otp.domainFactory.pipelines.AlignmentPipelineFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project

import static de.dkfz.tbi.otp.qcTrafficLight.TableCellValue.WarnColor.*

class QcThresholdServiceSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                CellRangerQualityAssessment,
                Project,
                QcThreshold,
                Realm,
                SeqType,
        ]
    }

    QcThresholdService qcThresholdService

    String testedProperty
    Long testedPropertyValue

    Project project
    SeqType seqType
    CellRangerQualityAssessment cellRangerQualityAssessment
    QcThreshold qcThreshold

    List<String> availableThresholdProperties = [
            "allBasesMapped",
            "duplicates",
            "estimatedNumberOfCells",
            "fractionReadsInCells",
            "insertSizeMedian",
            "insertSizeSD",
            "meanReadsPerCell",
            "medianGenesPerCell",
            "medianUmiCountsPerCell",
            "numberOfReads",
            "onTargetMappedBases",
            "onTargetRatio",
            "pairedInSequencing",
            "pairedRead1",
            "pairedRead2",
            "percentDiffChr",
            "percentDuplicates",
            "percentMappedReads",
            "percentProperlyPaired",
            "percentSingletons",
            "properlyPaired",
            "q30BasesInBarcode",
            "q30BasesInRnaRead",
            "q30BasesInUmi",
            "qcBasesMapped",
            "qcFailedReads",
            "readsMappedAntisenseToGene",
            "readsMappedConfidentlyToExonicRegions",
            "readsMappedConfidentlyToGenome",
            "readsMappedConfidentlyToIntergenicRegions",
            "readsMappedConfidentlyToIntronicRegions",
            "readsMappedConfidentlyToTranscriptome",
            "readsMappedToGenome",
            "referenceLength",
            "sequencingSaturation",
            "singletons",
            "totalGenesDetected",
            "totalMappedReadCounter",
            "totalReadCounter",
            "validBarcodes",
            "withItselfAndMateMapped",
            "withMateMappedToDifferentChr",
            "withMateMappedToDifferentChrMaq",
    ]


    def setup() {
        qcThresholdService = new QcThresholdService()

        testedProperty = "referenceLength"
        testedPropertyValue = 15

        project = DomainFactory.createProject()
        seqType = DomainFactory.createWholeGenomeSeqType()
        cellRangerQualityAssessment = AlignmentPipelineFactory.CellRangerFactoryInstance.INSTANCE.createQa(null, [
                ("${testedProperty}".toString()): testedPropertyValue,
        ], false)
    }

    void "test createThresholdColorizer with no QcThreshold"() {
        when:
        QcThresholdService.ThresholdColorizer thresholdColorizer = qcThresholdService.createThresholdColorizer(project, seqType, cellRangerQualityAssessment.class)
        Map<String, TableCellValue> result = thresholdColorizer.colorize([testedProperty], cellRangerQualityAssessment)

        then:
        result.get(testedProperty) == new TableCellValue(testedPropertyValue.toString(), OKAY, null, null)
    }

    @Unroll
    void "test createThresholdColorizer with QcThreshold with project #useProject"() {
        given:
        qcThreshold = DomainFactory.createQcThreshold(
                project: useProject ? project : null,
                seqType: seqType,
                qcProperty1: testedProperty,
                warningThresholdUpper: 10,
                errorThresholdUpper: 20,
                compare: QcThreshold.ThresholdStrategy.ABSOLUTE_LIMITS,
                qcClass: cellRangerQualityAssessment.class.name,
        )

        when:
        QcThresholdService.ThresholdColorizer thresholdColorizer = qcThresholdService.createThresholdColorizer(project, seqType, cellRangerQualityAssessment.class)
        Map<String, TableCellValue> result = thresholdColorizer.colorize([testedProperty], cellRangerQualityAssessment)

        then:
        result.get(testedProperty) == new TableCellValue(testedPropertyValue.toString(), WARNING, null, null)

        where:
        useProject | _
        false      | _
        true       | _
    }

    @Unroll
    void "test createThresholdColorizer with several QcThresholds, should use the one with the highest priority"() {
        given:
        List<Boolean> setProjectAndSeqType = [false, true]

        l.times { int i ->
            DomainFactory.createQcThreshold(
                    project: setProjectAndSeqType[i] ? project : null,
                    seqType: seqType,
                    qcProperty1: testedProperty,
                    warningThresholdUpper: l - 1 == i ? 10 : 99,
                    errorThresholdUpper: l - 1 == i ? 20 : 100,
                    compare: QcThreshold.ThresholdStrategy.ABSOLUTE_LIMITS,
                    qcClass: cellRangerQualityAssessment.class.name,
            )
        }

        when:
        QcThresholdService.ThresholdColorizer thresholdColorizer = qcThresholdService.createThresholdColorizer(project, seqType, cellRangerQualityAssessment.class)
        Map<String, TableCellValue> result = thresholdColorizer.colorize([testedProperty], cellRangerQualityAssessment)

        then:
        result.get(testedProperty) == new TableCellValue(testedPropertyValue.toString(), thresholdLevel, null, null)

        where:
        l || thresholdLevel
        1 || WARNING
        2 || WARNING
    }

    @Unroll
    void "test createThresholdColorizer with different warning and error levels"() {
        given:
        qcThreshold = DomainFactory.createQcThreshold(
                qcProperty1: testedProperty,
                warningThresholdLower: wtl,
                warningThresholdUpper: wtu,
                errorThresholdLower: etl,
                errorThresholdUpper: etu,
                compare: QcThreshold.ThresholdStrategy.ABSOLUTE_LIMITS,
                qcClass: cellRangerQualityAssessment.class.name,
                seqType: seqType,
        )

        when:
        QcThresholdService.ThresholdColorizer thresholdColorizer = qcThresholdService.createThresholdColorizer(project, seqType, cellRangerQualityAssessment.class)
        Map<String, TableCellValue> result = thresholdColorizer.colorize([testedProperty], cellRangerQualityAssessment)

        then:
        result.get(testedProperty) == new TableCellValue(testedPropertyValue.toString(), thresholdLevel, null, null)

        where:
        wtl  | wtu  | etl  | etu  || thresholdLevel
        14   | 16   | 0    | 20   || OKAY
        null | 16   | null | 20   || OKAY
        14   | null | 0    | null || OKAY
        16   | 20   | 0    | 30   || WARNING
        16   | null | 0    | null || WARNING
        10   | 14   | 0    | 20   || WARNING
        null | 14   | null | 20   || WARNING
        30   | 35   | 20   | 40   || ERROR
        30   | null | 20   | null || ERROR
        5    | 6    | 0    | 10   || ERROR
        null | 5    | null | 10   || ERROR
    }

    void "test getClassesWithProperties"() {
        given:
        QcThreshold threshold = DomainFactory.createQcThreshold(
                qcProperty1: testedProperty,
                errorThresholdLower: 1,
                warningThresholdLower: 2,
                warningThresholdUpper: 3,
                errorThresholdUpper: 4,
                compare: QcThreshold.ThresholdStrategy.ABSOLUTE_LIMITS,
                qcClass: cellRangerQualityAssessment.class.name,
        )

        expect:
        qcThresholdService.getClassesWithProperties() == [
                new ClassWithThreshold(
                        clasz: CellRangerQualityAssessment,
                        existingThresholds: [threshold],
                        availableThresholdProperties: availableThresholdProperties
                ),
        ]
    }

    void "test getClassesWithPropertiesForProjectAndSeqTypes"() {
        given:
        Project project = DomainFactory.createProject()
        SeqType seqType = DomainFactory.createWholeGenomeSeqType()
        QcThreshold threshold = DomainFactory.createQcThreshold(
                qcProperty1: testedProperty,
                errorThresholdLower: 1,
                warningThresholdLower: 2,
                warningThresholdUpper: 3,
                errorThresholdUpper: 4,
                compare: QcThreshold.ThresholdStrategy.ABSOLUTE_LIMITS,
                qcClass: cellRangerQualityAssessment.class.name,
                seqType: seqType,
        )

        QcThreshold threshold2 = DomainFactory.createQcThreshold(
                qcProperty1: testedProperty,
                errorThresholdLower: 1,
                warningThresholdLower: 2,
                warningThresholdUpper: 3,
                errorThresholdUpper: 4,
                compare: QcThreshold.ThresholdStrategy.ABSOLUTE_LIMITS,
                qcClass: cellRangerQualityAssessment.class.name,
                project: project,
                seqType: seqType,
        )

        expect:
        qcThresholdService.getClassesWithPropertiesForProjectAndSeqTypes(project, [seqType]) == [
                new ClassWithThresholds(
                        clasz: CellRangerQualityAssessment,
                        existingThresholds: [
                                new BothQcThresholds(seqType, testedProperty, threshold, threshold2),
                        ],
                        availableThresholdProperties: availableThresholdProperties
                ),
        ]
    }

    void "test getClassesWithPropertiesForProjectAndSeqTypes only default thresholds"() {
        given:
        Project project = DomainFactory.createProject()
        SeqType seqType = DomainFactory.createWholeGenomeSeqType()
        QcThreshold threshold = DomainFactory.createQcThreshold(
                qcProperty1: testedProperty,
                errorThresholdLower: 1,
                warningThresholdLower: 2,
                warningThresholdUpper: 3,
                errorThresholdUpper: 4,
                compare: QcThreshold.ThresholdStrategy.ABSOLUTE_LIMITS,
                qcClass: cellRangerQualityAssessment.class.name,
                seqType: seqType,
        )

        QcThreshold threshold2 = DomainFactory.createQcThreshold(
                qcProperty1: "qcBasesMapped",
                errorThresholdLower: 1,
                warningThresholdLower: 2,
                warningThresholdUpper: 3,
                errorThresholdUpper: 4,
                compare: QcThreshold.ThresholdStrategy.ABSOLUTE_LIMITS,
                qcClass: cellRangerQualityAssessment.class.name,
                seqType: seqType,
        )

        expect:
        qcThresholdService.getClassesWithPropertiesForProjectAndSeqTypes(project, [seqType]) == [
                new ClassWithThresholds(
                        clasz: CellRangerQualityAssessment,
                        existingThresholds: [
                                new BothQcThresholds(seqType, "qcBasesMapped", threshold2, null),
                                new BothQcThresholds(seqType, testedProperty, threshold, null),
                        ],
                        availableThresholdProperties: availableThresholdProperties
                ),
        ]
    }


    void "test createThreshold"() {
        given:
        when:
        qcThresholdService.createThreshold(
                null,
                CellRangerQualityAssessment.simpleName,
                testedProperty,
                seqType,
                QcThreshold.ThresholdStrategy.ABSOLUTE_LIMITS,
                1,
                2,
                3,
                4,
                null,
        )

        then:
        List<QcThreshold> all = QcThreshold.all
        all.size() == 1
        QcThreshold threshold = all.first()
        threshold.qcProperty1 == testedProperty
        threshold.project == null
        threshold.qcClass == CellRangerQualityAssessment.name
        threshold.seqType == seqType
        threshold.compare == QcThreshold.ThresholdStrategy.ABSOLUTE_LIMITS
        threshold.errorThresholdLower == 1
        threshold.warningThresholdLower == 2
        threshold.warningThresholdUpper == 3
        threshold.errorThresholdUpper == 4
        threshold.qcProperty2 == null
    }

    void "test updateThreshold"() {
        given:
        QcThreshold qcThreshold = DomainFactory.createQcThreshold(
                qcProperty1: testedProperty,
                errorThresholdLower: 1,
                warningThresholdLower: 2,
                warningThresholdUpper: 3,
                errorThresholdUpper: 4,
                compare: QcThreshold.ThresholdStrategy.ABSOLUTE_LIMITS,
                qcClass: cellRangerQualityAssessment.class.name,
        )

        when:
        Errors result = qcThresholdService.updateThreshold(
                qcThreshold,
                QcThreshold.ThresholdStrategy.DIFFERENCE_WITH_OTHER_PROPERTY,
                6,
                7,
                8,
                9,
                "estimatedNumberOfCells",
        )
        qcThreshold = QcThreshold.get(qcThreshold.id)

        then:
        result == null
        qcThreshold.qcProperty1 == testedProperty
        qcThreshold.errorThresholdLower == 6
        qcThreshold.warningThresholdLower == 7
        qcThreshold.warningThresholdUpper == 8
        qcThreshold.errorThresholdUpper == 9
        qcThreshold.compare == QcThreshold.ThresholdStrategy.DIFFERENCE_WITH_OTHER_PROPERTY
        qcThreshold.qcClass == cellRangerQualityAssessment.class.name
        qcThreshold.qcProperty2 == "estimatedNumberOfCells"
    }

    void "test updateThreshold, with validation error"() {
        given:
        QcThreshold qcThreshold = DomainFactory.createQcThreshold(
                qcProperty1: testedProperty,
                errorThresholdLower: 1,
                warningThresholdLower: 2,
                warningThresholdUpper: 3,
                errorThresholdUpper: 4,
                compare: QcThreshold.ThresholdStrategy.ABSOLUTE_LIMITS,
                qcClass: cellRangerQualityAssessment.class.name,
        )

        expect:
        qcThresholdService.updateThreshold(
                qcThreshold,
                QcThreshold.ThresholdStrategy.DIFFERENCE_WITH_OTHER_PROPERTY,
                6,
                7,
                8,
                9,
                null,
        ) instanceof ValidationErrors
    }

    void "test updateThreshold, with null"() {
        when:
        qcThresholdService.updateThreshold(
                null,
                QcThreshold.ThresholdStrategy.DIFFERENCE_WITH_OTHER_PROPERTY,
                6,
                7,
                8,
                9,
                "rnaContaminatedGenesCount",
        )

        then:
        thrown(AssertionError)
    }

    void "test deleteThreshold"() {
        given:
        QcThreshold qcThreshold = DomainFactory.createQcThreshold()

        when:
        qcThresholdService.deleteThreshold(qcThreshold)

        then:
        QcThreshold.all.size() == 0
    }

    void "test deleteThreshold, with null"() {
        when:
        qcThresholdService.deleteThreshold(null)

        then:
        thrown(AssertionError)
    }
}
