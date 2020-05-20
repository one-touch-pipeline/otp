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

import org.springframework.validation.Errors

import de.dkfz.tbi.otp.FlashMessage
import de.dkfz.tbi.otp.ProjectSelectionService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project

class QcThresholdController {
    QcThresholdService qcThresholdService
    ProjectSelectionService projectSelectionService

    static allowedMethods = [
            defaultConfiguration: "GET",
            projectConfiguration: "GET",
            create: "POST",
            update: "POST",
            delete: "POST",
    ]


    def defaultConfiguration() {
        List<SeqType> seqTypes = SeqTypeService.allProcessableSeqTypes

        List<ClassWithThreshold> classesWithProperties = qcThresholdService.getClassesWithProperties()

        return [
                classesWithProperties: classesWithProperties,
                seqTypes             : seqTypes,
                compare              : QcThreshold.ThresholdStrategy,
        ]
    }

    def projectConfiguration() {
        Project project = projectSelectionService.selectedProject

        List<SeqType> seqTypes = SeqTypeService.allProcessableSeqTypes

        List<ClassWithThresholds> classesWithProperties = qcThresholdService.getClassesWithPropertiesForProjectAndSeqTypes(project, seqTypes)

        return [
                classesWithProperties: classesWithProperties,
                seqTypes             : seqTypes,
                compare              : QcThreshold.ThresholdStrategy,
        ]
    }


    def create(CreateCommand cmd) {
        checkErrorAndCallMethod(cmd, {
            qcThresholdService.createThreshold(
                    cmd.forProject ? projectSelectionService.requestedProject : null,
                    cmd.className, cmd.property, cmd.seqType, cmd.condition,
                    cmd.actualErrorThresholdLower, cmd.actualWarningThresholdLower,
                    cmd.actualWarningThresholdUpper, cmd.actualErrorThresholdUpper,
                    cmd.property2 ?: null,
            )
        })
    }

    def update(UpdateCommand cmd) {
        checkErrorAndCallMethod(cmd, {
            qcThresholdService.updateThreshold(cmd.qcThreshold, cmd.condition,
                    cmd.actualErrorThresholdLower, cmd.actualWarningThresholdLower,
                    cmd.actualWarningThresholdUpper, cmd.actualErrorThresholdUpper,
                    cmd.property2 ?: null,
            )
        })
    }

    def delete(DeleteCommand cmd) {
        checkErrorAndCallMethod(cmd, { qcThresholdService.deleteThreshold(cmd.qcThreshold) })
    }


    private void checkErrorAndCallMethod(Object cmd, Closure<Errors> method) {
        if (cmd.hasErrors()) {
            flash.message = new FlashMessage(g.message(code: "qcThreshold.store.fail") as String, cmd.getErrors())
        } else {
            Errors errors = method()
            if (errors) {
                flash.message = new FlashMessage(g.message(code: "qcThreshold.store.fail") as String, errors)
            } else {
                flash.message = new FlashMessage(g.message(code: "qcThreshold.store.succ") as String)
            }
        }
        if (cmd.forProject) {
            redirect(action: "projectConfiguration")
        } else {
            redirect(action: "defaultConfiguration")
        }
    }
}

class AbstractCommand {
    QcThreshold.ThresholdStrategy condition
    Double actualWarningThresholdLower
    Double actualWarningThresholdUpper
    Double actualErrorThresholdLower
    Double actualErrorThresholdUpper
    String property2

    void setWarningThresholdLower(String s) {
        if (s) {
            this.actualWarningThresholdLower = Double.parseDouble(s)
        }
    }
    void setWarningThresholdUpper(String s) {
        if (s) {
            this.actualWarningThresholdUpper = Double.parseDouble(s)
        }
    }
    void setErrorThresholdLower(String s) {
        if (s) {
            this.actualErrorThresholdLower = Double.parseDouble(s)
        }
    }
    void setErrorThresholdUpper(String s) {
        if (s) {
            this.actualErrorThresholdUpper = Double.parseDouble(s)
        }
    }

    static constraints = {
        actualWarningThresholdLower nullable: true
        actualWarningThresholdUpper nullable: true
        actualErrorThresholdLower nullable: true
        actualErrorThresholdUpper nullable: true
        property2 nullable: true
    }
}

class CreateCommand extends AbstractCommand {
    Boolean forProject
    String className
    String property
    SeqType seqType

    static constraints = {
        forProject nullable: true
        property blank: false
    }
}

class UpdateCommand extends AbstractCommand {
    QcThreshold qcThreshold
    Boolean forProject
    static constraints = {
        forProject nullable: true
    }
}

class DeleteCommand {
    QcThreshold qcThreshold
    Boolean forProject
    static constraints = {
        forProject nullable: true
    }
}
