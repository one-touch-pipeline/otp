package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.administration.UserService
import de.dkfz.tbi.otp.security.User
import grails.converters.*
import org.springframework.validation.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

class ProjectUserController {

    ProjectService projectService
    ProjectSelectionService projectSelectionService
    ProjectOverviewService projectOverviewService
    UserService userService

    def index() {
        List<Project> projects = projectService.getAllProjects()
        ProjectSelection selection = projectSelectionService.getSelectedProject()

        Project project
        if (selection.projects.size() == 1) {
            project = selection.projects.first()
        } else {
            project = projects.first()
        }

        project = exactlyOneElement(Project.findAllByName(project.name, [fetch: [projectCategories: 'join', projectGroup: 'join']]))

        List<ProjectContactPerson> projectContactPersons = ProjectContactPerson.findAllByProject(project)

        List accessPersons = projectOverviewService.getAccessPersons(project)

        return [
                projects: projects,
                project: project,
                projectContactPersons: projectContactPersons,
                accessPersons: accessPersons,
                hasErrors: params.hasErrors,
                message: params.message,
        ]
    }

    JSON updateName(UpdateUserRealNameCommand cmd) {
        checkErrorAndCallMethod(cmd, { userService.updateRealName(cmd.user, cmd.newName) })
    }

    JSON updateEmail(UpdateUserEmailCommand cmd) {
        checkErrorAndCallMethod(cmd, { userService.updateEmail(cmd.user, cmd.newEmail) })
    }

    JSON updateAspera(UpdateUserAsperaCommand cmd) {
        checkErrorAndCallMethod(cmd, { userService.updateAsperaAccount(cmd.user, cmd.newAspera) })
    }

    private void checkErrorAndCallMethod(Serializable cmd, Closure method) {
        Map data
        if (cmd.hasErrors()) {
            data = getErrorData(cmd.errors.getFieldError())
        } else {
            method()
            data = [success: true]
        }
        render data as JSON
    }

    private Map getErrorData(FieldError errors) {
        return [success: false, error: "'" + errors.getRejectedValue() + "' is not a valid value for '" + errors.getField() + "'. Error code: '" + errors.code + "'"]
    }
}

class UpdateUserRealNameCommand implements Serializable {
    User user
    String newName
    static constraints = {
        newName(blank: false, validator: {val, obj ->
            if (val == obj.user?.realName) {
                return 'No Change'
            }
        })
    }
    void setValue(String value) {
        this.newName = value?.trim()?.replaceAll(" +", " ")
    }
}

class UpdateUserEmailCommand implements Serializable {
    User user
    String newEmail
    static constraints = {
        newEmail(nullable: false, email:true, blank: false, validator: {val, obj ->
            if (val == obj.user?.email) {
                return 'No Change'
            } else if (User.findByEmail(val)) {
                return 'Duplicate'
            }
        })
    }
    void setValue(String value) {
        this.newEmail = value?.trim()?.replaceAll(" +", " ")
    }
}

class UpdateUserAsperaCommand implements Serializable {
    User user
    String newAspera
    static constraints = {
        newAspera(blank: true, nullable: true, validator: {val, obj ->
            if (val == obj.user?.asperaAccount) {
                return 'No Change'
            }
        })
    }
    void setValue(String value) {
        this.newAspera = value?.trim()?.replaceAll(" +", " ") ?: null
    }
}
