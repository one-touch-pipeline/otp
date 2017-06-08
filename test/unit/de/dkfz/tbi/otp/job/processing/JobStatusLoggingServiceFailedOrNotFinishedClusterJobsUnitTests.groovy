package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifier
import de.dkfz.tbi.otp.ngsdata.Realm
import grails.buildtestdata.mixin.Build
import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for the {@link JobStatusLoggingService}.
 *
 */
@TestFor(JobStatusLoggingService)
@TestMixin(GrailsUnitTestMixin)
@Build([ProcessingStep, Realm])
class JobStatusLoggingServiceFailedOrNotFinishedClusterJobsUnitTests extends TestCase {

    final static Long ARBITRARY_REALM_ID = 987

    File tempDirectory
    ProcessingStep processingStep
    Realm realm1
    Realm realm2
    Realm realmWithEmptyLogFile
    Realm realmWithoutLogFile
    Realm realmWithoutLogDir
    Realm realmWithSameLogDirAs1
    ClusterJobIdentifier successfulJobOnRealm1
    ClusterJobIdentifier failedJobOnRealm1
    Collection<String> jobIdsOnRealm1
    ClusterJobIdentifier successfulJobOnRealm2
    ClusterJobIdentifier failedJob1OnRealm2
    ClusterJobIdentifier failedJob2OnRealm2
    ClusterJobIdentifier mixedUpJob1
    ClusterJobIdentifier mixedUpJob2
    Collection<String> jobIdsOnRealm2
    Collection<ClusterJobIdentifier> unsuccessfulJobsOnRealm2
    ClusterJobIdentifier jobOnRealmWithEmptyLogFile
    ClusterJobIdentifier jobOnRealmWithoutLogFile
    ClusterJobIdentifier jobOnRealmWithoutLogDir
    ClusterJobIdentifier successfulJobOnRealmWithSameLogDirAs1
    ClusterJobIdentifier failedJobOnRealmWithSameLogDirAs1
    Collection<String> jobIdsOnRealmWithSameLogDirAs1
    Collection<ClusterJobIdentifier> allUnsuccessfulJobs

