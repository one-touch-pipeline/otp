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
                warningThreshold: 10,
                errorThreshold: 20,
                compare: QcThreshold.Compare.biggerThanThreshold,
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
            qcThreshold = DomainFactory.createQcThreshold(
                    project: setProjectAndSeqType[i][0] ? project : null,
                    seqType: setProjectAndSeqType[i][1] ? seqType : null,
                    qcProperty1: testedProperty,
                    warningThreshold: l - 1 == i ? 10 : 99,
                    errorThreshold: l - 1 == i ? 20 : 100,
                    compare: QcThreshold.Compare.biggerThanThreshold,
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
                warningThreshold: warningThreshold,
                errorThreshold: errorThreshold,
                compare: QcThreshold.Compare.biggerThanThreshold,
                qcClass: sophiaQc.class.name,
        )

        when:
        QcThresholdService.ThresholdColorizer thresholdColorizer = qcThresholdService.createThresholdColorizer(project, seqType, sophiaQc.class)
        Map<String, TableCellValue> result = thresholdColorizer.colorize([testedProperty], sophiaQc)

        then:
        result.get(testedProperty) == new TableCellValue(testedPropertyValue.toString(), thresholdLevel, null, null)

        where:
        warningThreshold | errorThreshold || thresholdLevel
        0                | 0              || ERROR
        0                | 10             || ERROR
        0                | 20             || WARNING
        10               | 0              || ERROR
        10               | 10             || ERROR
        10               | 20             || WARNING
        20               | 0              || ERROR
        20               | 10             || ERROR
        20               | 20             || OKAY
    }
}
