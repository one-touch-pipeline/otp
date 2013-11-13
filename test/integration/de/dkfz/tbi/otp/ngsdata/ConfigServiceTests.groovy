package de.dkfz.tbi.otp.ngsdata

import static org.junit.Assert.*
import grails.util.Environment

import org.junit.*

class ConfigServiceTests {

    def configService

    Project projectDKFZ
    Project projectBQ

    @Before
    void setUp() {

        // Data on DKFZ LDSF, cluster in the DKFZ (regular case)
        Realm realmDKFZ = new Realm(
                name: "DKFZ",
                env: Environment.getCurrent().getName(),
                operationType: Realm.OperationType.DATA_PROCESSING,
                cluster: Realm.Cluster.DKFZ,
                rootPath: '/rootPath',
                processingRootPath: '/processingRootPath',
                programsRootPath: '/programsRootPath',
                webHost: '',
                host: 'localhost',
                port: '22',
                unixUser: '!invalid',
                timeout: 60,
                pbsOptions: ''
                )
        assertNotNull(realmDKFZ.save(flush: true))

        // Data on BioQuant LSDF, cluster in the BioQuant (regular case)
        Realm realmBQ = new Realm(
                name: "BioQuant",
                env: Environment.getCurrent().getName(),
                operationType: Realm.OperationType.DATA_PROCESSING,
                cluster: Realm.Cluster.BIOQUANT,
                rootPath: '/rootPath',
                processingRootPath: '/processingRootPath',
                programsRootPath: '/programsRootPath',
                webHost: '',
                host: 'localhost',
                port: '22',
                unixUser: '!invalid',
                timeout: 60,
                pbsOptions: ''
                )
        assertNotNull(realmBQ.save(flush: true))

        projectDKFZ = new Project(
                name: 'projectDKFZ',
                dirName: 'projectDKFZ',
                realmName: 'DKFZ'
                )
        assertNotNull(projectDKFZ.save([flush: true]))

        projectBQ = new Project(
                name: 'projectBQ',
                dirName: 'projectBQ',
                realmName: 'BioQuant'
                )
        assertNotNull(projectBQ.save([flush: true]))
    }

    @After
    void tearDown() {
        projectDKFZ = null
        projectBQ = null
    }

    @Ignore
    @Test
    void testRealmFromInitialFTPPath() {
        String path = "$BQ_ROOTPATH/ftp/"
        Realm realm = configService.getRealmForInitialFTPPath(path)
        assertEquals(realm.name, "BioQuant")

        path = "STORAGE_ROOT/ftp/"
        realm = configService.getRealmForInitialFTPPath(path)
        assertEquals(realm.name, "DKFZ")
    }

    @Test(expected=IllegalArgumentException)
    void testClusterSpecificPrefixesIsNull() {
        configService.clusterSpecificCommandPrefixes(null)
    }

    @Test
    void testClusterSpecificPrefixesDKFZ() {
        Map<String, String> act = configService.clusterSpecificCommandPrefixes(projectDKFZ)
        Map<String, String> exp = [
            'exec': 'sh -c',
            'cp': 'cp',
            'dest': '',
        ]
        assertTrue exp == act
    }

    @Test
    void testClusterSpecificPrefixesBioQuant() {
        Map<String,String> act = configService.clusterSpecificCommandPrefixes(projectBQ)
        Map<String, String> exp = [
            'exec': 'ssh -p 22 !invalid@localhost',
            'cp': 'scp -P 22',
            'dest': '!invalid@localhost:',
        ]
        assertTrue exp == act
    }
}
