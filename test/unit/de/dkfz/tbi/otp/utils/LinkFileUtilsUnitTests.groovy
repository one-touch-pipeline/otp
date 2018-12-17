package de.dkfz.tbi.otp.utils

import grails.buildtestdata.mixin.Build
import org.codehaus.groovy.runtime.powerassert.PowerAssertionError
import org.junit.*
import org.junit.rules.TemporaryFolder

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.job.processing.CreateClusterScriptService
import de.dkfz.tbi.otp.job.processing.RemoteShellHelper
import de.dkfz.tbi.otp.ngsdata.*

import java.nio.file.Paths

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

        RemoteShellHelper remoteShellHelper = [
                executeCommand: { Realm realm, String command ->
                    String stdout = LocalShellHelper.executeAndAssertExitCodeAndErrorOutAndReturnStdout(command)
                    assert stdout ==~ /^0?\s*$/
                    return stdout
                }
        ] as RemoteShellHelper

        linkFileUtils = new LinkFileUtils()
        linkFileUtils.createClusterScriptService = new CreateClusterScriptService()
        linkFileUtils.lsdfFilesService = new LsdfFilesService()
        linkFileUtils.lsdfFilesService.createClusterScriptService = new CreateClusterScriptService()
        linkFileUtils.lsdfFilesService.remoteShellHelper = remoteShellHelper

        linkFileUtils.remoteShellHelper = remoteShellHelper
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
        linkFileUtils.lsdfFilesService.remoteShellHelper = [
                executeCommand: { Realm realm, String command ->
                    assert this.realm == realm
                }
        ] as RemoteShellHelper

        TestCase.shouldFail(PowerAssertionError) {
            linkFileUtils.createAndValidateLinks([(sourceFile): linkFile], realm)
        }
    }

}
