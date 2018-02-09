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
    double warningThreshold
    double errorThreshold
    Compare compare
    // it can happen that a property has the same name in different qc classes, therefore the class has also to be stored
    String qcClass

    static constraints = {
        project nullable: true
        seqType nullable: true
        qcProperty2 nullable: true
    }

    static enum Compare {
        biggerThanThreshold,
        smallerThanThreshold,
        biggerThanQcProperty2,
        smallerThanQcProperty2,
        biggerThanThresholdFactorExternalValue,
        smallerThanThresholdFactorExternalValue,
    }

    static enum ThresholdLevel {
        OKAY('threshold-okay'),
        WARNING('threshold-warning'),
        ERROR('threshold-error')

        final String styleClass

        ThresholdLevel(String styleClass) {
            this.styleClass = styleClass
        }
    }

    ThresholdLevel qcPassed(QcTrafficLightValue qc, double externalValue = 0) {
        switch (this.compare) {
            case QcThreshold.Compare.biggerThanQcProperty2:
                isBiggerThanQcProperty2(qc)
                break
            case QcThreshold.Compare.smallerThanQcProperty2:
                isSmallerThanQcProperty2(qc)
                break
            case QcThreshold.Compare.biggerThanThreshold:
                isBiggerThanThreshold(qc)
                break
            case QcThreshold.Compare.smallerThanThreshold:
                isSmallerThanThreshold(qc)
                break
            case QcThreshold.Compare.biggerThanThresholdFactorExternalValue:
                isBiggerThanThresholdFactorExternalValue(qc, externalValue)
                break
            case QcThreshold.Compare.smallerThanThresholdFactorExternalValue:
                isSmallerThanThresholdFactorExternalValue(qc, externalValue)
                break
            default: throw new RuntimeException("No other comparison is defined yet")
        }
    }

    private ThresholdLevel isBiggerThanQcProperty2(QcTrafficLightValue qc) {
        if (qc."${qcProperty1}" == null || qc."${qcProperty2}" == null) {
            return ThresholdLevel.OKAY
        }
        return warningLevel((qc."${qcProperty1}" - qc."${qcProperty2}") > warningThreshold, (qc."${qcProperty1}" - qc."${qcProperty2}") > errorThreshold)
    }

    private ThresholdLevel isSmallerThanQcProperty2(QcTrafficLightValue qc) {
        if (qc."${qcProperty1}" == null || qc."${qcProperty2}" == null) {
            return ThresholdLevel.OKAY
        }
        return warningLevel((qc."${qcProperty2}" - qc."${qcProperty1}") > warningThreshold, (qc."${qcProperty2}" - qc."${qcProperty1}") > errorThreshold)
    }

    private ThresholdLevel isBiggerThanThreshold(QcTrafficLightValue qc) {
        if (qc."${qcProperty1}" == null) {
            return ThresholdLevel.OKAY
        }
        return warningLevel(qc."${qcProperty1}" > warningThreshold, qc."${qcProperty1}" > errorThreshold)
    }

    private ThresholdLevel isSmallerThanThreshold(QcTrafficLightValue qc) {
        if (qc."${qcProperty1}" == null) {
            return ThresholdLevel.OKAY
        }
        return warningLevel(qc."${qcProperty1}" < warningThreshold, qc."${qcProperty1}" < errorThreshold)
    }

    private ThresholdLevel isBiggerThanThresholdFactorExternalValue(QcTrafficLightValue qc, double externalValue) {
        if (qc."${qcProperty1}" == null) {
            return ThresholdLevel.OKAY
        }
        return warningLevel(qc."${qcProperty1}" > (warningThreshold * externalValue), qc."${qcProperty1}" > (errorThreshold * externalValue))
    }

    private ThresholdLevel isSmallerThanThresholdFactorExternalValue(QcTrafficLightValue qc, double externalValue) {
        if (qc."${qcProperty1}" == null) {
            return ThresholdLevel.OKAY
        }
        return warningLevel (qc."${qcProperty1}" < (warningThreshold * externalValue), qc."${qcProperty1}" < (errorThreshold * externalValue))
    }

    private static ThresholdLevel warningLevel(boolean conditionForWarning, boolean conditionForError) {
        if (conditionForError) {
            return ThresholdLevel.ERROR
        } else if (conditionForWarning) {
            return ThresholdLevel.WARNING
        } else {
            return ThresholdLevel.OKAY
        }
    }
}
