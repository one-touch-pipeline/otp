package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.testing.*
import grails.plugin.springsecurity.*
import grails.plugin.springsecurity.acl.*
import org.junit.*
import org.junit.rules.TemporaryFolder
import org.springframework.security.access.*
import org.springframework.security.acls.domain.*

import static org.junit.Assert.*

class MetaDataServiceTests extends AbstractIntegrationTest {
    MetaDataService metaDataService
    AclUtilService aclUtilService

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder()

    File baseDir

    @Before
    void setUp() {
        baseDir = temporaryFolder.newFolder()
        // Setup logic here
        createUserAndRoles()
    }

    @After
    void tearDown() {
        // Tear down logic here
        assertTrue(baseDir.deleteDir())
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
     * Tests that an operator user does have access to the MetaDataEntry
     * if no Project is defined for the MetaDataEntry.
     */
    @Test
    void testGetMetaDataEntryByIdNoProjectOperator() {
        MetaDataEntry entry = mockEntry()

        SpringSecurityUtils.doWithAuth("operator") {
            assertSame(entry, metaDataService.getMetaDataEntryById(entry.id))
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
    @Test
    void testGetMetaDataEntryByIdWithProjectNoAclAsAnonymous() {
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
     * if there is a project defined for it
     */
    @Test
    void testGetMetaDataEntryByIdWithProjectAsUser() {
        MetaDataEntry entry = mockEntry()

        SpringSecurityUtils.doWithAuth("testuser") {
            shouldFail(AccessDeniedException) {
                metaDataService.getMetaDataEntryById(entry.id)
            }
            // accessing a non-existing id should still work
            assertNull(metaDataService.getMetaDataEntryById(entry.id + 1))
        }

        // now create ACL for this user on the Project
        SpringSecurityUtils.doWithAuth("admin") {
            aclUtilService.addPermission(entry.dataFile.project, "testuser", BasePermission.READ)
        }
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
     * Tests that an operator user does have access to the MetaDataEntry
     * if there is a project defined for it, but no ACL
     */
    @Test
    void testGetMetaDataEntryByIdWithProjectAsOperator() {
        MetaDataEntry entry = mockEntry()

        SpringSecurityUtils.doWithAuth("operator") {
            assertSame(entry, metaDataService.getMetaDataEntryById(entry.id))
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
    @Test
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
    @Test
    void testUpdateMetaDataEntryNoProjectAsUser() {
        MetaDataEntry entry = mockEntry()
        SpringSecurityUtils.doWithAuth("testuser") {
            shouldFail(AccessDeniedException) {
                metaDataService.updateMetaDataEntry(entry, "test2")
            }
        }
    }

    /**
     * Tests that an operator user can update a metaDataEntry
     * if there is no project defined.
     */
    @Test
    void testUpdateMetaDataEntryNoProjectAsOperator() {
        MetaDataEntry entry = mockEntry()
        SpringSecurityUtils.doWithAuth("operator") {
            assertTrue(metaDataService.updateMetaDataEntry(entry, "test2"))
        }
    }

    /**
     * Tests that an admin user can update a metaDataEntry
     * if there is no project defined.
     */
    @Test
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
    @Test
    void testUpdateMetaDataEntryWithProjectAsAnonymous() {
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
    @Test
    void testUpdateMetaDataEntryWithProjectAsUser() {
        MetaDataEntry entry = mockEntry()
        SpringSecurityUtils.doWithAuth("testuser") {
            shouldFail(AccessDeniedException) {
                metaDataService.updateMetaDataEntry(entry, "test2")
            }
        }
        aclUtilService.addPermission(entry.dataFile.project, "testuser", BasePermission.READ)
        aclUtilService.addPermission(entry.dataFile.project, "testuser", BasePermission.WRITE)
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
        aclUtilService.addPermission(entry.dataFile.project, "user", BasePermission.READ)
        SpringSecurityUtils.doWithAuth("user") {
            shouldFail(AccessDeniedException) {
                metaDataService.updateMetaDataEntry(entry, "test3")
            }
        }
    }

    /**
     * Tests that an operator user can update a metaDataEntry
     * if there is no project defined.
     */
    @Test
    void testUpdateMetaDataEntryWithProjectAsOperator() {
        MetaDataEntry entry = mockEntry()
        SpringSecurityUtils.doWithAuth("operator") {
            assertTrue(metaDataService.updateMetaDataEntry(entry, "test2"))
        }
    }

    /**
     * Tests that an admin user can update a metaDataEntry
     * if there is no project defined.
     */
    @Test
    void testUpdateMetaDataEntryWithProjectAsAdmin() {
        MetaDataEntry entry = mockEntry()
        SpringSecurityUtils.doWithAuth("admin") {
            assertTrue(metaDataService.updateMetaDataEntry(entry, "test2"))
        }
    }

    /**
     * Test that verifies that an admin user can retrieve the changelog
     * if there is no project assigned to the Entry yet.
     * Test for BUG OTP-36
     */
    @Test
    void testRetrieveChangelogWithoutProject() {
        MetaDataEntry entry = mockEntry()
        // admin should always be able to see the entry
        SpringSecurityUtils.doWithAuth("admin") {
            assertTrue(metaDataService.retrieveChangeLog(entry).empty)
        }
        // also the operator
        SpringSecurityUtils.doWithAuth("operator") {
            assertTrue(metaDataService.retrieveChangeLog(entry).empty)
        }
        // but a user should not
        SpringSecurityUtils.doWithAuth("testuser") {
            shouldFail(AccessDeniedException) {
                metaDataService.retrieveChangeLog(entry)
            }
        }
        // creating a changelog entry should not change anything
        SpringSecurityUtils.doWithAuth("admin") {
            metaDataService.updateMetaDataEntry(entry, "test2")
            assertEquals(1, metaDataService.retrieveChangeLog(entry).size())
        }
        SpringSecurityUtils.doWithAuth("operator") {
            assertEquals(1, metaDataService.retrieveChangeLog(entry).size())
        }
        // a user should still not be able to see it
        SpringSecurityUtils.doWithAuth("testuser") {
            shouldFail(AccessDeniedException) {
                metaDataService.retrieveChangeLog(entry)
            }
        }
    }

    /**
     * Tests the security for the case that a project is assigned to the MetaDataEntry
     * but no ACL is defined
     */
    @Test
    void testRetrieveChangelogWithProjectNoAcl() {
        MetaDataEntry entry = mockEntry()
        // admin should always be able to see the entry
        SpringSecurityUtils.doWithAuth("admin") {
            assertTrue(metaDataService.retrieveChangeLog(entry).empty)
        }
        // also the operator
        SpringSecurityUtils.doWithAuth("operator") {
            assertTrue(metaDataService.retrieveChangeLog(entry).empty)
        }
        // but a user should not
        SpringSecurityUtils.doWithAuth("testuser") {
            shouldFail(AccessDeniedException) {
                metaDataService.retrieveChangeLog(entry)
            }
        }
        // creating a changelog entry should not change anything
        SpringSecurityUtils.doWithAuth("admin") {
            metaDataService.updateMetaDataEntry(entry, "test2")
            assertEquals(1, metaDataService.retrieveChangeLog(entry).size())
        }
        SpringSecurityUtils.doWithAuth("operator") {
            assertEquals(1, metaDataService.retrieveChangeLog(entry).size())
        }
        // a user should still not be able to see it
        SpringSecurityUtils.doWithAuth("testuser") {
            shouldFail(AccessDeniedException) {
                metaDataService.retrieveChangeLog(entry)
            }
        }
    }

    /**
     * Tests the security for the case that a project is assigned to the MetaDataEntry
     * but no ACL is defined
     */
    @Test
    void testRetrieveEmptyChangelogWithAcl() {
        MetaDataEntry entry = mockEntry()
        // admin should always be able to see the entry
        SpringSecurityUtils.doWithAuth("admin") {
            assertTrue(metaDataService.retrieveChangeLog(entry).empty)
            aclUtilService.addPermission(entry.dataFile.project, "testuser", BasePermission.READ)
        }
        // now the user should be able to retrieve the changelog
        SpringSecurityUtils.doWithAuth("testuser") {
            assertTrue(metaDataService.retrieveChangeLog(entry).empty)
        }
        // operator should see it
        SpringSecurityUtils.doWithAuth("operator") {
            assertTrue(metaDataService.retrieveChangeLog(entry).empty)
        }
        // but another user should not
        SpringSecurityUtils.doWithAuth("user") {
            shouldFail(AccessDeniedException) {
                metaDataService.retrieveChangeLog(entry)
            }
        }
    }

    /**
     * Tests the security for the case that a project is assigned to the MetaDataEntry
     * and the ACL is defined
     */
    @Test
    void testRetrieveChangelogWithAcl() {
        MetaDataEntry entry = mockEntry()
        // grant read to user and add an update
        SpringSecurityUtils.doWithAuth("admin") {
            assertTrue(metaDataService.retrieveChangeLog(entry).empty)
            metaDataService.updateMetaDataEntry(entry, "test2")
            assertEquals(1, metaDataService.retrieveChangeLog(entry).size())
            aclUtilService.addPermission(entry.dataFile.project, "testuser", BasePermission.READ)
        }
        // now user should be able to see the changelog
        SpringSecurityUtils.doWithAuth("testuser") {
            assertEquals(1, metaDataService.retrieveChangeLog(entry).size())
        }
        // operator should see it
        SpringSecurityUtils.doWithAuth("operator") {
            assertEquals(1, metaDataService.retrieveChangeLog(entry).size())
        }
        // but a different user should not
        SpringSecurityUtils.doWithAuth("user") {
            shouldFail(AccessDeniedException) {
                metaDataService.retrieveChangeLog(entry)
            }
        }
    }

    /**
     * Creates a very simple MetaDataEntry with minimum required fields.
     * @return
     */
    private MetaDataEntry mockEntry() {
        DataFile dataFile = DomainFactory.createDataFile()
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
}
