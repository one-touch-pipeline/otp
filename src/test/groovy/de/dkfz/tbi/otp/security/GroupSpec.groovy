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

package de.dkfz.tbi.otp.security

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.administration.GroupCommand
import de.dkfz.tbi.otp.ngsdata.DomainFactory

class GroupSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        [
                Role,
                Group,
        ]
    }

    void "test write project constraint"() {
        given:
        Role role = DomainFactory.createRoleLazy([
                authority: "GROUP_TEST"
        ])
        Group group

        when:
        group = new Group(
                writeProject: true,
                name: "test",
                role: role,
                description: "",
        )

        then:
        !group.validate()
        group.errors["writeProject"].code == "validator.invalid"

        when:
        group = new Group(name: "test", role: role, description: "")

        then:
        group.validate()

        when:
        group = new Group(readProject: true, name: "test", role: role, description: "")

        then:
        group.validate()

        when:
        group = new Group(readProject: true, writeProject: true, name: "test", role: role, description: "")

        then:
        group.validate()
    }

    void "test write job system constraint"() {
        given:
        Role role = DomainFactory.createRoleLazy([
                authority: "GROUP_TEST"
        ])
        Group group

        when:
        group = new Group(writeJobSystem: true, name: "test", role: role, description: "")

        then:
        !group.validate()
        group.errors["writeJobSystem"].code == "validator.invalid"

        when:
        group = new Group(name: "test", role: role, description: "")

        then:
        group.validate()

        when:
        group = new Group(readJobSystem: true, name: "test", role: role, description: "")

        then:
        group.validate()

        when:
        group = new Group(readJobSystem: true, writeJobSystem: true, name: "test", role: role, description: "")

        then:
        group.validate()
    }

    void "test write sequence center constraint"() {
        given:
        Role role = DomainFactory.createRoleLazy([
                authority: "GROUP_TEST"
        ])
        Group group

        when:
        group = new Group(writeSequenceCenter: true, name: "test", role: role, description: "")

        then:
        !group.validate()
        group.errors["writeSequenceCenter"].code == "validator.invalid"

        when:
        group = new Group(name: "test", role: role, description: "")

        then:
        group.validate()

        when:
        group = new Group(readSequenceCenter: true, name: "test", role: role, description: "")

        then:
        group.validate()

        when:
        group = new Group(readSequenceCenter: true, writeSequenceCenter: true, name: "test", role: role, description: "")

        then:
        group.validate()
    }

    void "test role constraint"() {
        given:
        Role role = DomainFactory.createRoleLazy([
                authority: "STARTSWITHGROUP",
        ])
        Role role2 = DomainFactory.createRoleLazy([
                authority: "GROUP_WHITE SPACE",
        ])
        Role role3 = DomainFactory.createRoleLazy([
                authority: "GROUP_alluppercase",
        ])
        Role role4 = DomainFactory.createRoleLazy([
                authority: "GROUP_CORRECT",
        ])
        Role role5 = DomainFactory.createRoleLazy([
                authority: "GROUP_CORRECT_WHITESPACE",
        ])
        Group group

        when:
        group = new Group(name: "startswithgroup", role: role, description: "")

        then:
        !group.validate()
        group.errors["role"].code == "validator.invalid"

        when:
        group = new Group(name: "white space", role: role2, description: "")

        then:
        !group.validate()
        group.errors["role"].code == "validator.invalid"

        when:
        group = new Group(name: "alluppercase", role: role3, description: "")

        then:
        !group.validate()
        group.errors["role"].code == "validator.invalid"

        when:
        group = new Group(name: "correct", role: role4, description: "")

        then:
        group.validate()

        when:
        group = new Group(name: "correct whitespace", role: role5, description: "")

        then:
        group.validate()

        when:
        group = new Group(name: "null role", role: null)

        then:
        !group.validate()
        group.errors["role"].code == "nullable"
    }

    void "test role unique constraint"() {
        given:
        Role role = DomainFactory.createRoleLazy([
                authority: "GROUP_TEST",
        ])

        when:
        Group group = new Group(name: "test", role: role, description: "").save(flush: true)

        then:
        group.validate()

        when:
        Group group2 = new Group(name: "teSt", role: role, description: "Something needs to be different")

        then:
        !group2.validate()
        group2.errors["role"].code == "unique"
    }

    void "test group from command object"() {
        given:
        Role role = DomainFactory.createRoleLazy([
                authority: "GROUP_TEST",
        ])
        Group group

        when:
        group = Group.fromCommandObject(new GroupCommand(), role)

        then:
        !group.readProject
        !group.writeProject
        !group.readJobSystem
        !group.writeJobSystem
        !group.readSequenceCenter
        !group.writeSequenceCenter
        group.name == null
        group.description == null
        role == group.role

        when:
        group = Group.fromCommandObject(new GroupCommand(readProject: true), role)

        then:
        group.readProject
        !group.writeProject
        !group.readJobSystem
        !group.writeJobSystem
        !group.readSequenceCenter
        !group.writeSequenceCenter

        when:
        group = Group.fromCommandObject(new GroupCommand(writeProject: true), role)

        then:
        !group.readProject
        group.writeProject
        !group.readJobSystem
        !group.writeJobSystem
        !group.readSequenceCenter
        !group.writeSequenceCenter

        when:
        group = Group.fromCommandObject(new GroupCommand(readJobSystem: true), role)

        then:
        !group.readProject
        !group.writeProject
        group.readJobSystem
        !group.writeJobSystem
        !group.readSequenceCenter
        !group.writeSequenceCenter

        when:
        group = Group.fromCommandObject(new GroupCommand(writeJobSystem: true), role)

        then:
        !group.readProject
        !group.writeProject
        !group.readJobSystem
        group.writeJobSystem
        !group.readSequenceCenter
        !group.writeSequenceCenter

        when:
        group = Group.fromCommandObject(new GroupCommand(readSequenceCenter: true), role)

        then:
        !group.readProject
        !group.writeProject
        !group.readJobSystem
        !group.writeJobSystem
        group.readSequenceCenter
        !group.writeSequenceCenter

        when:
        group = Group.fromCommandObject(new GroupCommand(writeSequenceCenter: true), role)

        then:
        !group.readProject
        !group.writeProject
        !group.readJobSystem
        !group.writeJobSystem
        !group.readSequenceCenter
        group.writeSequenceCenter

        when:
        group = Group.fromCommandObject(new GroupCommand(readProject: true,
                                                         writeProject: true,
                                                         readJobSystem: true,
                                                         writeJobSystem: true,
                                                         readSequenceCenter: true,
                                                         writeSequenceCenter: true), role)

        then:
        group.readProject
        group.writeProject
        group.readJobSystem
        group.writeJobSystem
        group.readSequenceCenter
        group.writeSequenceCenter

        when:
        group = Group.fromCommandObject(new GroupCommand(name: "name", description: "description"), role)

        then:
        "name" == group.name
        "description" == group.description
    }
}
