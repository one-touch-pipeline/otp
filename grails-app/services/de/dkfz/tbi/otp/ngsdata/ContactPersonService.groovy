package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.utils.CollectionUtils
import org.springframework.security.access.prepost.PreAuthorize

class ContactPersonService {

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    public ContactPerson createContactPerson(String name, String email, String aspera, Project project, ContactPersonRole role) {
        assert name : "the input name '${name}' must not be null"
        assert email : "the input email '${email}' must not be null"
        assert project : "project must not be null"
        assert !ContactPerson.findByFullName(name) : "The ContactPerson '${name}' already exists"
        ContactPerson contactPerson = new ContactPerson(
                fullName: name,
                email: email,
                aspera: aspera,
        )
        assert contactPerson.save(flush: true, failOnError: true)
        addContactPersonToProject(name, project, role)
        return contactPerson
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    public void addContactPersonToProject(String fullName, Project project, ContactPersonRole contactPersonRole) {
        assert fullName : "the input name '${fullName}' must not be null"
        assert project : "project must not be null"
        ContactPerson contactPerson = ContactPerson.findByFullName(fullName)
        assert contactPerson: "Can't find Person with name '${fullName}'"
        assert !ProjectContactPerson.findByContactPersonAndProject(contactPerson, project) : "the person '${fullName}' is already in project '${project}'"
        ProjectContactPerson projectContactPerson = new ProjectContactPerson(
                contactPerson: contactPerson,
                project: project,
                contactPersonRole: contactPersonRole,
        )
        assert projectContactPerson.save(flush: true, failOnError: true)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    public void removeContactPersonFromProject(ProjectContactPerson projectContactPerson) {
        ContactPerson contactPerson = projectContactPerson.getContactPerson()
        projectContactPerson.delete(flush: true, failOnError: true)
        if (!ProjectContactPerson.findByContactPerson(contactPerson)) {
            deleteContactPerson(contactPerson)
        }
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    public ContactPerson updateName(ContactPerson contactPerson, String newName) {
        assert newName : "the input newName '${newName}' must not be null"
        assert !ContactPerson.findByFullName(newName) : "a contactPerson with the name '${newName}' already exists"
        assert contactPerson: "Can't find Person with name '${contactPerson.fullName}'"
        contactPerson.fullName = newName
        assert contactPerson.save(flush: true, failOnError: true)
        return contactPerson
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    public ContactPerson updateEmail(ContactPerson contactPerson, String email) {
        assert email : "the input Email '${email}' must not be null"
        assert contactPerson: "Can't find Person with name '${contactPerson.fullName}'"
        contactPerson.email = email
        assert contactPerson.save(flush: true, failOnError: true)
        return contactPerson
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    public ContactPerson updateAspera(ContactPerson contactPerson, String aspera) {
        assert contactPerson: "Can't find Person with name '${contactPerson.fullName}'"
        contactPerson.aspera = aspera
        assert contactPerson.save(flush: true, failOnError: true)
        return contactPerson
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    public void updateRole(ProjectContactPerson projectContactPerson, ContactPersonRole role) {
        assert projectContactPerson.contactPerson : "contact person must not be null"
        assert projectContactPerson.project : "project must not be null"
        projectContactPerson.contactPersonRole = role
        projectContactPerson.save(flush: true, failOnError: true)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    public List<ContactPerson> getAllContactPersons() {
        return ContactPerson.list(sort: "fullName", order: "asc")
    }

    private deleteContactPerson(ContactPerson contactPerson) {
        contactPerson.delete(flush: true, failOnError: true)
    }
}
