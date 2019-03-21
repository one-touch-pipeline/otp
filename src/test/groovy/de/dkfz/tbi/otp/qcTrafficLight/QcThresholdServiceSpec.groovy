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

import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaQc
import de.dkfz.tbi.otp.ngsdata.*

import static de.dkfz.tbi.otp.qcTrafficLight.TableCellValue.WarnColor.*

class QcThresholdServiceSpec extends Specification implements DataTest {

    Class[] getDomainClassesToMock() {[
            Project,
            QcThreshold,
            Realm,
            SeqType,
            SophiaQc,
    ]}

    QcThresholdService qcThresholdService

    String testedProperty
    int testedPropertyValue

    Project project
    SeqType seqType
    SophiaQc sophiaQc
    QcThreshold qcThreshold

    def setup() {
        qcThresholdService = new QcThresholdService()

        testedProperty = "controlMassiveInvPrefilteringLevel"
        testedPropertyValue = 15

        project = DomainFactory.createProject()
        seqType = DomainFactory.createWholeGenomeSeqType()
        sophiaQc = DomainFactory.createSophiaQc([
                ("${testedProperty}".toString()): testedPropertyValue,
                // don't create sophiaInstance, otherwise we have to @Mock too many domain classes
                sophiaInstance     : null,
        ], false).save(validate: false)
    }

    void "test createThresholdColorizer with no QcThreshold"() {
        when:
        QcThresholdService.ThresholdColorizer thresholdColorizer = qcThresholdService.createThresholdColorizer(project, seqType, sophiaQc.class)
        Map<String, TableCellValue> result = thresholdColorizer.colorize([testedProperty], sophiaQc)

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
                qcClass: sophiaQc.class.name,
        )

        when:
        QcThresholdService.ThresholdColorizer thresholdColorizer = qcThresholdService.createThresholdColorizer(project, seqType, sophiaQc.class)
        Map<String, TableCellValue> result = thresholdColorizer.colorize([testedProperty], sophiaQc)

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
                    qcClass: sophiaQc.class.name,
            )
        }

        when:
        QcThresholdService.ThresholdColorizer thresholdColorizer = qcThresholdService.createThresholdColorizer(project, seqType, sophiaQc.class)
        Map<String, TableCellValue> result = thresholdColorizer.colorize([testedProperty], sophiaQc)

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
                qcClass: sophiaQc.class.name,
                seqType: seqType,
        )

        when:
        QcThresholdService.ThresholdColorizer thresholdColorizer = qcThresholdService.createThresholdColorizer(project, seqType, sophiaQc.class)
        Map<String, TableCellValue> result = thresholdColorizer.colorize([testedProperty], sophiaQc)

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
                qcClass: sophiaQc.class.name,
        )

        expect:
        qcThresholdService.getClassesWithProperties() == [
                new ClassWithThreshold(
                        clasz: SophiaQc,
                        existingThresholds: [threshold],
                        availableThresholdProperties: [
                                "controlMassiveInvPrefilteringLevel",
                                "rnaContaminatedGenesCount",
                                "tumorMassiveInvFilteringLevel",
                        ]
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
                qcClass: sophiaQc.class.name,
                seqType: seqType,
        )

        QcThreshold threshold2 = DomainFactory.createQcThreshold(
                qcProperty1: testedProperty,
                errorThresholdLower: 1,
                warningThresholdLower: 2,
                warningThresholdUpper: 3,
                errorThresholdUpper: 4,
                compare: QcThreshold.ThresholdStrategy.ABSOLUTE_LIMITS,
                qcClass: sophiaQc.class.name,
                project: project,
                seqType: seqType,
        )

        expect:
        qcThresholdService.getClassesWithPropertiesForProjectAndSeqTypes(project, [seqType]) == [
                new ClassWithThresholds(
                        clasz: SophiaQc,
                        existingThresholds: [
                                new BothQcThresholds(seqType, testedProperty, threshold, threshold2),
                        ],
                        availableThresholdProperties: [
                                "controlMassiveInvPrefilteringLevel",
                                "rnaContaminatedGenesCount",
                                "tumorMassiveInvFilteringLevel",
                        ]
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
                qcClass: sophiaQc.class.name,
                seqType: seqType,
        )

        QcThreshold threshold2 = DomainFactory.createQcThreshold(
                qcProperty1: "tumorMassiveInvFilteringLevel",
                errorThresholdLower: 1,
                warningThresholdLower: 2,
                warningThresholdUpper: 3,
                errorThresholdUpper: 4,
                compare: QcThreshold.ThresholdStrategy.ABSOLUTE_LIMITS,
                qcClass: sophiaQc.class.name,
                seqType: seqType,
        )

        expect:
        qcThresholdService.getClassesWithPropertiesForProjectAndSeqTypes(project, [seqType]) == [
                new ClassWithThresholds(
                        clasz: SophiaQc,
                        existingThresholds: [
                                new BothQcThresholds(seqType, testedProperty, threshold, null),
                                new BothQcThresholds(seqType, "tumorMassiveInvFilteringLevel", threshold2, null),
                        ],
                        availableThresholdProperties: [
                                "controlMassiveInvPrefilteringLevel",
                                "rnaContaminatedGenesCount",
                                "tumorMassiveInvFilteringLevel",
                        ]
                ),
        ]
    }


    void "test createThreshold"() {
        given:
        when:
        qcThresholdService.createThreshold(
                null,
                SophiaQc.simpleName,
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
        threshold.qcClass == SophiaQc.name
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
                qcClass: sophiaQc.class.name,
        )

        when:
        Errors result = qcThresholdService.updateThreshold(
                qcThreshold,
                QcThreshold.ThresholdStrategy.DIFFERENCE_WITH_OTHER_PROPERTY,
                6,
                7,
                8,
                9,
                "rnaContaminatedGenesCount",
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
        qcThreshold.qcClass == sophiaQc.class.name
        qcThreshold.qcProperty2 == "rnaContaminatedGenesCount"
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
                qcClass: sophiaQc.class.name,
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
