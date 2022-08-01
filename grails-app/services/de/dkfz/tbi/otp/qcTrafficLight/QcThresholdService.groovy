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

import grails.gorm.transactions.Transactional
import grails.validation.ValidationException
import groovy.transform.Canonical
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.Errors

import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.utils.FormatHelper

@Transactional
class QcThresholdService {

    def <T extends QcTrafficLightValue> ThresholdColorizer<T> createThresholdColorizer(Project project, SeqType seqType, Class<T> qcClass) {
        new ThresholdColorizer(project, seqType, qcClass)
    }

    static class ThresholdColorizer<T extends QcTrafficLightValue> {
        private final Map<String, QcThreshold> thresholdList
        private final Class<QcTrafficLightValue> qcClass

        ThresholdColorizer(Project project, SeqType seqType, Class<T> qcClass) {
            this.thresholdList = QcThreshold.createCriteria().list {
                eq("qcClass", qcClass.name)
                eq("seqType", seqType)
                or {
                    eq("project", project)
                    isNull("project")
                }
            }.groupBy { it.qcProperty1 }.collect {
                it.value.find { it.project == project && it.seqType == seqType } ?:
                        it.value.find { it.project == null && it.seqType == seqType }
            }.collectEntries { QcThreshold threshold ->
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

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    List<ClassWithThreshold> getClassesWithProperties() {
        List<QcThreshold> thr = QcThreshold.createCriteria().list {
            isNull("project")
        }

        List<Class> qcTrafficLightClasses = QcThreshold.validQcClass
        List<ClassWithThreshold> classesWithProperties = qcTrafficLightClasses.collect { Class clasz ->
            new ClassWithThreshold(clasz: clasz, availableThresholdProperties: QcThreshold.getValidQcPropertyForQcClass(clasz.name))
        }.sort { it.clasz.simpleName }

        classesWithProperties.each { ClassWithThreshold cl ->
            cl.existingThresholds = thr.findAll { it.qcClass == cl.clasz.name }.sort { it.qcProperty1 }
        }
        return classesWithProperties
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#project, 'OTP_READ_ACCESS')")
    List<ClassWithThresholds> getClassesWithPropertiesForProjectAndSeqTypes(Project project, List<SeqType> seqTypes) {
        assert seqTypes: "No seqTypes given"

        List<QcThreshold> thresholds = QcThreshold.createCriteria().list {
            or {
                eq("project", project)
                isNull("project")
            }
        }
        List<Class> qcTrafficLightClasses = QcThreshold.validQcClass
        List<ClassWithThresholds> classesWithProperties = qcTrafficLightClasses.collect { Class clasz ->
            new ClassWithThresholds(
                    clasz: clasz,
                    availableThresholdProperties: QcThreshold.getValidQcPropertyForQcClass(clasz.name),
                    existingThresholds: [],
            )
        }.sort { it.clasz.simpleName }

        classesWithProperties.each { ClassWithThresholds cl ->
            cl.availableThresholdProperties.each { String property ->
                seqTypes.each { SeqType seqType ->
                    QcThreshold projectThreshold = thresholds.find {
                        it.qcClass == cl.clasz.name && it.qcProperty1 == property && it.seqType == seqType && it.project == project
                    }
                    QcThreshold defaultThreshold = thresholds.find {
                        it.qcClass == cl.clasz.name && it.qcProperty1 == property && it.seqType == seqType && it.project == null
                    }

                    if (projectThreshold || defaultThreshold) {
                        cl.existingThresholds.add(new BothQcThresholds(
                                defaultExistingThresholds: defaultThreshold,
                                projectExistingThresholds: projectThreshold,
                                seqType: projectThreshold?.seqType ?: defaultThreshold?.seqType,
                                property: projectThreshold?.qcProperty1 ?: defaultThreshold.qcProperty1,
                        ))
                    }
                }
            }
        }
        return classesWithProperties
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#project, 'OTP_READ_ACCESS')")
    Errors createThreshold(Project project, String clasz, String property, SeqType seqType, QcThreshold.ThresholdStrategy condition,
                           Double errorThresholdLower, Double warningThresholdLower,
                           Double warningThresholdUpper, Double errorThresholdUpper, String property2) {
        String className = QcThreshold.convertShortToLongName(clasz)

        try {
            new QcThreshold(
                    project: project,
                    qcClass: className,
                    qcProperty1: property,
                    seqType: seqType,
                    compare: condition,
                    errorThresholdLower: errorThresholdLower,
                    warningThresholdLower: warningThresholdLower,
                    warningThresholdUpper: warningThresholdUpper,
                    errorThresholdUpper: errorThresholdUpper,
                    qcProperty2: property2,
            ).save(flush: true)
            return null
        } catch (ValidationException e) {
            return e.errors
        }
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#qcThreshold?.project, 'OTP_READ_ACCESS')")
    Errors updateThreshold(QcThreshold qcThreshold, QcThreshold.ThresholdStrategy condition,
                           Double errorThresholdLower, Double warningThresholdLower,
                           Double warningThresholdUpper, Double errorThresholdUpper, String property2) {
        assert qcThreshold
        try {
            qcThreshold.compare = condition
            qcThreshold.errorThresholdLower = errorThresholdLower
            qcThreshold.warningThresholdLower = warningThresholdLower
            qcThreshold.warningThresholdUpper = warningThresholdUpper
            qcThreshold.errorThresholdUpper = errorThresholdUpper
            qcThreshold.qcProperty2 = property2
            qcThreshold.save(flush: true)
        } catch (ValidationException e) {
            return e.errors
        }
        return null
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#qcThreshold?.project, 'OTP_READ_ACCESS')")
    Errors deleteThreshold(QcThreshold qcThreshold) {
        assert qcThreshold
        try {
            qcThreshold.delete(flush: true)
        } catch (ValidationException e) {
            return e.errors
        }
        return null
    }
}

@Canonical
class TableCellValue implements Serializable {
    String value
    WarnColor warnColor
    String link
    String tooltip
    Icon icon
    String status
    long id
    String linkTarget
    boolean archived

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

@Canonical
class ClassWithThreshold {
    Class clasz
    List<QcThreshold> existingThresholds
    List<String> availableThresholdProperties
}

@Canonical
class ClassWithThresholds {
    Class clasz
    List<BothQcThresholds> existingThresholds
    List<String> availableThresholdProperties
}

@Canonical
class BothQcThresholds {
    SeqType seqType
    String property
    QcThreshold defaultExistingThresholds
    QcThreshold projectExistingThresholds
}
