package de.dkfz.tbi.otp.utils

import de.dkfz.tbi.otp.job.processing.ExecutionService
import de.dkfz.tbi.otp.ngsdata.Realm
import org.junit.Test
import grails.buildtestdata.mixin.Build
import org.codehaus.groovy.runtime.powerassert.PowerAssertionError
import org.springframework.context.ApplicationContext

@Build([Realm])
class LinkFileUtilsUnitTests {

    LinkFileUtils linkFileUtils

    String UNIQUE_PATH = HelperUtils.getUniqueString()
    File testDirectory
    Realm realm

    @Before
    void setUp() {
        testDirectory = new File("/tmp/otp-test/${UNIQUE_PATH}")
        assert testDirectory.mkdirs()  // This will fail if the directory already exists or if it could not be created.

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
