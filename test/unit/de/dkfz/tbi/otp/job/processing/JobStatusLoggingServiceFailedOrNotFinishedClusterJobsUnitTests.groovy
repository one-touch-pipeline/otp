package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifier
import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifierImpl
import de.dkfz.tbi.otp.ngsdata.Realm
import grails.buildtestdata.mixin.Build
import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

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

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder()

    @Before
    void setUp() {
        tmpDir.create()
        tempDirectory = tmpDir.newFolder('otp-test')

        processingStep = JobStatusLoggingServiceUnitTests.createFakeProcessingStep()

        int realmId = ARBITRARY_REALM_ID
        realm1 = Realm.build([id: realmId++, name: 'realm1', loggingRootPath: new File(tempDirectory, '1').path])
        assert new File(service.logFileBaseDir(realm1, processingStep)).mkdirs()
        realm2 = Realm.build([id: realmId++, name: 'realm2', loggingRootPath: new File(tempDirectory, '2').path])
        assert new File(service.logFileBaseDir(realm2, processingStep)).mkdirs()
        realmWithEmptyLogFile = new Realm([id: realmId++, name: 'realmWithEmptyLogFile', loggingRootPath: new File(tempDirectory, '3').path])
        assert new File(service.logFileBaseDir(realmWithEmptyLogFile, processingStep)).mkdirs()
        realmWithoutLogFile = new Realm([id: realmId++, name: 'realmWithoutLogFile', loggingRootPath: new File(tempDirectory, '4').path])
        assert new File(service.logFileBaseDir(realmWithoutLogFile, processingStep)).mkdirs()
        realmWithoutLogDir = new Realm([id: realmId++, name: 'realmWithoutLogDir', loggingRootPath: new File(tempDirectory, '5').path])
        realmWithSameLogDirAs1 = new Realm([id: realmId++, name: 'realmWithSameLogDirAs1', loggingRootPath: realm1.loggingRootPath])

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
        successfulJobOnRealm1 = new ClusterJobIdentifierImpl(realm1, id1)
        failedJobOnRealm1 = new ClusterJobIdentifierImpl(realm1, id2)
        jobIdsOnRealm1 = [
                successfulJobOnRealm1.clusterJobId,
                failedJobOnRealm1.clusterJobId,
        ]
        successfulJobOnRealm2 = new ClusterJobIdentifierImpl(realm2, id3)
        failedJob1OnRealm2 = new ClusterJobIdentifierImpl(realm2, prefixOfId3)
        failedJob2OnRealm2 = new ClusterJobIdentifierImpl(realm2, suffixOfId3)
        mixedUpJob1 = new ClusterJobIdentifierImpl(realm2, successfulJobOnRealm1.clusterJobId)
        mixedUpJob2 = new ClusterJobIdentifierImpl(realm2, failedJobOnRealm1.clusterJobId)
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
        jobOnRealmWithEmptyLogFile = new ClusterJobIdentifierImpl(realmWithEmptyLogFile, id5)
        jobOnRealmWithoutLogFile = new ClusterJobIdentifierImpl(realmWithoutLogFile, id5)
        jobOnRealmWithoutLogDir = new ClusterJobIdentifierImpl(realmWithoutLogDir, id5)
        successfulJobOnRealmWithSameLogDirAs1 = new ClusterJobIdentifierImpl(realmWithSameLogDirAs1, failedJobOnRealm1.clusterJobId)
        failedJobOnRealmWithSameLogDirAs1 = new ClusterJobIdentifierImpl(realmWithSameLogDirAs1, successfulJobOnRealm1.clusterJobId)
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

        new File(service.logFileLocation(realm1, processingStep)).text = service.constructMessage(processingStep, successfulJobOnRealm1.clusterJobId) + "\n"
        new File(service.logFileLocation(realm2, processingStep)).text = "\n" + service.constructMessage(processingStep, successfulJobOnRealm2.clusterJobId)
        new File(service.logFileLocation(realmWithEmptyLogFile, processingStep)).text = ''
        new File(service.logFileLocation(realmWithSameLogDirAs1, processingStep)).text = service.constructMessage(processingStep, successfulJobOnRealmWithSameLogDirAs1.clusterJobId)
    }

    @Test
    void testFailedOrNotFinishedClusterJobsWithArgumentsRealmAndCollectionOfIds_Realm1() {
        final Collection<ClusterJobIdentifier> unsuccessfulClusterJobs
        unsuccessfulClusterJobs = service.failedOrNotFinishedClusterJobs(processingStep, realm1, jobIdsOnRealm1)
        assert unsuccessfulClusterJobs == [failedJobOnRealm1]
    }

    @Test
    void testFailedOrNotFinishedClusterJobsWithArgumentsRealmAndCollectionOfIds_Realm2() {
        final Collection<ClusterJobIdentifier> unsuccessfulClusterJobs
        unsuccessfulClusterJobs = service.failedOrNotFinishedClusterJobs(processingStep, realm2, jobIdsOnRealm2)
        assertAndContinue { assert unsuccessfulClusterJobs.size() == unsuccessfulJobsOnRealm2.size() }
        assertAndContinue { assert unsuccessfulClusterJobs.containsAll(unsuccessfulJobsOnRealm2) }
    }

    @Test
    void testFailedOrNotFinishedClusterJobsWithArgumentsRealmAndCollectionOfIds_RealmWithEmptyLogFile() {
        final Collection<ClusterJobIdentifier> unsuccessfulClusterJobs
        unsuccessfulClusterJobs = service.failedOrNotFinishedClusterJobs(processingStep, realmWithEmptyLogFile, [
                jobOnRealmWithEmptyLogFile.clusterJobId,
        ])
        assert unsuccessfulClusterJobs == [jobOnRealmWithEmptyLogFile]
    }

    @Test
    void testFailedOrNotFinishedClusterJobsWithArgumentsRealmAndCollectionOfIds_RealmWithSameLogDirAs1() {
        final Collection<ClusterJobIdentifier> unsuccessfulClusterJobs
        unsuccessfulClusterJobs = service.failedOrNotFinishedClusterJobs(processingStep, realmWithSameLogDirAs1, jobIdsOnRealmWithSameLogDirAs1)
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
                (realm1): jobIdsOnRealm1,
                (realm2): jobIdsOnRealm2,
                (realmWithEmptyLogFile): [jobOnRealmWithEmptyLogFile.clusterJobId],
                (realmWithoutLogFile): [jobOnRealmWithoutLogFile.clusterJobId],
                (realmWithoutLogDir): [jobOnRealmWithoutLogDir.clusterJobId],
                (realmWithSameLogDirAs1): jobIdsOnRealmWithSameLogDirAs1,
        ])
        assertAndContinue { assert unsuccessfulClusterJobs.size() == allUnsuccessfulJobs.size() }
        assertAndContinue { assert unsuccessfulClusterJobs.containsAll(allUnsuccessfulJobs) }
    }
}
