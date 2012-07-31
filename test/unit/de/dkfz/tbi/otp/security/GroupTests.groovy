package de.dkfz.tbi.otp.security

import static org.junit.Assert.*

import grails.test.mixin.*
import org.junit.*

import de.dkfz.tbi.otp.administration.GroupCommand

@TestFor(Group)
class GroupTests {

    void testWriteProjectConstraint() {
        // create required Role
        Role role = new Role(authority: "GROUP_TEST")
        mockDomain(Role, [role])
        Group group = new Group(writeProject: true, name: "test", role: role, description: "")
        mockForConstraintsTests(Group)
        // write project is enabled, but read project is disabled - should not validate
        assertFalse(group.validate())
        assertEquals("validator", group.errors["writeProject"])
        // disabling write project should validate
        group = new Group(name: "test", role: role, description: "")
        assertTrue(group.validate())
        // enabling read project should validate for disabled write
        group = new Group(readProject: true, name: "test", role: role, description: "")
        assertTrue(group.validate())
        // and for enabled write
        group = new Group(readProject: true, writeProject: true, name: "test", role: role, description: "")
        assertTrue(group.validate())
    }

    void testWriteJobSystemConstraint() {
        // create required Role
        Role role = new Role(authority: "GROUP_TEST")
        mockDomain(Role, [role])
        Group group = new Group(writeJobSystem: true, name: "test", role: role, description: "")
        mockForConstraintsTests(Group)
        // write job system is enabled, but read job system is disabled - should not validate
        assertFalse(group.validate())
        assertEquals("validator", group.errors["writeJobSystem"])
        // disabling write job system should validate
        group = new Group(name: "test", role: role, description: "")
        assertTrue(group.validate())
        // enabling read job system should validate for disabled write
        group = new Group(readJobSystem: true, name: "test", role: role, description: "")
        assertTrue(group.validate())
        // and for enabled write
        group = new Group(readJobSystem: true, writeJobSystem: true, name: "test", role: role, description: "")
        assertTrue(group.validate())
    }

    void testWriteSequenceCenterConstraint() {
        // create required Role
        Role role = new Role(authority: "GROUP_TEST")
        mockDomain(Role, [role])
        Group group = new Group(writeSequenceCenter: true, name: "test", role: role, description: "")
        mockForConstraintsTests(Group)
        // write sequence center is enabled, but read sequence center is disabled - should not validate
        assertFalse(group.validate())
        assertEquals("validator", group.errors["writeSequenceCenter"])
        // disabling write sequence center should validate
        group = new Group(name: "test", role: role, description: "")
        assertTrue(group.validate())
        // enabling read sequence center should validate for disabled write
        group = new Group(readSequenceCenter: true, name: "test", role: role, description: "")
        assertTrue(group.validate())
        // and for enabled write
        group = new Group(readSequenceCenter: true, writeSequenceCenter: true, name: "test", role: role, description: "")
        assertTrue(group.validate())
    }

    void testRoleConstraint() {
        Role role = new Role(authority: "STARTSWITHGROUP")
        Role role2 = new Role(authority: "GROUP_WHITE SPACE")
        Role role3 = new Role(authority: "GROUP_alluppercase")
        Role role4 = new Role(authority: "GROUP_CORRECT")
        Role role5 = new Role(authority: "GROUP_CORRECT_WHITESPACE")
        mockDomain(Role, [role, role2, role3, role4, role5])
        Group group = new Group(name: "startswithgroup", role: role, description: "")
        mockForConstraintsTests(Group)
        assertFalse(group.validate())
        assertEquals("validator", group.errors["role"])
        group = new Group(name: "white space", role: role2, description: "")
        assertFalse(group.validate())
        assertEquals("validator", group.errors["role"])
        group = new Group(name: "alluppercase", role: role3, description: "")
        assertFalse(group.validate())
        assertEquals("validator", group.errors["role"])
        group = new Group(name: "correct", role: role4, description: "")
        assertTrue(group.validate())
        group = new Group(name: "correct whitespace", role: role5, description: "")
        assertTrue(group.validate())
        group = new Group(name: "null role", role: null)
        assertFalse(group.validate())
        assertEquals("nullable", group.errors["role"])
    }

