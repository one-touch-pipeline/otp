package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.dataprocessing.sophia.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.qcTrafficLight.*
import grails.test.mixin.*
import spock.lang.*

import static de.dkfz.tbi.otp.qcTrafficLight.TableCellValue.WarnColor.*

@Mock([
        Project,
        QcThreshold,
        Realm,
        SeqType,
        SophiaQc,
])
class QcThresholdServiceSpec extends Specification {
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
                "${testedProperty}": testedPropertyValue,
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
    void "test createThresholdColorizer with QcThreshold with project #useProject and seqType #useSeqType"() {
        given:
        qcThreshold = DomainFactory.createQcThreshold(
                project: useProject ? project : null,
                seqType: useSeqType ? seqType : null,
                qcProperty1: testedProperty,
                warningThresholdUpper: 10,
                errorThresholdUpper: 20,
                compare: QcThreshold.Compare.toThreshold,
                qcClass: sophiaQc.class.name,
        )

        when:
        QcThresholdService.ThresholdColorizer thresholdColorizer = qcThresholdService.createThresholdColorizer(project, seqType, sophiaQc.class)
        Map<String, TableCellValue> result = thresholdColorizer.colorize([testedProperty], sophiaQc)

        then:
        result.get(testedProperty) == new TableCellValue(testedPropertyValue.toString(), WARNING, null, null)

        where:
        useProject | useSeqType
        false      | false
        false      | true
        true       | false
        true       | true
    }

    @Unroll
    void "test createThresholdColorizer with several QcThresholds, should use the one with the highest priority"() {
        given:
        List<List<Boolean>> setProjectAndSeqType = [
                [false, false],
                [false, true],
                [true, false],
                [true, true],
        ]

        l.times { int i ->
            DomainFactory.createQcThreshold(
                    project: setProjectAndSeqType[i][0] ? project : null,
                    seqType: setProjectAndSeqType[i][1] ? seqType : null,
                    qcProperty1: testedProperty,
                    warningThresholdUpper: l - 1 == i ? 10 : 99,
                    errorThresholdUpper: l - 1 == i ? 20 : 100,
                    compare: QcThreshold.Compare.toThreshold,
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
        3 || WARNING
        4 || WARNING
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
                compare: QcThreshold.Compare.toThreshold,
                qcClass: sophiaQc.class.name,
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
        16   | 20   | 0    | 20   || WARNING
        16   | null | 0    | null || WARNING
        10   | 14   | 0    | 20   || WARNING
        null | 14   | null | 20   || WARNING
        30   | 30   | 20   | 40   || ERROR
        30   | null | 20   | null || ERROR
        5    | 5    | 0    | 10   || ERROR
        null | 5    | null | 10   || ERROR
    }
}
