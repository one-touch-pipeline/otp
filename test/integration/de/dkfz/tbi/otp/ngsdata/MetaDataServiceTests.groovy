package de.dkfz.tbi.otp.ngsdata

import static org.junit.Assert.*
import org.junit.*
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.security.UserRole
import de.dkfz.tbi.otp.security.Role
import de.dkfz.tbi.otp.testing.AbstractIntegrationTest
import org.codehaus.groovy.grails.plugins.springsecurity.acl.AclSid
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.codehaus.groovy.grails.plugins.springsecurity.SpringSecurityUtils
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.acls.domain.BasePermission
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.authority.GrantedAuthorityImpl

class MetaDataServiceTests extends AbstractIntegrationTest {
    def metaDataService
    def aclUtilService

    @Before
    void setUp() {
        // Setup logic here
        createUserAndRoles()
    }

    @After
    void tearDown() {
        // Tear down logic here
    }

    /**
     * Tests that an anonymous user does not have access to the MetaDataEntry
     * if no project is defined which could have an ACL on it.
     */
    @Test
    void testGetMetaDataEntryByIdNoProjectAnonymous() {
        MetaDataEntry entry = mockEntry()
        authenticateAnonymous()
        shouldFail(AccessDeniedException) {
            metaDataService.getMetaDataEntryById(entry.id)
        }
        // accessing a non-existing id should still work
        assertNull(metaDataService.getMetaDataEntryById(entry.id + 1))
    }

    /**
     * Tests that a User does not have access to the MetaDataEntry
     * if there is no project which could have an ACL on it.
     */
    @Test
    void testGetMetaDataEntryByIdNoProjectUser() {
        MetaDataEntry entry = mockEntry()
        SpringSecurityUtils.doWithAuth("testuser") {
            shouldFail(AccessDeniedException) {
                metaDataService.getMetaDataEntryById(entry.id)
            }
            // accessing a non-existing id should still work
            assertNull(metaDataService.getMetaDataEntryById(entry.id + 1))
        }
    }

    /**
     * Tests that an admin user does have access to the MetaDataEntry
     * if no Project is defined for the MetaDataEntry.
     */
    @Test
    void testGetMetaDataEntryByIdNoProjectAdmin() {
        MetaDataEntry entry = mockEntry()
        SpringSecurityUtils.doWithAuth("admin") {
            assertSame(entry, metaDataService.getMetaDataEntryById(entry.id))
            // accessing a non-existing id should still work
            assertNull(metaDataService.getMetaDataEntryById(entry.id + 1))
        }
    }

    /**
     * Tests that an anonymous user does not have access to a MetaDataEntry
     * if a project is defined for it, but no ACL.
     */
    void testGetMetaDataEntryByIdWithProjectNoAclAsAnonymous() {
        MetaDataEntry entry = mockEntry()
        Project project = mockProject()
        entry.dataFile.project = project
        assertNotNull(entry.dataFile.save(flush: true))
        authenticateAnonymous()
        shouldFail(AccessDeniedException) {
            metaDataService.getMetaDataEntryById(entry.id)
        }
        // accessing a non-existing id should still work
        assertNull(metaDataService.getMetaDataEntryById(entry.id + 1))
    }

    /**
     * Tests that a User does not have access to the MetaDataEntry
     * if there is a project defined for it
     */
    @Test
    void testGetMetaDataEntryByIdWithProjectAsUser() {
        MetaDataEntry entry = mockEntry()
        Project project = mockProject()
        entry.dataFile.project = project
        assertNotNull(entry.dataFile.save(flush: true))
        SpringSecurityUtils.doWithAuth("testuser") {
            shouldFail(AccessDeniedException) {
                metaDataService.getMetaDataEntryById(entry.id)
            }
            // accessing a non-existing id should still work
            assertNull(metaDataService.getMetaDataEntryById(entry.id + 1))
        }
        // now create ACL for this user on the Project
        aclUtilService.addPermission(project, "testuser", BasePermission.READ)
        SpringSecurityUtils.doWithAuth("testuser") {
            // now our test user should have access
            assertSame(entry, metaDataService.getMetaDataEntryById(entry.id))
            // accessing a non-existing id should still work
            assertNull(metaDataService.getMetaDataEntryById(entry.id + 1))
        }
        // but another user should not have
        SpringSecurityUtils.doWithAuth("user") {
            shouldFail(AccessDeniedException) {
                metaDataService.getMetaDataEntryById(entry.id)
            }
            // accessing a non-existing id should still work
            assertNull(metaDataService.getMetaDataEntryById(entry.id + 1))
        }
    }

