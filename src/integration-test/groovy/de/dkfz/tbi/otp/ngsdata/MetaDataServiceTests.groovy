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

package de.dkfz.tbi.otp.ngsdata

import grails.plugin.springsecurity.SpringSecurityUtils
import grails.plugin.springsecurity.acl.AclUtilService
import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.junit.*
import org.junit.rules.TemporaryFolder
import org.springframework.security.access.AccessDeniedException

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.security.UserAndRoles

import static org.junit.Assert.*

@Rollback
@Integration
class MetaDataServiceTests implements UserAndRoles {
    MetaDataService metaDataService
    AclUtilService aclUtilService

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder()

    File baseDir

    void setupData() {
        baseDir = temporaryFolder.newFolder()
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
        setupData()
        MetaDataEntry entry = mockEntry()

        TestCase.shouldFail(AccessDeniedException) {
            doWithAnonymousAuth {
                metaDataService.getMetaDataEntryById(entry.id)
            }
        }
        // accessing a non-existing id should still work
        doWithAnonymousAuth {
            assertNull(metaDataService.getMetaDataEntryById(entry.id + 1))
        }
    }

    /**
     * Tests that a User does not have access to the MetaDataEntry
     * if there is no project which could have an ACL on it.
     */
    @Test
    void testGetMetaDataEntryByIdNoProjectUser() {
        setupData()
        MetaDataEntry entry = mockEntry()

        SpringSecurityUtils.doWithAuth(TESTUSER) {
            TestCase.shouldFail(AccessDeniedException) {
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
        setupData()
        MetaDataEntry entry = mockEntry()

        SpringSecurityUtils.doWithAuth(OPERATOR) {
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
        setupData()
        MetaDataEntry entry = mockEntry()

        SpringSecurityUtils.doWithAuth(ADMIN) {
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
        setupData()
        MetaDataEntry entry = mockEntry()

        TestCase.shouldFail(AccessDeniedException) {
            doWithAnonymousAuth {
                metaDataService.getMetaDataEntryById(entry.id)
            }
        }

        // accessing a non-existing id should still work
        doWithAnonymousAuth {
            assertNull(metaDataService.getMetaDataEntryById(entry.id + 1))
        }
    }

    /**
     * Tests that a User does not have access to the MetaDataEntry
     * if there is a project defined for it
     */
    @Test
    void testGetMetaDataEntryByIdWithProjectAsUser() {
        setupData()
        MetaDataEntry entry = mockEntry()

        SpringSecurityUtils.doWithAuth(TESTUSER) {
            TestCase.shouldFail(AccessDeniedException) {
                metaDataService.getMetaDataEntryById(entry.id)
            }
            // accessing a non-existing id should still work
            assertNull(metaDataService.getMetaDataEntryById(entry.id + 1))
        }

        addUserWithReadAccessToProject(User.findByUsername(TESTUSER), entry.dataFile.project)
        SpringSecurityUtils.doWithAuth(TESTUSER) {
            // now our test user should have access
            assertSame(entry, metaDataService.getMetaDataEntryById(entry.id))
            // accessing a non-existing id should still work
            assertNull(metaDataService.getMetaDataEntryById(entry.id + 1))
        }

        // but another user should not have
        SpringSecurityUtils.doWithAuth(USER) {
            TestCase.shouldFail(AccessDeniedException) {
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
        setupData()
        MetaDataEntry entry = mockEntry()

        SpringSecurityUtils.doWithAuth(OPERATOR) {
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
        setupData()
        MetaDataEntry entry = mockEntry()

        SpringSecurityUtils.doWithAuth(ADMIN) {
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
        setupData()
        MetaDataEntry entry = mockEntry()
        TestCase.shouldFail(AccessDeniedException) {
            doWithAnonymousAuth {
                metaDataService.updateMetaDataEntry(entry, "test2")
            }
        }
    }

    /**
     * Tests that a user cannot update a metaDataEntry
     * if there is no project defined.
     */
    @Test
    void testUpdateMetaDataEntryNoProjectAsUser() {
        setupData()
        MetaDataEntry entry = mockEntry()
        SpringSecurityUtils.doWithAuth(TESTUSER) {
            TestCase.shouldFail(AccessDeniedException) {
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
        setupData()
        MetaDataEntry entry = mockEntry()
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            assertTrue(metaDataService.updateMetaDataEntry(entry, "test2"))
        }
    }

    /**
     * Tests that an admin user can update a metaDataEntry
     * if there is no project defined.
     */
    @Test
    void testUpdateMetaDataEntryNoProjectAsAdmin() {
        setupData()
        MetaDataEntry entry = mockEntry()
        SpringSecurityUtils.doWithAuth(ADMIN) {
            assertTrue(metaDataService.updateMetaDataEntry(entry, "test2"))
        }
    }

    /**
     * Tests that an anonymous user cannot update a metaDataEntry
     * if there is no project defined.
     */
    @Test
    void testUpdateMetaDataEntryWithProjectAsAnonymous() {
        setupData()
        MetaDataEntry entry = mockEntry()
        TestCase.shouldFail(AccessDeniedException) {
            doWithAnonymousAuth {
                metaDataService.updateMetaDataEntry(entry, "test2")
            }
        }
    }

    @Test
    void testUpdateMetaDataEntryPermission() {
        setupData()
        MetaDataEntry entry = mockEntry()
        SpringSecurityUtils.doWithAuth(TESTUSER) {
            TestCase.shouldFail(AccessDeniedException) {
                metaDataService.updateMetaDataEntry(entry, "test2")
            }
        }
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            assertTrue(metaDataService.updateMetaDataEntry(entry, "test2"))
        }
    }

    /**
     * Tests that an operator user can update a metaDataEntry
     * if there is no project defined.
     */
    @Test
    void testUpdateMetaDataEntryWithProjectAsOperator() {
        setupData()
        MetaDataEntry entry = mockEntry()
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            assertTrue(metaDataService.updateMetaDataEntry(entry, "test2"))
        }
    }

    /**
     * Tests that an admin user can update a metaDataEntry
     * if there is no project defined.
     */
    @Test
    void testUpdateMetaDataEntryWithProjectAsAdmin() {
        setupData()
        MetaDataEntry entry = mockEntry()
        SpringSecurityUtils.doWithAuth(ADMIN) {
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
        setupData()
        MetaDataEntry entry = mockEntry()
        // admin should always be able to see the entry
        SpringSecurityUtils.doWithAuth(ADMIN) {
            assertTrue(metaDataService.retrieveChangeLog(entry).empty)
        }
        // also the operator
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            assertTrue(metaDataService.retrieveChangeLog(entry).empty)
        }
        // but a user should not
        SpringSecurityUtils.doWithAuth(TESTUSER) {
            TestCase.shouldFail(AccessDeniedException) {
                metaDataService.retrieveChangeLog(entry)
            }
        }
        // creating a changelog entry should not change anything
        SpringSecurityUtils.doWithAuth(ADMIN) {
            metaDataService.updateMetaDataEntry(entry, "test2")
            assertEquals(1, metaDataService.retrieveChangeLog(entry).size())
        }
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            assertEquals(1, metaDataService.retrieveChangeLog(entry).size())
        }
        // a user should still not be able to see it
        SpringSecurityUtils.doWithAuth(TESTUSER) {
            TestCase.shouldFail(AccessDeniedException) {
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
        setupData()
        MetaDataEntry entry = mockEntry()
        // admin should always be able to see the entry
        SpringSecurityUtils.doWithAuth(ADMIN) {
            assertTrue(metaDataService.retrieveChangeLog(entry).empty)
        }
        // also the operator
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            assertTrue(metaDataService.retrieveChangeLog(entry).empty)
        }
        // but a user should not
        SpringSecurityUtils.doWithAuth(TESTUSER) {
            TestCase.shouldFail(AccessDeniedException) {
                metaDataService.retrieveChangeLog(entry)
            }
        }
        // creating a changelog entry should not change anything
        SpringSecurityUtils.doWithAuth(ADMIN) {
            metaDataService.updateMetaDataEntry(entry, "test2")
            assertEquals(1, metaDataService.retrieveChangeLog(entry).size())
        }
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            assertEquals(1, metaDataService.retrieveChangeLog(entry).size())
        }
        // a user should still not be able to see it
        SpringSecurityUtils.doWithAuth(TESTUSER) {
            TestCase.shouldFail(AccessDeniedException) {
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
        setupData()
        MetaDataEntry entry = mockEntry()
        addUserWithReadAccessToProject(User.findByUsername(TESTUSER), entry.dataFile.project)
        // admin should always be able to see the entry
        SpringSecurityUtils.doWithAuth(ADMIN) {
            assertTrue(metaDataService.retrieveChangeLog(entry).empty)
        }
        // now the user should be able to retrieve the changelog
        SpringSecurityUtils.doWithAuth(TESTUSER) {
            assertTrue(metaDataService.retrieveChangeLog(entry).empty)
        }
        // operator should see it
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            assertTrue(metaDataService.retrieveChangeLog(entry).empty)
        }
        // but another user should not
        SpringSecurityUtils.doWithAuth(USER) {
            TestCase.shouldFail(AccessDeniedException) {
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
        setupData()
        MetaDataEntry entry = mockEntry()
        addUserWithReadAccessToProject(User.findByUsername(TESTUSER), entry.dataFile.project)
        // grant read to user and add an update
        SpringSecurityUtils.doWithAuth(ADMIN) {
            assertTrue(metaDataService.retrieveChangeLog(entry).empty)
            metaDataService.updateMetaDataEntry(entry, "test2")
            assertEquals(1, metaDataService.retrieveChangeLog(entry).size())
        }
        // now user should be able to see the changelog
        SpringSecurityUtils.doWithAuth(TESTUSER) {
            assertEquals(1, metaDataService.retrieveChangeLog(entry).size())
        }
        // operator should see it
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            assertEquals(1, metaDataService.retrieveChangeLog(entry).size())
        }
        // but a different user should not
        SpringSecurityUtils.doWithAuth(USER) {
            TestCase.shouldFail(AccessDeniedException) {
                metaDataService.retrieveChangeLog(entry)
            }
        }
    }

    /**
     * Creates a very simple MetaDataEntry with minimum required fields.
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
