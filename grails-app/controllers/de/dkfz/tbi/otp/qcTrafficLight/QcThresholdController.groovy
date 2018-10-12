package de.dkfz.tbi.otp.qcTrafficLight

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.ngsdata.*
import org.springframework.validation.*

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
        ProjectSelection selection = projectSelectionService.getSelectedProject()
        Project project = projectSelectionService.getProjectFromProjectSelectionOrAllProjects(selection)

        List<SeqType> seqTypes = SeqTypeService.allProcessableSeqTypes

        List<ClassWithThresholds> classesWithProperties = qcThresholdService.getClassesWithPropertiesForProjectAndSeqTypes(project, seqTypes)

        return [
                classesWithProperties: classesWithProperties,
                seqTypes             : seqTypes,
                compare              : QcThreshold.ThresholdStrategy,
                project              : project,
        ]
    }


    def create(CreateCommand cmd) {
        checkErrorAndCallMethod(cmd, {
            qcThresholdService.createThreshold(cmd.project ?: null, cmd.className, cmd.property, cmd.seqType, cmd.condition,
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
            flash.message = g.message(code: "qcThreshold.store.fail")
            flash.errors = cmd.getErrors()
        } else {
            Errors errors = method()
            if (errors) {
                flash.message = g.message(code: "qcThreshold.store.fail")
                flash.errors = errors
            } else {
                flash.message = g.message(code: "qcThreshold.store.succ")
            }
        }
        if (cmd.project) {
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
    Project project
    String className
    String property
    SeqType seqType

    static constraints = {
        project nullable: true
        property blank: false
    }
}

class UpdateCommand extends AbstractCommand {
    QcThreshold qcThreshold
    Boolean project
    static constraints = {
        project nullable: true
    }
}

class DeleteCommand {
    QcThreshold qcThreshold
    Boolean project
    static constraints = {
        project nullable: true
    }
}
