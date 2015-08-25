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
        Realm realmDKFZ = DomainFactory.createRealmDataManagement(name: 'DKFZ')
        assertNotNull(realmDKFZ.save(flush: true))

        // Data on BioQuant LSDF, cluster in the BioQuant (regular case); fake values
        Realm realmBQ = DomainFactory.createRealmDataManagement([
            name: 'BioQuant',
            unixUser: '!invalid',
            host: 'localhost',
            port: 22,
            ])
        assertNotNull(realmBQ.save(flush: true))

        projectDKFZ = TestData.createProject(
                name: 'projectDKFZ',
                dirName: 'projectDKFZ',
                realmName: 'DKFZ'
                )
        assertNotNull(projectDKFZ.save([flush: true]))

        projectBQ = TestData.createProject(
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
        assert exp == act
    }
}
