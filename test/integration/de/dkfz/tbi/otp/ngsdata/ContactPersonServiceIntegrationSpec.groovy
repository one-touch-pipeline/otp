package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.testing.UserAndRoles
import spock.lang.Specification
import spock.lang.Unroll
import grails.buildtestdata.mixin.Build
import grails.plugin.springsecurity.SpringSecurityUtils

class ContactPersonServiceIntegrationSpec extends Specification implements UserAndRoles{
    ContactPersonService contactPersonService = new ContactPersonService()

    def setup() {
        createUserAndRoles()
        Project project01 = DomainFactory.createProject(name: "project01")
        Project project02 = DomainFactory.createProject(name: "project02")
        DomainFactory.createProject(name: "project03")
        ContactPerson contactPerson01 = createContactPerson("TestName", "test@dkfz.de", "", project01)
        addProject(project02, contactPerson01)
        createContactPerson("TestName2", "test2@dkfz.de", "", project02)
    }

    void "test createContactPerson valid input"() {
        given:
        ContactPerson contactPerson

        when:
        SpringSecurityUtils.doWithAuth("operator") {
            contactPerson = contactPersonService.createContactPerson(name, email, aspera, getProject("01"))
        }

        then:
        contactPerson == ContactPerson.findByFullName(name)
        contactPerson.projects.contains(getProject("01"))

        where:
        name        | email             | aspera
        "newUser"   | "newUser@dkfz.de" | "newUser"
        "newUser"   | "newUser@dkfz.de" | ""
    }

    void "test createContactPerson invalid input"() {
        expect:
        SpringSecurityUtils.doWithAuth("operator") {
            shouldFail(AssertionError){
                contactPersonService.createContactPerson(name, email, "", getProject("01"))
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

    void "test addProject valid input"() {
        given:
        ContactPerson contactPerson = ContactPerson.findByFullName("TestName")

        when:
        SpringSecurityUtils.doWithAuth("operator") {
            contactPersonService.addProject("TestName", getProject("03"))
        }

        then:
        contactPerson.projects.contains(getProject("03"))
    }

    void "test addProject invalid input"() {
        expect:
        SpringSecurityUtils.doWithAuth("operator") {
            shouldFail(AssertionError){
                contactPersonService.addProject(name, getProject("01"))
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

    void "test removeProjectAndDeleteContactPerson valid input"() {
        given:
        ContactPerson contactPerson = ContactPerson.findByFullName("TestName")

        when:
        SpringSecurityUtils.doWithAuth("operator") {
            contactPersonService.removeProjectAndDeleteContactPerson("TestName", getProject("02"))
        }

        then:
        !contactPerson.projects.contains(getProject("02"))
    }

    void "test removeProjectAndDeleteContactPerson valid input and delete ContactPerson"() {

        when:
        SpringSecurityUtils.doWithAuth("operator") {
            contactPersonService.removeProjectAndDeleteContactPerson("TestName2", getProject("02"))
        }

        then:
        !ContactPerson.findByFullName("TestName2")
    }

    void "test removeProjectAndDeleteContactPerson invalid input"() {
        expect:
        SpringSecurityUtils.doWithAuth("operator") {
            shouldFail(AssertionError){
                contactPersonService.removeProjectAndDeleteContactPerson(name, getProject("01"))
            }
        }

        where:
        name        | _
        and: 'empty name'
        ""          | _
        and: 'Unknown name'
        "newUser"   | _
        and: 'Not on the Project'
        "TestName2"   | _
    }

    void "test updateName valid input"() {
        given:
        ContactPerson contactPerson = ContactPerson.findByFullName("TestName")

        when:
        SpringSecurityUtils.doWithAuth("operator") {
            contactPersonService.updateName("TestName", "newName")
        }

        then:
        contactPerson == ContactPerson.findByFullName("newName")
        !ContactPerson.findByFullName("TestName")
    }

    void "test updateName invalid input"() {
        expect:
        SpringSecurityUtils.doWithAuth("operator") {
            shouldFail(AssertionError){
                contactPersonService.updateName(oldName, newName)
            }
        }

        where:
        oldName     | newName
        and: 'empty oldName'
        ""          | "newName"
        and: 'Unknown name'
        "newUser"   | "newName"
        and: 'empty newName'
        "TestName" | ""
        and: 'Duplicate name'
        "TestName" | "TestName2"
    }

    void "test updateEmail valid input"() {
        given:
        ContactPerson contactPerson = ContactPerson.findByFullName("TestName")

        when:
        SpringSecurityUtils.doWithAuth("operator") {
            contactPersonService.updateEmail("TestName", "newUser@dkfz.de")
        }

        then:
        contactPerson.email == "newUser@dkfz.de"
    }

    void "test updateEmail invalid input"() {
        expect:
        SpringSecurityUtils.doWithAuth("operator") {
            shouldFail(AssertionError){
                contactPersonService.updateEmail("TestName", "")
            }
        }
    }

    void "test updateAspera valid input"() {
        given:
        ContactPerson contactPerson = ContactPerson.findByFullName("TestName")

        when:
        SpringSecurityUtils.doWithAuth("operator") {
            contactPersonService.updateAspera("TestName", "newUser")
        }

        then:
        contactPerson.aspera == "newUser"
    }

    private ContactPerson createContactPerson(String name, String email, String aspera, Project project){
        ContactPerson contactPerson = new ContactPerson(
                fullName: name,
                email: email,
                aspera: aspera
        )
        assert contactPerson.save(flush: true, failOnError: true)
        project.addToContactPersons(contactPerson)
        assert project.save(flush: true, failOnError: true)
        return contactPerson
    }
    private void addProject(Project project, ContactPerson contactPerson) {
        project.addToContactPersons(contactPerson)
        assert project.save(flush: true, failOnError: true)
    }
    private Project getProject(String name){
        return Project.findByName("project"+name)
    }
}
