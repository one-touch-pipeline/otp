package de.dkfz.tbi.otp.ngsdata

import static org.junit.Assert.*
import org.junit.*

class ConfigServiceTests {

    def configService

    @Before
    void setUp() {
        // Setup logic here
    }

    @After
    void tearDown() {
        // Tear down logic here
    }

    @Test
    void testRealmFromInitialFTPPath() {
        String path = "$BQ_ROOTPATH/ftp/"
        Realm realm = configService.getRealmForInitialFTPPath(path)
        println "${path} ${realm.name}"

        path = "STORAGE_ROOT/ftp/"
        realm = configService.getRealmForInitialFTPPath(path)
        println "${path} ${realm.name}"
    }
}
