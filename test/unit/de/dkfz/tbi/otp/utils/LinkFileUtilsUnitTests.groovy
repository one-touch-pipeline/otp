package de.dkfz.tbi.otp.utils

import de.dkfz.tbi.otp.job.processing.ExecutionService
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
        testDirectory = tmpDir.newFolder("/otp-test")
        if(!testDirectory.exists()) {
            assert testDirectory.mkdirs()
        }

        realm = Realm.build()

        linkFileUtils = new LinkFileUtils()

        linkFileUtils.applicationContext = [:] as ApplicationContext
        linkFileUtils.applicationContext.metaClass.executionService = new ExecutionService()
    }

    @After
    void tearDown() {
        assert testDirectory.deleteDir()
        realm = null
    }


    @Test(expected = PowerAssertionError)
    void testCreateAndValidateLinks_MapIsNull_shouldFail() {
        linkFileUtils.createAndValidateLinks(null, realm)

    }

    @Test(expected = PowerAssertionError)
    void testCreateAndValidateLinks_RealmIsNull_shouldFail() {
            linkFileUtils.createAndValidateLinks([:], null)
    }

    @Test
    void testCreateAndValidateLinks() {
        File sourceFile = new File(testDirectory, "sourceFile")
        File linkFile = new File(testDirectory, "linkFile")

        linkFileUtils.applicationContext.executionService.metaClass.executeCommand = { Realm realm, String command ->
            assert command == "ln -sf ${sourceFile.path} ${linkFile.path}\n"
            assert linkFile.createNewFile()
            assert realm == realm
        }

        linkFileUtils.createAndValidateLinks([(sourceFile):linkFile], realm)
    }

    @Test(expected = IOException)
    void testCreateAndValidateLinks_linkCouldNotBeCreated_shouldFail() {
        File sourceFile = new File(testDirectory, "sourceFile")
        File linkFile = new File(testDirectory, "linkFile")

        linkFileUtils.applicationContext.executionService.metaClass.executeCommand = { Realm realm, String command ->
            assert command == "ln -sf ${sourceFile.path} ${linkFile.path}\n"
            assert realm == realm
        }

        linkFileUtils.createAndValidateLinks([(sourceFile):linkFile], realm)
    }

}
