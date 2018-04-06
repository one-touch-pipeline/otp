package de.dkfz.tbi.otp.qcTrafficLight

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import groovy.transform.*

class QcThresholdService {

    def <T extends QcTrafficLightValue> ThresholdColorizer<T> createThresholdColorizer(Project project, SeqType seqType, Class<T> qcClass) {
        new ThresholdColorizer(project, seqType, qcClass)
    }


    static class ThresholdColorizer<T extends QcTrafficLightValue> {
        private Map<String, QcThreshold> thresholdList
        private Class<QcTrafficLightValue> qcClass

        public ThresholdColorizer(Project project, SeqType seqType, Class<T> qcClass) {
            this.thresholdList = QcThreshold.createCriteria().list {
                eq("qcClass", qcClass.name)
                or {
                    eq("project", project)
                    isNull("project")
                }
                or {
                    eq("seqType", seqType)
                    isNull("seqType")
                }
            } .groupBy { it.qcProperty1 }.collect {
                it.value.find { it.project == project && it.seqType == seqType } ?:
                        it.value.find { it.project == project && it.seqType == null } ?:
                                it.value.find { it.project == null && it.seqType == seqType } ?:
                                        it.value.find { it.project == null && it.seqType == null }
            } .collectEntries { QcThreshold threshold ->
                [(threshold.qcProperty1): threshold]
            }
            this.qcClass = qcClass
        }

        Map<String, TableCellValue> colorize(List<String> properties, T qcObject) {
            properties.collectEntries { String property ->
                String value = FormatHelper.formatNumber(qcObject."${property}")
                TableCellValue.WarnColor warn = convert(thresholdList[property]?.qcPassed(qcObject) ?:
                        QcThreshold.ThresholdLevel.OKAY)
                [(property), new TableCellValue(value, warn)]
            }
        }

        /** for properties where a comparison with an external value is needed */
        Map<String, TableCellValue> colorize(Map<String, Double> properties, T qcObject) {
            properties.collectEntries { String property, Double externalValue ->
                String value = FormatHelper.formatNumber(qcObject."${property}")
                TableCellValue.WarnColor warn = convert(thresholdList[property]?.qcPassed(qcObject, externalValue) ?:
                        QcThreshold.ThresholdLevel.OKAY)
                [(property), new TableCellValue(value, warn)]
            } as Map<String, TableCellValue>
        }

        private TableCellValue.WarnColor convert(QcThreshold.ThresholdLevel t) {
            return [
                    (QcThreshold.ThresholdLevel.OKAY)   : TableCellValue.WarnColor.OKAY,
                    (QcThreshold.ThresholdLevel.WARNING): TableCellValue.WarnColor.WARNING,
                    (QcThreshold.ThresholdLevel.ERROR)  : TableCellValue.WarnColor.ERROR,
            ].get(t)
        }
    }
}

@ToString
@TupleConstructor
@EqualsAndHashCode
class TableCellValue implements Serializable {
    String value
    WarnColor warnColor
    String link
    String tooltip
    Icon icon
    String status
    long id

    enum WarnColor {
        OKAY,
        WARNING,
        ERROR,
    }

    enum Icon {
        OKAY,
        WARNING,
        ERROR,
    }
}
