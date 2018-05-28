package de.dkfz.tbi.otp.qcTrafficLight

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*

class QcThreshold implements Entity {

    Project project

    SeqType seqType
    // i.e. duplicates
    String qcProperty1
    // this value can be nullable, it can be used if two qc values shall be compared
    String qcProperty2
    Double warningThresholdLower
    Double warningThresholdUpper
    Double errorThresholdLower
    Double errorThresholdUpper
    Compare compare
    // it can happen that a property has the same name in different qc classes, therefore the class has also to be stored
    String qcClass

    static constraints = {
        qcProperty1(validator: { val, obj ->
            QcThreshold qcThreshold = CollectionUtils.atMostOneElement(QcThreshold.findAllByProjectAndSeqTypeAndQcClassAndQcProperty1(obj.project, obj.seqType, obj.qcClass, val))
            if (qcThreshold && qcThreshold != obj) {
                return "QcThreshold for Project: '${obj.project}', SeqType: '${obj.seqType}', QcClass: '${obj.qcClass}' and QcProperty1: '${val}' already exists"
            }
        })
        project nullable: true
        seqType nullable: true
        qcProperty2 nullable: true
        warningThresholdLower(nullable: true, validator: { val, obj ->
            if (val == null) {
                if (obj.warningThresholdUpper == null || obj.errorThresholdUpper == null || obj.errorThresholdLower != null) {
                    return 'At least both lower or upper thresholds must be defined'
                }
            } else {
                if (obj.errorThresholdLower == null) {
                    return 'At least both lower or upper thresholds must be defined'
                }
                if (obj.warningThresholdUpper != null && val > obj.warningThresholdUpper) {
                    return 'warningThresholdLower must be smaller than warningThresholdUpper'
                }
                if (val < obj.errorThresholdLower) {
                    return 'warningThresholdLower must be bigger than errorThresholdLower'
                }
            }
        })
        warningThresholdUpper(nullable: true, validator: { val, obj ->
            if (val == null) {
                if (obj.errorThresholdUpper != null) {
                    return 'At least both lower or upper thresholds must be defined'
                }
            } else {
                if (obj.errorThresholdUpper == null) {
                    return 'At least both lower or upper thresholds must be defined'
                }
                if (val > obj.errorThresholdUpper) {
                    return 'warningThresholdUpper must be smaller than errorThresholdUpper'
                }
            }
        })
        errorThresholdLower(nullable: true)
        errorThresholdUpper(nullable: true)
    }

    static enum Compare {
        toThreshold,
        toQcProperty2,
        toThresholdFactorExternalValue
    }

    static enum ThresholdLevel {
        OKAY,
        WARNING,
        ERROR,
    }

    ThresholdLevel qcPassed(QcTrafficLightValue qc, double externalValue = 0) {
        switch (this.compare) {
            case Compare.toThreshold:
                compareToThreshold(qc)
                break
            case Compare.toQcProperty2:
                compareToQcProperty2(qc)
                break
            case Compare.toThresholdFactorExternalValue:
                compareToThresholdFactorExternalValue(qc, externalValue)
                break
            default: throw new RuntimeException("No other comparison is defined yet")
        }
    }

    private ThresholdLevel compareToThreshold(QcTrafficLightValue qc) {
        if (qc."${qcProperty1}" == null) {
            return ThresholdLevel.OKAY
        }
        return getWarningLevel(qc."${qcProperty1}")
    }

    private ThresholdLevel compareToQcProperty2(QcTrafficLightValue qc) {
        if (qc."${qcProperty1}" == null || qc."${qcProperty2}" == null) {
            return ThresholdLevel.OKAY
        }
        return getWarningLevel(qc."${qcProperty1}" - qc."${qcProperty2}")
    }


    private ThresholdLevel compareToThresholdFactorExternalValue(QcTrafficLightValue qc, double externalValue) {
        if (qc."${qcProperty1}" == null || externalValue == 0) {
            return ThresholdLevel.OKAY
        }
        return getWarningLevel(qc."${qcProperty1}" / externalValue)
    }

    private ThresholdLevel getWarningLevel(double value) {
        return warningLevel(
                ((value < (warningThresholdLower != null ? warningThresholdLower : Double.MIN_VALUE)) || ((warningThresholdUpper != null ? warningThresholdUpper : Double.MAX_VALUE) < value)),
                ((value < (errorThresholdLower != null ? errorThresholdLower : Double.MIN_VALUE)) || ((errorThresholdUpper != null ? errorThresholdUpper : Double.MAX_VALUE) < value))
        )
    }

    private ThresholdLevel warningLevel(boolean conditionForWarning, boolean conditionForError) {
        if (conditionForError) {
            return ThresholdLevel.ERROR
        } else if (conditionForWarning) {
            return ThresholdLevel.WARNING
        } else {
            return ThresholdLevel.OKAY
        }
    }
}
