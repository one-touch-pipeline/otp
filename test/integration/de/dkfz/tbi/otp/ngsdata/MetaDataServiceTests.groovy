package de.dkfz.tbi.otp.ngsdata

import static org.junit.Assert.*

import grails.plugin.springsecurity.acl.AclUtilService
import org.junit.*
import de.dkfz.tbi.otp.testing.AbstractIntegrationTest
import grails.plugin.springsecurity.SpringSecurityUtils
import org.springframework.security.access.AccessDeniedException

class MetaDataServiceTests extends AbstractIntegrationTest {
    MetaDataService metaDataService
    AclUtilService aclUtilService

    File baseDir = new File("/tmp/otp/metadataservice")

    @Before
    void setUp() {
        // Setup logic here
        createUserAndRoles()
        if (!baseDir.exists()) {
            assertTrue(baseDir.mkdirs())
        }
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
        SpringSecurityUtils.doWithAuth("admin") {
            aclUtilService.addPermission(project, "testuser", BasePermission.READ)
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
        Project project = mockProject()
        entry.dataFile.project = project
        assertNotNull(entry.dataFile.save(flush: true))

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
     * Tests that an operator user can update a metaDataEntry
     * if there is no project defined.
     */
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
     * Tests that an operator user can update a metaDataEntry
     * if there is no project defined.
     */
    void testUpdateMetaDataEntryWithProjectAsOperator() {
        MetaDataEntry entry = mockEntry()
        Project project = mockProject()
        entry.dataFile.project = project
        assertNotNull(entry.dataFile.save(flush: true))
        SpringSecurityUtils.doWithAuth("operator") {
            assertTrue(metaDataService.updateMetaDataEntry(entry, "test2"))
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
     * Test that verifies that an admin user can retrieve the changelog
     * if there is no project assigned to the Entry yet.
     * Test for BUG OTP-36
     */
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
    void testRetrieveChangelogWithProjectNoAcl() {
        MetaDataEntry entry = mockEntry()
        Project project = mockProject()
        entry.dataFile.project = project
        assertNotNull(entry.dataFile.save(flush: true))
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
    void testRetrieveEmptyChangelogWithAcl() {
        MetaDataEntry entry = mockEntry()
        Project project = mockProject()
        entry.dataFile.project = project
        assertNotNull(entry.dataFile.save(flush: true))
        // admin should always be able to see the entry
        SpringSecurityUtils.doWithAuth("admin") {
            assertTrue(metaDataService.retrieveChangeLog(entry).empty)
            aclUtilService.addPermission(project, "testuser", BasePermission.READ)
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
    void testRetrieveChangelogWithAcl() {
        MetaDataEntry entry = mockEntry()
        Project project = mockProject()
        entry.dataFile.project = project
        assertNotNull(entry.dataFile.save(flush: true))
        // grant read to user and add an update
        SpringSecurityUtils.doWithAuth("admin") {
            assertTrue(metaDataService.retrieveChangeLog(entry).empty)
            metaDataService.updateMetaDataEntry(entry, "test2")
            assertEquals(1, metaDataService.retrieveChangeLog(entry).size())
            aclUtilService.addPermission(project, "testuser", BasePermission.READ)
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


    void testProcessMetaDataFileEmptyLines() {
        String runName = "130312_D00133_0018_ADTWTJACXX"
        String fileName = "${runName}.fastq.tsv"
        File file = new File(baseDir, fileName)
        file << """FASTQ_FILE\tMD5\tCENTER_NAME\tRUN_ID\tRUN_DATE\tLANE_NO\tBASE_COUNT\tREAD_COUNT\tCYCLE_COUNT\tSAMPLE_ID\tSEQUENCING_TYPE\tINSTRUMENT_PLATFORM\tINSTRUMENT_MODEL\tPIPELINE_VERSION\tINSERT_SIZE\tLIBRARY_LAYOUT\tWITHDRAWN\tWITHDRAWN_DATE\tCOMMENT\tBARCODE\tLIB_PREP_KIT

example_GATCGA_fileR1.fastq.gz\t5ac314c6113474753364c55d41deff04\tTheSequencingCenter\t130312_D00133_0018_ADTWTJACXX\t2013-03-12\t1\t8781211000\t87812110\t101\tSampleIdentifier\tEXON\tIllumina\tHiSeq2000\tCASAVA-1.8.2\t162\tPAIRED\t0\t\t\t\tAgilent SureSelect V3

example_GATCGA_fileR2.fastq.gz\t5be4751eef9535af3df3f78047cc9137\tTheSequencingCenter\t130312_D00133_0018_ADTWTJACXX\t2013-03-12\t1\t8781211000\t87812110\t101\tSampleIdentifier\tEXON\tIllumina\tHiSeq2000\tCASAVA-1.8.2\t162\tPAIRED\t0\t\t\t\tAgilent SureSelect V3

    """

        FileType fileType = new FileType([
            signature: ".fastq",
            sub_Type: "fastq",
            type: "SEQUENCE",
            vbp_path: "/sequence/"
            ])
        assertNotNull(fileType.save(flush: true))

        SeqPlatform seqPlatform = SeqPlatform.build()

        SeqCenter seqCenter = new SeqCenter(
                        name: "seqCenter",
                        dirName: "seqCenter"
                        )
        assertNotNull(seqCenter.save([flush: true]))

        Run run = new Run()
        run.name = runName
        run.seqCenter = seqCenter
        run.seqPlatform = seqPlatform
        run.storageRealm = Run.StorageRealm.DKFZ
        assertNotNull(run.save([flush: true]))

        RunSegment runSegment = new RunSegment()
        runSegment.initialFormat = RunSegment.DataFormat.FILES_IN_DIRECTORY
        runSegment.currentFormat = RunSegment.DataFormat.FILES_IN_DIRECTORY
        runSegment.dataPath = baseDir.getPath()
        runSegment.filesStatus = RunSegment.FilesStatus.NEEDS_INSTALLATION
        runSegment.mdPath = baseDir.getPath()
        runSegment.run = run
        assertNotNull(runSegment.save([flush: true]))

        MetaDataFile metaDataFile = new MetaDataFile(
                        fileName: fileName,
                        filePath: baseDir.getPath(),
                        runSegment: runSegment
                        )
        assertNotNull(metaDataFile.save([flush: true]))

        metaDataService.processMetaDataFile(metaDataFile)
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
        Project project = TestData.createProject(name: "test", dirName: "test", realmName: "test")
        assertNotNull(project.save(flush: true))
        return project
    }
}