    @Before
    void setUp() {
        tempDirectory = TestCase.createEmptyTestDirectory()

        processingStep = JobStatusLoggingServiceUnitTests.createFakeProcessingStep()

        int realmId = ARBITRARY_REALM_ID
        realm1 = Realm.build([id: realmId++, name: 'realm1', loggingRootPath: new File(tempDirectory, '1').path])
        assert new File(service.logFileBaseDir(realm1, processingStep)).mkdirs()
        realm2 = Realm.build([id: realmId++, name: 'realm2', loggingRootPath: new File(tempDirectory, '2').path])
        assert new File(service.logFileBaseDir(realm2, processingStep)).mkdirs()
        realmWithEmptyLogFile = new Realm([id: realmId++, name: 'realmWithEmptyLogFile', loggingRootPath: new File(tempDirectory, '3').path, unixUser: "notEmtpy"])
        assert new File(service.logFileBaseDir(realmWithEmptyLogFile, processingStep)).mkdirs()
        realmWithoutLogFile = new Realm([id: realmId++, name: 'realmWithoutLogFile', loggingRootPath: new File(tempDirectory, '4').path, unixUser: "notEmtpy"])
        assert new File(service.logFileBaseDir(realmWithoutLogFile, processingStep)).mkdirs()
        realmWithoutLogDir = new Realm([id: realmId++, name: 'realmWithoutLogDir', loggingRootPath: new File(tempDirectory, '5').path, unixUser: "notEmtpy"])
        realmWithSameLogDirAs1 = new Realm([id: realmId++, name: 'realmWithSameLogDirAs1', loggingRootPath: realm1.loggingRootPath, unixUser: "notEmtpy"])

        final String id1 = '1001'
        final String id2 = '1002'
        /* prefixOfId3 and suffixOfId3 are for testing that the code works correctly even if the
         * constructed log message of a failed job (failedJob*OnRealm2) is a prefix/suffix of the log message of a successful
         * job (successfulJobOnRealm2).
         */
        final String prefixOfId3 = '12'
        final String suffixOfId3 = '34'
        final String id3 = prefixOfId3 + suffixOfId3
        final String id5 = '1005'
        successfulJobOnRealm1 = new ClusterJobIdentifier(realm1, id1, realm1.unixUser)
        failedJobOnRealm1 = new ClusterJobIdentifier(realm1, id2, realm1.unixUser)
        jobIdsOnRealm1 = [
                successfulJobOnRealm1.clusterJobId,
                failedJobOnRealm1.clusterJobId,
        ]
        successfulJobOnRealm2 = new ClusterJobIdentifier(realm2, id3, realm2.unixUser)
        failedJob1OnRealm2 = new ClusterJobIdentifier(realm2, prefixOfId3, realm2.unixUser)
        failedJob2OnRealm2 = new ClusterJobIdentifier(realm2, suffixOfId3, realm2.unixUser)
        mixedUpJob1 = new ClusterJobIdentifier(realm2, successfulJobOnRealm1.clusterJobId, realm2.unixUser)
        mixedUpJob2 = new ClusterJobIdentifier(realm2, failedJobOnRealm1.clusterJobId, realm2.unixUser)
        jobIdsOnRealm2 = [
                successfulJobOnRealm2.clusterJobId,
                failedJob1OnRealm2.clusterJobId,
                failedJob2OnRealm2.clusterJobId,
                mixedUpJob1.clusterJobId,
                mixedUpJob2.clusterJobId,
        ]
        unsuccessfulJobsOnRealm2 = [
                failedJob1OnRealm2,
                failedJob2OnRealm2,
                mixedUpJob1,
                mixedUpJob2,
        ]
        jobOnRealmWithEmptyLogFile = new ClusterJobIdentifier(realmWithEmptyLogFile, id5, realmWithEmptyLogFile.unixUser)
        jobOnRealmWithoutLogFile = new ClusterJobIdentifier(realmWithoutLogFile, id5, realmWithoutLogFile.unixUser)
        jobOnRealmWithoutLogDir = new ClusterJobIdentifier(realmWithoutLogDir, id5, realmWithoutLogDir.unixUser)
        successfulJobOnRealmWithSameLogDirAs1 = new ClusterJobIdentifier(realmWithSameLogDirAs1, failedJobOnRealm1.clusterJobId, realmWithSameLogDirAs1.unixUser)
        failedJobOnRealmWithSameLogDirAs1 = new ClusterJobIdentifier(realmWithSameLogDirAs1, successfulJobOnRealm1.clusterJobId, realmWithSameLogDirAs1.unixUser)
        jobIdsOnRealmWithSameLogDirAs1 = [
                successfulJobOnRealmWithSameLogDirAs1.clusterJobId,
                failedJobOnRealmWithSameLogDirAs1.clusterJobId,
        ]
        allUnsuccessfulJobs = [
                failedJobOnRealm1,
                failedJob1OnRealm2,
                failedJob2OnRealm2,
                mixedUpJob1,
                mixedUpJob2,
                jobOnRealmWithEmptyLogFile,
                jobOnRealmWithoutLogFile,
                jobOnRealmWithoutLogDir,
                failedJobOnRealmWithSameLogDirAs1,
        ]

        new File(service.constructLogFileLocation(realm1, processingStep, successfulJobOnRealm1.clusterJobId)).text = service.constructMessage(realm1, processingStep, successfulJobOnRealm1.clusterJobId) + "\n"
        new File(service.constructLogFileLocation(realm2, processingStep, successfulJobOnRealm2.clusterJobId)).text = "\n" + service.constructMessage(realm2, processingStep, successfulJobOnRealm2.clusterJobId)
        new File(service.constructLogFileLocation(realmWithEmptyLogFile, processingStep, jobOnRealmWithEmptyLogFile.clusterJobId)).text = ''
        new File(service.constructLogFileLocation(realmWithSameLogDirAs1, processingStep, successfulJobOnRealmWithSameLogDirAs1.clusterJobId)).text = service.constructMessage(realmWithSameLogDirAs1, processingStep, successfulJobOnRealmWithSameLogDirAs1.clusterJobId)
    }

    @After
    void tearDown() {
        assert tempDirectory.deleteDir()
    }

    @Test
    void testFailedOrNotFinishedClusterJobsWithArgumentsRealmAndCollectionOfIds_Realm1() {
        final Collection<ClusterJobIdentifier> unsuccessfulClusterJobs
        unsuccessfulClusterJobs = service.failedOrNotFinishedClusterJobs(processingStep, [(realm1): jobIdsOnRealm1.collect { new ClusterJobIdentifier(realm1, it, realm1.unixUser) }])
        assert unsuccessfulClusterJobs == [failedJobOnRealm1]
    }

