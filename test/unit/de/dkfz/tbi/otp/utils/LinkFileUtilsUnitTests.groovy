package de.dkfz.tbi.otp.utils

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.job.processing.CreateClusterScriptService
import de.dkfz.tbi.otp.job.processing.ExecutionService
import de.dkfz.tbi.otp.ngsdata.LsdfFilesService
import de.dkfz.tbi.otp.ngsdata.Realm
import grails.buildtestdata.mixin.Build
import org.codehaus.groovy.runtime.powerassert.PowerAssertionError
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.springframework.context.ApplicationContext

@Build([Realm])
class LinkFileUtilsUnitTests {

    LinkFileUtils linkFileUtils

    File testDirectory
    Realm realm

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder()

    @Before
    void setUp() {
        tmpDir.create()
        testDirectory = tmpDir.newFolder()

        realm = Realm.build()

        ExecutionService executionService = [
                executeCommand: { Realm realm, String command ->
                    assert ProcessHelperService.executeAndAssertExitCodeAndErrorOutAndReturnStdout(command) ==~ /^0?\s*$/
                }
        ] as ExecutionService

        linkFileUtils = new LinkFileUtils()
        linkFileUtils.createClusterScriptService = new CreateClusterScriptService()
        linkFileUtils.lsdfFilesService = new LsdfFilesService()
        linkFileUtils.lsdfFilesService.createClusterScriptService = new CreateClusterScriptService()
        linkFileUtils.lsdfFilesService.executionService = executionService

        linkFileUtils.applicationContext = [:] as ApplicationContext
        linkFileUtils.applicationContext.metaClass.executionService = executionService
    }

    @After
    void tearDown() {
        realm = null
        testDirectory = null
        linkFileUtils = null
    }


    @Test
    void testCreateAndValidateLinks_MapIsNull_shouldFail() {
        TestCase.shouldFail (PowerAssertionError) {
            linkFileUtils.createAndValidateLinks(null, realm)
        }

    }

    @Test
    void testCreateAndValidateLinks_RealmIsNull_shouldFail() {
        TestCase.shouldFail (PowerAssertionError) {
            linkFileUtils.createAndValidateLinks([:], null)
        }
    }

    @Test
    void testCreateAndValidateLinks_baseDirExist_LinkDoesNotExist() {
        File sourceFile = new File(testDirectory, "sourceFile")
        sourceFile.createNewFile()
        File linkFile = new File(testDirectory, "linkFile")

        linkFileUtils.createAndValidateLinks([(sourceFile):linkFile], realm)
        assert linkFile.exists()
    }

    @Test
    void testCreateAndValidateLinks_baseDirExist_FileExistsInPlaceOfLink() {
        String oldContent = "OldContent"
        File sourceFile = new File(testDirectory, "sourceFile")
        sourceFile.createNewFile()
        File linkFile = new File(testDirectory, "linkFile")
        linkFile << oldContent

        linkFileUtils.createAndValidateLinks([(sourceFile):linkFile], realm)
        assert linkFile.exists()
        assert linkFile.text != oldContent
    }

    @Test
    void testCreateAndValidateLinks_baseDirExist_DirectoryExistsInPlaceOfLink() {
        File sourceFile = new File(testDirectory, "sourceFile")
        sourceFile.createNewFile()
        File linkFile = new File(testDirectory, "linkFile")
        assert linkFile.mkdirs()

        linkFileUtils.createAndValidateLinks([(sourceFile):linkFile], realm)
        assert linkFile.exists() && !linkFile.isDirectory()
    }

    @Test
    void testCreateAndValidateLinks_baseDirDoesNotExist() {
        File sourceFile = new File(testDirectory, "sourceFile")
        sourceFile.createNewFile()
        File linkDir = new File(testDirectory, "linkDir")
        assert !linkDir.exists()
        File linkFile = new File(linkDir, "linkFile")

        linkFileUtils.createAndValidateLinks([(sourceFile):linkFile], realm)
        assert linkFile.exists()
    }


    @Test
    void testCreateAndValidateLinks_linkCouldNotBeCreated_shouldFail() {
        File sourceFile = new File(testDirectory, "sourceFile")
        sourceFile.createNewFile()
        File linkFile = new File(testDirectory, "linkFile")

        linkFileUtils.applicationContext.executionService.metaClass.executeCommand = { Realm realm, String command ->
            assert realm == realm
        }

        TestCase.shouldFail(PowerAssertionError) {
            linkFileUtils.createAndValidateLinks([(sourceFile): linkFile], realm)
        }
    }

}
