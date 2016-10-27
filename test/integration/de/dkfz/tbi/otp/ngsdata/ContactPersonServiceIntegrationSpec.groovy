package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.testing.UserAndRoles
import spock.lang.Specification
import grails.plugin.springsecurity.SpringSecurityUtils

class ContactPersonServiceIntegrationSpec extends Specification implements UserAndRoles{
    ContactPersonService contactPersonService = new ContactPersonService()
    ProjectContactPerson projectContactPerson = new ProjectContactPerson()

    def setup() {
        createUserAndRoles()
        Project project01 = DomainFactory.createProject(name: "project01")
        DomainFactory.createProject(name: "project02")
        DomainFactory.createProject(name: "project03")
        DomainFactory.createContactPersonRole(name: "PI")
        DomainFactory.createContactPersonRole(name: "OTHER")
        ContactPerson contactPerson01 = createContactPerson("TestName", "test@dkfz.de", "")
        createContactPerson("TestName2", "test2@dkfz.de", "")
        createContactPerson("TestName3", "test3@dkfz.de", "")
        projectContactPerson = DomainFactory.createProjectContactPerson(project: project01, contactPerson: contactPerson01)
    }

    void "test createContactPerson valid input"() {
        given:
        ContactPerson contactPerson

        when:
        SpringSecurityUtils.doWithAuth("operator") {
            contactPersonService.createContactPerson(name, email, aspera, getProject("03"), getContactPersonRole())
        }
        contactPerson = ContactPerson.findByFullName(name)

        then:
        ProjectContactPerson.findByContactPerson(contactPerson).getProject() == getProject("03")

        where:
        name        | email             | aspera
        "newUser"   | "newUser@dkfz.de" | "newUser"
        "newUser"   | "newUser@dkfz.de" | ""
    }

    void "test createContactPerson invalid input"() {
        expect:
        SpringSecurityUtils.doWithAuth("operator") {
            shouldFail(AssertionError) {
                contactPersonService.createContactPerson(name, email, "", getProject("01"), getContactPersonRole())
            }
        }

        where:
        name        | email
        and: 'empty name'
        ""          | "test@dkfz.de"
        and: 'Duplicate name'
        "TestName"  | "test@dkfz.de"
        and: 'empty email'
        "newUser"   | ""
    }

    void "test addContactPersonToProject valid input"() {
        given:
        ContactPerson contactPerson = ContactPerson.findByFullName("TestName2")
        when:
        SpringSecurityUtils.doWithAuth("operator") {
            contactPersonService.addContactPersonToProject("TestName2", getProject("02"), getContactPersonRole())
        }

        then:
        ProjectContactPerson.findByContactPersonAndProject(contactPerson, getProject("02"))
    }

    void "test addContactPersonToProject invalid input"() {
        expect:
        SpringSecurityUtils.doWithAuth("operator") {
            shouldFail(AssertionError) {
                contactPersonService.addContactPersonToProject(name, getProject("01"), getContactPersonRole())
            }
        }

        where:
        name        | _
        and: 'empty name'
        ""          | _
        and: 'Duplicate name'
        "TestName"  | _
        and: 'Unknown name'
        "newUser"   | _
    }

    void "test removeContactPersonFromProject valid input"() {
        given:
        ContactPerson contactPerson = ContactPerson.findByFullName("TestName")
        ContactPersonRole contactPersonRole = ContactPersonRole.findByName("PI")
        ProjectContactPerson projectContactPerson = DomainFactory.createProjectContactPerson(project: getProject("01"),
                contactPersonRole: contactPersonRole, contactPerson: contactPerson)

        when:
        SpringSecurityUtils.doWithAuth("operator") {
            contactPersonService.removeContactPersonFromProject(projectContactPerson)
        }

        then:
        !ProjectContactPerson.findByProjectAndContactPersonAndContactPersonRole(getProject("01"), contactPerson, contactPersonRole)
    }

    void "test updateName valid input"() {
        given:
        ContactPerson contactPerson = ContactPerson.findByFullName("TestName")

        when:
        SpringSecurityUtils.doWithAuth("operator") {
            contactPersonService.updateName(contactPerson, "newName")
        }

        then:
        contactPerson == ContactPerson.findByFullName("newName")
        !ContactPerson.findByFullName("TestName")
    }

    void "test updateName invalid input no name"() {
        given:
        ContactPerson contactPerson = ContactPerson.findByFullName("TestName")

        expect:
        SpringSecurityUtils.doWithAuth("operator") {
            shouldFail(AssertionError){
                contactPersonService.updateName(contactPerson, "")
            }
        }
    }

    void "test updateName invalid input duplicate name"() {
        given:
        ContactPerson contactPerson = ContactPerson.findByFullName("TestName")

        expect:
        SpringSecurityUtils.doWithAuth("operator") {
            shouldFail(AssertionError){
                contactPersonService.updateName(contactPerson, "TestName")
            }
        }
    }

    void "test updateEmail valid input"() {
        given:
        ContactPerson contactPerson = ContactPerson.findByFullName("TestName")

        when:
        SpringSecurityUtils.doWithAuth("operator") {
            contactPersonService.updateEmail(contactPerson, "newUser@dkfz.de")
        }

        then:
        contactPerson.email == "newUser@dkfz.de"
    }

    void "test updateEmail invalid input"() {
        given:
        ContactPerson contactPerson = ContactPerson.findByFullName("TestName")

        expect:
        SpringSecurityUtils.doWithAuth("operator") {
            shouldFail(AssertionError){
                contactPersonService.updateEmail(contactPerson, "")
            }
        }
    }

    void "test updateAspera valid input"() {
        given:
        ContactPerson contactPerson = ContactPerson.findByFullName("TestName")

        when:
        SpringSecurityUtils.doWithAuth("operator") {
            contactPersonService.updateAspera(contactPerson, "newUser")
        }

        then:
        contactPerson.aspera == "newUser"
    }

    void "test updateRole valid input"() {
        given:
        ContactPerson contactPerson = ContactPerson.findByFullName("TestName")
        Project project = Project.findByName("project03")
        ContactPersonRole contactPersonRole = ContactPersonRole.findByName("OTHER")
        ProjectContactPerson projectContactPerson = DomainFactory.createProjectContactPerson(contactPerson : contactPerson,
                project: project, contactPersonRole: contactPersonRole)

        when:
        SpringSecurityUtils.doWithAuth("operator") {
            contactPersonService.updateRole(projectContactPerson, getContactPersonRole())
        }

        then:
        projectContactPerson.contactPersonRole.name == "PI"
    }

    private static ContactPerson createContactPerson(String name, String email, String aspera) {
        ContactPerson contactPerson = new ContactPerson(
                fullName: name,
                email: email,
                aspera: aspera
        )
        assert contactPerson.save(flush: true, failOnError: true)
        return contactPerson
    }

    private Project getProject(String name) {
        return Project.findByName("project"+name)
    }

    private static ContactPersonRole getContactPersonRole() {
        return ContactPersonRole.findByName("PI")
    }
}