    /**
     * Tests that an admin user does have access to the MetaDataEntry
     * if there is a project defined for it, but no ACL
     */
    @Test
    void testGetMetaDataEntryByIdWithProjectAsAdmin() {
        MetaDataEntry entry = mockEntry()
        Project project = mockProject()
        entry.dataFile.project = project
        assertNotNull(entry.dataFile.save(flush: true))
        SpringSecurityUtils.doWithAuth("admin") {
            assertSame(entry, metaDataService.getMetaDataEntryById(entry.id))
            // accessing a non-existing id should still work
            assertNull(metaDataService.getMetaDataEntryById(entry.id + 1))
        }
    }

    /**
     * Tests that an anonymous user cannot update a metaDataEntry
     * if there is no project defined.
     */
    void testUpdateMetaDataEntryNoProjectAsAnonymous() {
        MetaDataEntry entry = mockEntry()
        authenticateAnonymous()
        shouldFail(AccessDeniedException) {
            metaDataService.updateMetaDataEntry(entry, "test2")
        }
    }

    /**
     * Tests that a user cannot update a metaDataEntry
     * if there is no project defined.
     */
    void testUpdateMetaDataEntryNoProjectAsUser() {
        MetaDataEntry entry = mockEntry()
        SpringSecurityUtils.doWithAuth("testuser") {
            shouldFail(AccessDeniedException) {
                metaDataService.updateMetaDataEntry(entry, "test2")
            }
        }
    }

    /**
     * Tests that an admin user can update a metaDataEntry
     * if there is no project defined.
     */
    void testUpdateMetaDataEntryNoProjectAsAdmin() {
        MetaDataEntry entry = mockEntry()
        SpringSecurityUtils.doWithAuth("admin") {
            assertTrue(metaDataService.updateMetaDataEntry(entry, "test2"))
        }
    }

    /**
     * Tests that an anonymous user cannot update a metaDataEntry
     * if there is no project defined.
     */
    void testUpdateMetaDataEntryWithProjectAsAnonymous() {
        MetaDataEntry entry = mockEntry()
        Project project = mockProject()
        entry.dataFile.project = project
        assertNotNull(entry.dataFile.save(flush: true))
        authenticateAnonymous()
        shouldFail(AccessDeniedException) {
            metaDataService.updateMetaDataEntry(entry, "test2")
        }
    }

    /**
     * Tests that a user cannot update a metaDataEntry
     * if there is no project defined.
     */
    void testUpdateMetaDataEntryWithProjectAsUser() {
        MetaDataEntry entry = mockEntry()
        Project project = mockProject()
        entry.dataFile.project = project
        assertNotNull(entry.dataFile.save(flush: true))
        SpringSecurityUtils.doWithAuth("testuser") {
            shouldFail(AccessDeniedException) {
                metaDataService.updateMetaDataEntry(entry, "test2")
            }
        }
        aclUtilService.addPermission(project, "testuser", BasePermission.READ)
        aclUtilService.addPermission(project, "testuser", BasePermission.WRITE)
        SpringSecurityUtils.doWithAuth("testuser") {
            assertTrue(metaDataService.updateMetaDataEntry(entry, "test2"))
        }
        // other user should not have access
        SpringSecurityUtils.doWithAuth("user") {
            shouldFail(AccessDeniedException) {
                metaDataService.updateMetaDataEntry(entry, "test3")
            }
        }
        // let's give that user read permission
        aclUtilService.addPermission(project, "user", BasePermission.READ)
        SpringSecurityUtils.doWithAuth("user") {
            shouldFail(AccessDeniedException) {
                metaDataService.updateMetaDataEntry(entry, "test3")
            }
        }
    }

    /**
     * Tests that an admin user can update a metaDataEntry
     * if there is no project defined.
     */
    void testUpdateMetaDataEntryWithProjectAsAdmin() {
        MetaDataEntry entry = mockEntry()
        Project project = mockProject()
        entry.dataFile.project = project
        assertNotNull(entry.dataFile.save(flush: true))
        SpringSecurityUtils.doWithAuth("admin") {
            assertTrue(metaDataService.updateMetaDataEntry(entry, "test2"))
        }
    }

    /**
     * Creates a very simple MetaDataEntry with minimum required fields.
     * @return
     */
    private MetaDataEntry mockEntry() {
        DataFile dataFile = new DataFile()
        assertTrue(dataFile.validate())
        assertNotNull(dataFile.save(flush: true))
        MetaDataKey key = new MetaDataKey(name: "Test")
        assertTrue(key.validate())
        assertNotNull(key.save(flush: true))
        MetaDataEntry entry = new MetaDataEntry(value: "test", dataFile: dataFile, key: key, source: MetaDataEntry.Source.MANUAL)
        assertTrue(entry.validate())
        entry = entry.save(flush: true)
        assertNotNull(entry)
        return entry
    }

    /**
     * Creates a very simple Project with minimum required fields
     * @return
     */
    private Project mockProject() {
        Project project = new Project(name: "test", dirName: "test", realmName: "test")
        assertNotNull(project.save(flush: true))
        return project
    }
}
