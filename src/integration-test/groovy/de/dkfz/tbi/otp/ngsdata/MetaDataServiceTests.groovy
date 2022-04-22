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
import de.dkfz.tbi.otp.utils.CollectionUtils

import static org.junit.Assert.*

@Rollback
@Integration
class MetaDataServiceTests implements UserAndRoles {
    MetaDataService metaDataService
    AclUtilService aclUtilService

    @SuppressWarnings("PublicInstanceField") // must be public in JUnit tests
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

        addUserWithReadAccessToProject(CollectionUtils.atMostOneElement(User.findAllByUsername(TESTUSER)), entry.dataFile.project)
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
     * Creates a very simple MetaDataEntry with minimum required fields.
     */
    private MetaDataEntry mockEntry() {
        DataFile dataFile = DomainFactory.createDataFile()
        assertTrue(dataFile.validate())
        assertNotNull(dataFile.save(flush: true))
        MetaDataKey key = new MetaDataKey(name: "Test")
        assertTrue(key.validate())
        assertNotNull(key.save(flush: true))
        MetaDataEntry entry = new MetaDataEntry(value: "test", dataFile: dataFile, key: key)
        assertTrue(entry.validate())
        entry = entry.save(flush: true)
        assertNotNull(entry)
        return entry
    }
}