    @Test
    void testFailedOrNotFinishedClusterJobsWithArgumentsRealmAndCollectionOfIds_Realm2() {
        final Collection<ClusterJobIdentifier> unsuccessfulClusterJobs
        unsuccessfulClusterJobs = service.failedOrNotFinishedClusterJobs(processingStep, [(realm2): jobIdsOnRealm2.collect { new ClusterJobIdentifier(realm2, it, realm2.unixUser) }])
        assertAndContinue { assert unsuccessfulClusterJobs.size() == unsuccessfulJobsOnRealm2.size() }
        assertAndContinue { assert unsuccessfulClusterJobs.containsAll(unsuccessfulJobsOnRealm2) }
    }

    @Test
    void testFailedOrNotFinishedClusterJobsWithArgumentsRealmAndCollectionOfIds_RealmWithEmptyLogFile() {
        final Collection<ClusterJobIdentifier> unsuccessfulClusterJobs
        unsuccessfulClusterJobs = service.failedOrNotFinishedClusterJobs(processingStep,
                [(realmWithEmptyLogFile): [jobOnRealmWithEmptyLogFile],]
        )
        assert unsuccessfulClusterJobs == [jobOnRealmWithEmptyLogFile]
    }

    @Test
    void testFailedOrNotFinishedClusterJobsWithArgumentsRealmAndCollectionOfIds_RealmWithSameLogDirAs1() {
        final Collection<ClusterJobIdentifier> unsuccessfulClusterJobs
        unsuccessfulClusterJobs = service.failedOrNotFinishedClusterJobs(processingStep, [(realmWithSameLogDirAs1): jobIdsOnRealmWithSameLogDirAs1.collect { new ClusterJobIdentifier(realmWithSameLogDirAs1, it, realmWithSameLogDirAs1.unixUser) }])
        assert unsuccessfulClusterJobs == [failedJobOnRealmWithSameLogDirAs1]
    }

    @Test
    void testFailedOrNotFinishedClusterJobsWithArgumentCollectionOfClusterJobIdentifiers() {
        final Collection<ClusterJobIdentifier> unsuccessfulClusterJobs
        unsuccessfulClusterJobs = service.failedOrNotFinishedClusterJobs(processingStep, [
                successfulJobOnRealm1,
                failedJobOnRealm1,
                successfulJobOnRealm2,
                failedJob1OnRealm2,
                failedJob2OnRealm2,
                mixedUpJob1,
                mixedUpJob2,
                jobOnRealmWithEmptyLogFile,
                jobOnRealmWithoutLogFile,
                jobOnRealmWithoutLogDir,
                successfulJobOnRealmWithSameLogDirAs1,
                failedJobOnRealmWithSameLogDirAs1,
        ])
        assertAndContinue { assert unsuccessfulClusterJobs.size() == allUnsuccessfulJobs.size() }
        assertAndContinue { assert unsuccessfulClusterJobs.containsAll(allUnsuccessfulJobs) }
    }

    @Test
    void testFailedOrNotFinishedClusterJobsWithArgumentMap() {
        final Collection<ClusterJobIdentifier> unsuccessfulClusterJobs
        unsuccessfulClusterJobs = service.failedOrNotFinishedClusterJobs(processingStep, [
                (realm1): jobIdsOnRealm1.collect { new ClusterJobIdentifier(realm1, it, realm1.unixUser) },
                (realm2): jobIdsOnRealm2.collect { new ClusterJobIdentifier(realm2, it, realm2.unixUser) },
                (realmWithEmptyLogFile): [new ClusterJobIdentifier(realmWithEmptyLogFile, jobOnRealmWithEmptyLogFile.clusterJobId, realmWithEmptyLogFile.unixUser)],
                (realmWithoutLogFile): [new ClusterJobIdentifier(realmWithoutLogFile, jobOnRealmWithoutLogFile.clusterJobId, realmWithoutLogFile.unixUser)],
                (realmWithoutLogDir): [new ClusterJobIdentifier(realmWithoutLogDir, jobOnRealmWithoutLogDir.clusterJobId, realmWithoutLogDir.unixUser)],
                (realmWithSameLogDirAs1): jobIdsOnRealmWithSameLogDirAs1.collect { new ClusterJobIdentifier(realmWithSameLogDirAs1, it, realmWithSameLogDirAs1.unixUser) },
        ])
        assertAndContinue { assert unsuccessfulClusterJobs.size() == allUnsuccessfulJobs.size() }
        assertAndContinue { assert unsuccessfulClusterJobs.containsAll(allUnsuccessfulJobs) }
    }
}
