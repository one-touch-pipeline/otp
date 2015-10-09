package de.dkfz.tbi.otp.ngsdata

import static org.junit.Assert.*
import org.junit.*


class ConfigServiceTests {

    ConfigService configService


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
}
