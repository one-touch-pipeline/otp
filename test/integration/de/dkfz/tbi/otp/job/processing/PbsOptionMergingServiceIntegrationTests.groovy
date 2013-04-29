package de.dkfz.tbi.otp.job.processing

import static org.junit.Assert.*
import org.junit.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.testing.AbstractIntegrationTest


class PbsOptionMergingServiceIntegrationTests extends AbstractIntegrationTest {

    PbsOptionMergingService pbsOptionMergingService

    ProcessingOptionService processingOptionService

    @Before
    void setUp() {
    }

    @After
    void tearDown() {
    }

    @Test
    void testMergePbsOptions() {
        Realm realm = createRealm()
        assertEquals("", pbsOptionMergingService.mergePbsOptions(realm))

        realm = createRealm("{'-a': a}")
        assertEquals("-a a ", pbsOptionMergingService.mergePbsOptions(realm))

        realm = createRealm()
        ProcessingOption processingOption = createProcessingOption("job")
        assertEquals("", pbsOptionMergingService.mergePbsOptions(realm, "job"))

        realm = createRealm("{'-a': a}")
        processingOption = createProcessingOption("job")
        assertEquals("-a a ", pbsOptionMergingService.mergePbsOptions(realm, "job"))

        realm = createRealm()
        processingOption = createProcessingOption("job", "{-a: a}")
        assertEquals("-a a ", pbsOptionMergingService.mergePbsOptions(realm, "job"))

        realm = createRealm("{-a: a}")
        processingOption = createProcessingOption("job", "{-b: b}")
        assertEquals("-a a -b b ", pbsOptionMergingService.mergePbsOptions(realm, "job"))

        realm = createRealm("{-a: a}")
        processingOption = createProcessingOption("job", "{-a: b}")
        assertEquals("-a b ", pbsOptionMergingService.mergePbsOptions(realm, "job"))

        realm = createRealm("{-a: {aa: aa}}")
        processingOption = createProcessingOption("job")
        assertEquals("-a aa=aa ", pbsOptionMergingService.mergePbsOptions(realm, "job"))

        realm = createRealm()
        processingOption = createProcessingOption("job", "{-a: {aa: aa}}")
        assertEquals("-a aa=aa ", pbsOptionMergingService.mergePbsOptions(realm, "job"))

        realm = createRealm("{-a: {aa: aa}}")
        processingOption = createProcessingOption("job", "{-a: {aa: aa2}}")
        assertEquals("-a aa=aa2 ", pbsOptionMergingService.mergePbsOptions(realm, "job"))

        realm = createRealm("{-a: {aa: aa}}")
        processingOption = createProcessingOption("job", "{-a: {ab: ab}}")
        assertEquals("-a aa=aa -a ab=ab ", pbsOptionMergingService.mergePbsOptions(realm, "job"))

        realm = createRealm("{-l: {nodes: '1:lsdf', walltime: '48:00:00'}}")
        processingOption = createProcessingOption("job", "{-l: {nodes: '1:ppn=6:lsdf', walltime: '50:00:00', mem: '3g'}, -q: convey}")
        assertEquals("-q convey -l nodes=1:ppn=6:lsdf -l mem=3g -l walltime=50:00:00 ", pbsOptionMergingService.mergePbsOptions(realm, "job"))

        Realm realm_dkfz = createRealm("{-a: b}", Realm.Cluster.DKFZ)
        Realm realm_bq = createRealm("{-c: d}", Realm.Cluster.BIOQUANT)
        processingOption = createProcessingOption("job", "{-e: f}", Realm.Cluster.DKFZ)
        processingOption = createProcessingOption("job", "{-g: h}", Realm.Cluster.BIOQUANT)
        assertEquals("-a b -e f ", pbsOptionMergingService.mergePbsOptions(realm_dkfz, "job"))
        assertEquals("-c d -g h ", pbsOptionMergingService.mergePbsOptions(realm_bq, "job"))

        realm = createRealm("")
        processingOption = createProcessingOption("job", "{-a: b}")
        shouldFail(IllegalArgumentException.class, { pbsOptionMergingService.mergePbsOptions(realm, "job") })

        realm = createRealm("{-a: b}")
        processingOption = createProcessingOption("job", "")
        shouldFail(IllegalArgumentException.class, { pbsOptionMergingService.mergePbsOptions(realm, "job") })

        realm = createRealm("{}", Realm.Cluster.BIOQUANT)
        processingOption = createProcessingOption("job_dkfz", "{}", , Realm.Cluster.DKFZ)
        shouldFail(IllegalArgumentException.class, { pbsOptionMergingService.mergePbsOptions(realm, "job_dkfz") })

        realm = createRealm("{}", Realm.Cluster.DKFZ)
        processingOption = createProcessingOption("job_bq", "{}", Realm.Cluster.BIOQUANT)
        shouldFail(IllegalArgumentException.class, { pbsOptionMergingService.mergePbsOptions(realm, "job_bq") })
    }

    private Realm createRealm(String pbsOptions = "{}", Realm.Cluster cluster = Realm.Cluster.DKFZ) {
        Realm realm = new Realm(
                name: "DKFZ",
                env: "development",
                operationType: Realm.OperationType.DATA_MANAGEMENT,
                cluster: cluster,
                rootPath: "/",
                processingRootPath: "/test",
                programsRootPath: "/testPrograms",
                webHost: "http://test.me",
                host: "http://test.me",
                port: 22,
                unixUser: "testuser",
                timeout: 100,
                pbsOptions: pbsOptions
                )
        assertNotNull(realm.save())
        return realm
    }

    private ProcessingOption createProcessingOption(String jobKey, String pbsOptions = "{}", Realm.Cluster cluster = Realm.Cluster.DKFZ) {
        String name = PbsOptionMergingService.PBS_PREFIX + jobKey

        //check, if already a option with this name exist and if yes, delete it
        ProcessingOption oldProcessingOption = processingOptionService.findStrict(name, cluster.toString(), null)
        if (oldProcessingOption!= null) {
            oldProcessingOption.delete()
        }

        ProcessingOption processingOption = new ProcessingOption(
                name: name,
                type: cluster.toString(),
                value: pbsOptions,
                comment: 'comment'
                )
        assertNotNull(processingOption.save())
        return processingOption
    }
}