    void testRoleUniqueConstraint() {
        Role role = new Role(authority: "GROUP_TEST")
        mockDomain(Role, [role])
        Group group = new Group(name: "test", role: role, description: "")
        mockForConstraintsTests(Group, [group])
        assertTrue(group.validate())
        Group group2 = new Group(name: "teSt", role: role, description: "Something needs to be different")
        assertFalse(group2.validate())
        assertEquals("unique", group2.errors["role"])
    }

    void testGroupFromCommandObject() {
        Role role = new Role(authority: "GROUP_TEST")
        mockDomain(Role, [role])
        // bare minimum
        Group group = Group.fromCommandObject(new GroupCommand(), role)
        assertFalse(group.readProject)
        assertFalse(group.writeProject)
        assertFalse(group.readJobSystem)
        assertFalse(group.writeJobSystem)
        assertFalse(group.readSequenceCenter)
        assertFalse(group.writeSequenceCenter)
        assertNull(group.name)
        assertNull(group.description)
        assertSame(role, group.role)
        // read project as true
        group = Group.fromCommandObject(new GroupCommand(readProject: true), role)
        assertTrue(group.readProject)
        assertFalse(group.writeProject)
        assertFalse(group.readJobSystem)
        assertFalse(group.writeJobSystem)
        assertFalse(group.readSequenceCenter)
        assertFalse(group.writeSequenceCenter)
        // write project as true
        group = Group.fromCommandObject(new GroupCommand(writeProject: true), role)
        assertFalse(group.readProject)
        assertTrue(group.writeProject)
        assertFalse(group.readJobSystem)
        assertFalse(group.writeJobSystem)
        assertFalse(group.readSequenceCenter)
        assertFalse(group.writeSequenceCenter)
        // read job system as true
        group = Group.fromCommandObject(new GroupCommand(readJobSystem: true), role)
        assertFalse(group.readProject)
        assertFalse(group.writeProject)
        assertTrue(group.readJobSystem)
        assertFalse(group.writeJobSystem)
        assertFalse(group.readSequenceCenter)
        assertFalse(group.writeSequenceCenter)
        // write job system as true
        group = Group.fromCommandObject(new GroupCommand(writeJobSystem: true), role)
        assertFalse(group.readProject)
        assertFalse(group.writeProject)
        assertFalse(group.readJobSystem)
        assertTrue(group.writeJobSystem)
        assertFalse(group.readSequenceCenter)
        assertFalse(group.writeSequenceCenter)
        // read sequence center as true
        group = Group.fromCommandObject(new GroupCommand(readSequenceCenter: true), role)
        assertFalse(group.readProject)
        assertFalse(group.writeProject)
        assertFalse(group.readJobSystem)
        assertFalse(group.writeJobSystem)
        assertTrue(group.readSequenceCenter)
        assertFalse(group.writeSequenceCenter)
        // write sequence center as true
        group = Group.fromCommandObject(new GroupCommand(writeSequenceCenter: true), role)
        assertFalse(group.readProject)
        assertFalse(group.writeProject)
        assertFalse(group.readJobSystem)
        assertFalse(group.writeJobSystem)
        assertFalse(group.readSequenceCenter)
        assertTrue(group.writeSequenceCenter)
        // all boolean as true
        group = Group.fromCommandObject(new GroupCommand(readProject: true,
                                                         writeProject: true,
                                                         readJobSystem: true,
                                                         writeJobSystem: true,
                                                         readSequenceCenter: true,
                                                         writeSequenceCenter: true), role)
        assertTrue(group.readProject)
        assertTrue(group.writeProject)
        assertTrue(group.readJobSystem)
        assertTrue(group.writeJobSystem)
        assertTrue(group.readSequenceCenter)
        assertTrue(group.writeSequenceCenter)
        // setting a name and a description
        group = Group.fromCommandObject(new GroupCommand(name: "name", description: "description"), role)
        assertEquals("name", group.name)
        assertEquals("description", group.description)
    }
}
