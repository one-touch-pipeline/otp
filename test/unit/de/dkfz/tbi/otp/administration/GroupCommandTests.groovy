package de.dkfz.tbi.otp.administration

import grails.test.mixin.Mock
import grails.test.mixin.TestMixin
import grails.test.mixin.web.ControllerUnitTestMixin
import org.junit.Before
import org.junit.Test

import de.dkfz.tbi.otp.security.Role

import static org.junit.Assert.*

@TestMixin(ControllerUnitTestMixin)
@Mock([Role])
class GroupCommandTests {

    @Before
    void setup() {
        mockCommandObject(GroupCommand)
    }

    @Test
    void testWriteProjectConstraint() {
        GroupCommand group = new GroupCommand(writeProject: true, name: "test", description: "")
        // write project is enabled, but read project is disabled - should not validate
        assertFalse(group.validate())
        assertEquals("Write without read", group.errors.getFieldError("writeProject").code)
        // disabling write project should validate
        group = new GroupCommand(name: "test", description: "")
        assertTrue(group.validate())
        // enabling read project should validate for disabled write
        group = new GroupCommand(name: "test", description: "", readProject: true)
        assertTrue(group.validate())
        // and for enabled write
        group = new GroupCommand(name: "test", description: "", readProject: true, writeProject: true)
        assertTrue(group.validate())
    }

    @Test
    void testWriteJobSystemConstraint() {
        GroupCommand group = new GroupCommand(writeJobSystem: true, name: "test", description: "")
        // write job system is enabled, but read job system is disabled - should not validate
        assertFalse(group.validate())
        assertEquals("Write without read", group.errors.getFieldError("writeJobSystem").code)
        // disabling write job system should validate
        group = new GroupCommand(name: "test", description: "")
        assertTrue(group.validate())
        // enabling read job system should validate for disabled write
        group = new GroupCommand(readJobSystem: true, name: "test", description: "")
        assertTrue(group.validate())
        // and for enabled write
        group = new GroupCommand(readJobSystem: true, writeJobSystem: true, name: "test", description: "")
        assertTrue(group.validate())
    }

    @Test
    void testWriteSequenceCenterConstraint() {
        GroupCommand group = new GroupCommand(writeSequenceCenter: true, name: "test", description: "")
        // write sequence center is enabled, but read sequence center is disabled - should not validate
        assertFalse(group.validate())
        assertEquals("Write without read", group.errors.getFieldError("writeSequenceCenter").code)
        // disabling write sequence center should validate
        group = new GroupCommand(name: "test", description: "")
        assertTrue(group.validate())
        // enabling read sequence center should validate for disabled write
        group = new GroupCommand(readSequenceCenter: true, name: "test", description: "")
        assertTrue(group.validate())
        // and for enabled write
        group = new GroupCommand(readSequenceCenter: true, writeSequenceCenter: true, name: "test", description: "")
        assertTrue(group.validate())
    }

    @Test
    void testNameConstraint() {
        Role role = new Role(authority: "GROUP_TEST")
        role.save(flush: true)
        GroupCommand group = new GroupCommand(name: "test", description: "")
        assertFalse(group.validate())
        assertEquals("Group already exists.", group.errors.getFieldError("name").code)
        group = new GroupCommand(name: "tEsT", description: "")
        assertFalse(group.validate())
        assertEquals("Group already exists.", group.errors.getFieldError("name").code)
        role = new Role(authority: "GROUP_TEST_2")
        role.save(flush: true)
        group = new GroupCommand(name: "tEsT 2", description: "")
        assertFalse(group.validate())
        assertEquals("Group already exists.", group.errors.getFieldError("name").code)
        group = new GroupCommand(name: "tEsT 3", description: "")
        assertTrue(group.validate())
    }
}
