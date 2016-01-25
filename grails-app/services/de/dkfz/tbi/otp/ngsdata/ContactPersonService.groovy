package de.dkfz.tbi.otp.ngsdata

import org.springframework.security.access.prepost.PreAuthorize

class ContactPersonService {

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    public ContactPerson createContactPerson(String name, String email, String aspera, Project project) {
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
        addProject(name, project)
        return contactPerson
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    public void addProject(String name, Project project) {
        assert name : "the input name '${name}' must not be null"
        assert project : "project must not be null"
        ContactPerson contactPerson = ContactPerson.findByFullName(name)
        assert contactPerson: "Can't find Person with name '${name}'"
        assert !contactPerson?.projects?.contains(project) : "the person '${name}' is already in project '${project}'"
        project.addToContactPersons(contactPerson)
        assert project.save(flush: true, failOnError: true)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    public void removeProjectAndDeleteContactPerson(String name, Project project) {
        assert name : "the input name '${name}' must not be null"
        assert project : "project must not be null"
        ContactPerson contactPerson = ContactPerson.findByFullName(name)
        assert contactPerson: "Can't find Person with name '${name}'"
        assert contactPerson.projects.contains(project) : "the person '${name}' is not in project '${project}'"
        project.removeFromContactPersons(contactPerson)
        assert project.save(flush: true, failOnError: true)
        if (contactPerson.projects.isEmpty()) {
            deleteContactPerson(contactPerson)
        }
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    public ContactPerson updateName(String oldName, String newName) {
        assert oldName : "the input oldName '${oldName}' must not be null"
        assert newName : "the input newName '${newName}' must not be null"
        assert !ContactPerson.findByFullName(newName) : "a contactPerson with the name '${newName}' already exists"
        ContactPerson contactPerson = ContactPerson.findByFullName(oldName)
        assert contactPerson: "Can't find Person with name '${oldName}'"
        contactPerson.fullName = newName
        assert contactPerson.save(flush: true, failOnError: true)
        return contactPerson
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    public ContactPerson updateEmail(String name, String email) {
        assert name : "the input name '${name}' must not be null"
        assert email : "the input Email '${email}' must not be null"
        ContactPerson contactPerson = ContactPerson.findByFullName(name)
        assert contactPerson: "Can't find Person with name '${name}'"
        contactPerson.email = email
        assert contactPerson.save(flush: true, failOnError: true)
        return contactPerson
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    public ContactPerson updateAspera(String name, String aspera) {
        assert name : "the input name '${name}' must not be null"
        ContactPerson contactPerson = ContactPerson.findByFullName(name)
        assert contactPerson: "Can't find Person with name '${name}'"
        contactPerson.aspera = aspera
        assert contactPerson.save(flush: true, failOnError: true)
        return contactPerson
    }

    private deleteContactPerson(ContactPerson contactPerson) {
        contactPerson.delete(flush: true, failOnError: true)
    }
}
