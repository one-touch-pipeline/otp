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

package de.dkfz.tbi.otp.job.processing

import grails.buildtestdata.mixin.Build
import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import org.junit.*

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifier
import de.dkfz.tbi.otp.ngsdata.Realm

/**
 * Unit tests for the {@link JobStatusLoggingService}.
 */
@TestFor(JobStatusLoggingService)
@TestMixin(GrailsUnitTestMixin)
@Build([ProcessingStep, Realm])
class JobStatusLoggingServiceFailedOrNotFinishedClusterJobsUnitTests extends TestCase {
    TestConfigService configService

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
    ConfigService configService1

    @Before
    void setUp() {
        tempDirectory = TestCase.createEmptyTestDirectory()

        configService = new TestConfigService([(OtpProperty.PATH_CLUSTER_LOGS_OTP): tempDirectory.path])
        service.configService = configService

        processingStep = JobStatusLoggingServiceUnitTests.createFakeProcessingStep()
        assert new File(service.logFileBaseDir(processingStep)).mkdirs()

        int realmId = ARBITRARY_REALM_ID

        realm1 = Realm.build([id: realmId++, name: 'realm1'])

        realm2 = Realm.build([id: realmId++, name: 'realm2'])

        realmWithEmptyLogFile = new Realm([id: realmId++, name: 'realmWithEmptyLogFile'])

        realmWithoutLogFile = new Realm([id: realmId++, name: 'realmWithoutLogFile'])

        realmWithoutLogDir = new Realm([id: realmId++, name: 'realmWithoutLogDir', loggingRootPath: new File(tempDirectory, '5').path])
        realmWithSameLogDirAs1 = new Realm([id: realmId++, name: 'realmWithSameLogDirAs1'])

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
        successfulJobOnRealm1 = new ClusterJobIdentifier(realm1, id1)
        failedJobOnRealm1 = new ClusterJobIdentifier(realm1, id2)
        jobIdsOnRealm1 = [
                successfulJobOnRealm1.clusterJobId,
                failedJobOnRealm1.clusterJobId,
        ]
        successfulJobOnRealm2 = new ClusterJobIdentifier(realm2, id3)
        failedJob1OnRealm2 = new ClusterJobIdentifier(realm2, prefixOfId3)
        failedJob2OnRealm2 = new ClusterJobIdentifier(realm2, suffixOfId3)
        mixedUpJob1 = new ClusterJobIdentifier(realm2, successfulJobOnRealm1.clusterJobId)
        mixedUpJob2 = new ClusterJobIdentifier(realm2, failedJobOnRealm1.clusterJobId)
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
        jobOnRealmWithEmptyLogFile = new ClusterJobIdentifier(realmWithEmptyLogFile, id5)
        jobOnRealmWithoutLogFile = new ClusterJobIdentifier(realmWithoutLogFile, id5)
        jobOnRealmWithoutLogDir = new ClusterJobIdentifier(realmWithoutLogDir, id5)
        successfulJobOnRealmWithSameLogDirAs1 = new ClusterJobIdentifier(realmWithSameLogDirAs1, failedJobOnRealm1.clusterJobId)
        failedJobOnRealmWithSameLogDirAs1 = new ClusterJobIdentifier(realmWithSameLogDirAs1, successfulJobOnRealm1.clusterJobId)
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
        configService.clean()
    }

    @Test
    void testFailedOrNotFinishedClusterJobsWithArgumentsRealmAndCollectionOfIds_Realm1() {
        final Collection<ClusterJobIdentifier> unsuccessfulClusterJobs
        unsuccessfulClusterJobs = service.failedOrNotFinishedClusterJobs(processingStep, [(realm1): jobIdsOnRealm1.collect {
            new ClusterJobIdentifier(realm1, it)
        }])
        assert unsuccessfulClusterJobs == [failedJobOnRealm1]
    }

    @Test
    void testFailedOrNotFinishedClusterJobsWithArgumentsRealmAndCollectionOfIds_Realm2() {
        final Collection<ClusterJobIdentifier> unsuccessfulClusterJobs
        unsuccessfulClusterJobs = service.failedOrNotFinishedClusterJobs(processingStep, [(realm2): jobIdsOnRealm2.collect {
            new ClusterJobIdentifier(realm2, it)
        }])
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
        unsuccessfulClusterJobs = service.failedOrNotFinishedClusterJobs(processingStep, [(realmWithSameLogDirAs1): jobIdsOnRealmWithSameLogDirAs1.collect {
            new ClusterJobIdentifier(realmWithSameLogDirAs1, it)
        }])
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
                (realm1)                : jobIdsOnRealm1.collect {
                    new ClusterJobIdentifier(realm1, it)
                },
                (realm2)                : jobIdsOnRealm2.collect {
                    new ClusterJobIdentifier(realm2, it)
                },
                (realmWithEmptyLogFile) : [new ClusterJobIdentifier(realmWithEmptyLogFile, jobOnRealmWithEmptyLogFile.clusterJobId)],
                (realmWithoutLogFile)   : [new ClusterJobIdentifier(realmWithoutLogFile, jobOnRealmWithoutLogFile.clusterJobId)],
                (realmWithoutLogDir)    : [new ClusterJobIdentifier(realmWithoutLogDir, jobOnRealmWithoutLogDir.clusterJobId)],
                (realmWithSameLogDirAs1): jobIdsOnRealmWithSameLogDirAs1.collect {
                    new ClusterJobIdentifier(realmWithSameLogDirAs1, it)
                },
        ])
        assertAndContinue { assert unsuccessfulClusterJobs.size() == allUnsuccessfulJobs.size() }
        assertAndContinue { assert unsuccessfulClusterJobs.containsAll(allUnsuccessfulJobs) }
    }
}
