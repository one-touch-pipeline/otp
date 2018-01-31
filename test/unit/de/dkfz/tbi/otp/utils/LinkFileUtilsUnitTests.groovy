package de.dkfz.tbi.otp.utils

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import grails.buildtestdata.mixin.*
import org.codehaus.groovy.runtime.powerassert.*
import org.junit.*
import org.junit.rules.*

import java.nio.file.*


@Build([Realm])
class LinkFileUtilsUnitTests {

    LinkFileUtils linkFileUtils

    File testDirectory
    Realm realm

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder()

    @Before
    void setUp() {
        testDirectory = tmpDir.newFolder()
        if(!testDirectory.exists()) {
            assert testDirectory.mkdirs()
        }

        realm = DomainFactory.createRealm()

        ExecutionService executionService = [
                executeCommand: { Realm realm, String command ->
                    String stdout = ProcessHelperService.executeAndAssertExitCodeAndErrorOutAndReturnStdout(command)
                    assert stdout ==~ /^0?\s*$/
                    return stdout
                }
        ] as ExecutionService

        linkFileUtils = new LinkFileUtils()
        linkFileUtils.createClusterScriptService = new CreateClusterScriptService()
        linkFileUtils.lsdfFilesService = new LsdfFilesService()
        linkFileUtils.lsdfFilesService.createClusterScriptService = new CreateClusterScriptService()
        linkFileUtils.lsdfFilesService.executionService = executionService

        linkFileUtils.executionService = executionService
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
        assert Paths.get(sourceFile.absolutePath) == Paths.get(linkFile.absolutePath).toRealPath()
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
        assert Paths.get(sourceFile.absolutePath) == Paths.get(linkFile.absolutePath).toRealPath()
    }

    @Test
    void testCreateAndValidateLinks_baseDirExist_DirectoryExistsInPlaceOfLink() {
        File sourceFile = new File(testDirectory, "sourceFile")
        sourceFile.createNewFile()
        File linkFile = new File(testDirectory, "linkFile")
        assert linkFile.mkdirs()

        linkFileUtils.createAndValidateLinks([(sourceFile):linkFile], realm)
        assert linkFile.exists() && !linkFile.isDirectory()
        assert Paths.get(sourceFile.absolutePath) == Paths.get(linkFile.absolutePath).toRealPath()
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
        assert Paths.get(sourceFile.absolutePath) == Paths.get(linkFile.absolutePath).toRealPath()
    }


    @Test
    void testCreateAndValidateLinks_linkCouldNotBeCreated_shouldFail() {
        File sourceFile = new File(testDirectory, "sourceFile")
        sourceFile.createNewFile()
        File linkFile = new File(testDirectory, "linkFile")

        linkFileUtils.lsdfFilesService = new LsdfFilesService()
        linkFileUtils.lsdfFilesService.createClusterScriptService = new CreateClusterScriptService()
        linkFileUtils.lsdfFilesService.executionService = [
                executeCommand: { Realm realm, String command ->
                    assert realm == realm
                }
        ] as ExecutionService

        TestCase.shouldFail(PowerAssertionError) {
            linkFileUtils.createAndValidateLinks([(sourceFile): linkFile], realm)
        }
    }

}
