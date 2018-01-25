package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.*
import grails.converters.*
import org.springframework.validation.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

class ProjectMemberController {

    ProjectService projectService
    ProjectSelectionService projectSelectionService
    ProjectOverviewService projectOverviewService
    ContactPersonService contactPersonService


    def index() {
        List<Project> projects = projectService.getAllProjects()
        if (!projects) {
            return [
                    projects: projects,
            ]
        }

        ProjectSelection selection = projectSelectionService.getSelectedProject()

        Project project
        if (selection.projects.size() == 1) {
            project = selection.projects.first()
        } else {
            project = projects.first()
        }

        project = exactlyOneElement(Project.findAllByName(project.name, [fetch: [projectCategories: 'join', projectGroup: 'join']]))

        List<ProjectContactPerson> projectContactPersons = ProjectContactPerson.findAllByProject(project)

        List<String> contactPersonRoles = [''] + ContactPersonRole.findAll()*.name

        List accessPersons = projectOverviewService.getAccessPersons(project)

        return [
                projects: projects,
                project: project,
                projectContactPersons: projectContactPersons,
                roleDropDown: contactPersonRoles,
                accessPersons: accessPersons,
                hasErrors: params.hasErrors,
                message: params.message,
        ]
    }

    static ContactPersonRole getContactPersonRoleByName(String roleName) {
        return roleName.isEmpty() ? null : exactlyOneElement(ContactPersonRole.findAllByName(roleName))
    }

    JSON createContactPersonOrAddToProject(UpdateContactPersonCommand cmd){
        if (cmd.hasErrors()) {
            Map data = getErrorData(cmd.errors.getFieldError())
            render data as JSON
            return
        }
        if (ContactPerson.findByFullName(cmd.name)) {
            checkErrorAndCallMethod(cmd, { contactPersonService.addContactPersonToProject(cmd.name, cmd.project, cmd.contactPersonRole) })
        } else {
            checkErrorAndCallMethod(cmd, { contactPersonService.createContactPerson(cmd.name, cmd.email, cmd.aspera, cmd.project, cmd.contactPersonRole) })
        }
    }

    JSON deleteContactPersonOrRemoveProject(UpdateDeleteContactPersonCommand cmd){
        checkErrorAndCallMethod(cmd, { contactPersonService.removeContactPersonFromProject(cmd.projectContactPerson) })
    }

    JSON updateName(UpdateContactPersonNameCommand cmd) {
        checkErrorAndCallMethod(cmd, { contactPersonService.updateName(cmd.contactPerson, cmd.newName) })
    }

    JSON updateEmail(UpdateContactPersonEmailCommand cmd) {
        checkErrorAndCallMethod(cmd, { contactPersonService.updateEmail(cmd.contactPerson, cmd.newEmail) })
    }

    JSON updateAspera(UpdateContactPersonAsperaCommand cmd) {
        checkErrorAndCallMethod(cmd, { contactPersonService.updateAspera(cmd.contactPerson, cmd.newAspera) })
    }

    JSON updateRole(UpdateContactPersonRoleCommand cmd) {
        checkErrorAndCallMethod(cmd, { contactPersonService.updateRole(cmd.projectContactPerson, getContactPersonRoleByName(cmd.newRole)) })
    }

    JSON contactPersons() {
        List<String> contactPersons = contactPersonService.getAllContactPersons()*.fullName
        render contactPersons as JSON
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

class UpdateContactPersonCommand implements Serializable {
    String name
    String email
    String aspera
    ContactPersonRole contactPersonRole
    Project project
    static constraints = {
        name(blank: false, validator: {val, obj ->
            ContactPerson contactPerson = ContactPerson.findByFullName(val)
            if (ProjectContactPerson.findByContactPersonAndProject(contactPerson, obj.project)) {
                return 'Duplicate'
            } else if (contactPerson && obj.email != "" && obj.email != contactPerson.email) {
                return 'There is already a Person with \'' + contactPerson.fullName + '\' as Name and \'' + contactPerson.email + '\' as Email in the Database'
            }})
        email(email:true, validator: {val, obj ->
            if (!ContactPerson.findByFullName(obj.name) && val == "") {
                return 'Empty'
            }})
        aspera(blank: true)
        project(nullable: false)
        contactPersonRole(nullable: true)
    }
    void setName(String name) {
        this.name = name?.trim()?.replaceAll(" +", " ")
    }
    void setEmail(String email) {
        this.email = email?.trim()?.replaceAll(" +", " ")
    }
    void setAspera(String aspera) {
        this.aspera = aspera?.trim()?.replaceAll(" +", " ")
    }
    void setRole(String role) {
        this.contactPersonRole = ProjectMemberController.getContactPersonRoleByName(role)
    }
}

class UpdateDeleteContactPersonCommand implements Serializable {
    ProjectContactPerson projectContactPerson
}

class UpdateContactPersonNameCommand implements Serializable {
    ContactPerson contactPerson
    String newName
    static constraints = {
        newName(blank: false, validator: {val, obj ->
            if (val == obj.contactPerson?.fullName) {
                return 'No Change'
            } else if (ContactPerson.findByFullName(val)) {
                return 'Duplicate'
            }
        })
    }
    void setValue(String value) {
        this.newName = value?.trim()?.replaceAll(" +", " ")
    }
}

class UpdateContactPersonEmailCommand implements Serializable {
    ContactPerson contactPerson
    String newEmail
    static constraints = {
        newEmail(email:true, blank: false ,validator: {val, obj ->
            if (val == obj.contactPerson?.email) {
                return 'No Change'
            } else if (ContactPerson.findByEmail(val)) {
                return 'Duplicate'
            }
        })
    }
    void setValue(String value) {
        this.newEmail = value?.trim()?.replaceAll(" +", " ")
    }
}

class UpdateContactPersonAsperaCommand implements Serializable {
    ContactPerson contactPerson
    String newAspera
    static constraints = {
        newAspera(blank: true, validator: {val, obj ->
            if (val == obj.contactPerson?.aspera) {
                return 'No Change'
            }
        })
    }
    void setValue(String value) {
        this.newAspera = value?.trim()?.replaceAll(" +", " ")
    }
}

class UpdateContactPersonRoleCommand implements Serializable {
    ProjectContactPerson projectContactPerson
    String newRole
    void setValue(String value) {
        this.newRole = value
    }
}
