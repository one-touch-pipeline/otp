package de.dkfz.tbi.otp.job.processing

import static org.junit.Assert.*

import org.junit.*

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*

class PbsOptionMergingServiceIntegrationTests {

    PbsOptionMergingService pbsOptionMergingService

    ProcessingOptionService processingOptionService


    final String QSUB_PARAMETERS = "{-l: {nodes: '1:ppn=6', walltime: '50:00:00', mem: '3g'}, -q: convey}"

    @Test
    void testMergePbsOptions() {
        ProcessParameterObject processParameterObject = DomainFactory.createSeqTrack()

        ProcessingStep processingStep1 = DomainFactory.createAndSaveProcessingStep('test.NonExistentDummyJob1', processParameterObject)
        ProcessingStep processingStep2 = DomainFactory.createAndSaveProcessingStep('test.NonExistentDummyJob2', processParameterObject)
        ProcessingStep processingStep3 = DomainFactory.createAndSaveProcessingStep('test.NonExistentDummyJob3', processParameterObject)

        Realm realm = createRealm()
        ProcessingOption processingOption = createProcessingOption(processingStep1.nonQualifiedJobClass)
        assertEquals("", pbsOptionMergingService.mergePbsOptions(processingStep1, realm))

        realm = createRealm("{'-a': a}")
        processingOption = createProcessingOption(processingStep1.nonQualifiedJobClass)
        assertEquals("-a a ", pbsOptionMergingService.mergePbsOptions(processingStep1, realm))

        realm = createRealm()
        processingOption = createProcessingOption(processingStep1.nonQualifiedJobClass, "{-a: a}")
        assertEquals("-a a ", pbsOptionMergingService.mergePbsOptions(processingStep1, realm))

        realm = createRealm("{-a: a}")
        processingOption = createProcessingOption(processingStep1.nonQualifiedJobClass, "{-b: b}")
        assertEquals("-a a -b b ", pbsOptionMergingService.mergePbsOptions(processingStep1, realm))

        realm = createRealm("{-a: a}")
        processingOption = createProcessingOption(processingStep1.nonQualifiedJobClass, "{-a: b}")
        assertEquals("-a b ", pbsOptionMergingService.mergePbsOptions(processingStep1, realm))

        realm = createRealm("{-a: {aa: aa}}")
        processingOption = createProcessingOption(processingStep1.nonQualifiedJobClass)
        assertEquals("-a aa=aa ", pbsOptionMergingService.mergePbsOptions(processingStep1, realm))

        realm = createRealm()
        processingOption = createProcessingOption(processingStep1.nonQualifiedJobClass, "{-a: {aa: aa}}")
        assertEquals("-a aa=aa ", pbsOptionMergingService.mergePbsOptions(processingStep1, realm))

        realm = createRealm("{-a: {aa: aa}}")
        processingOption = createProcessingOption(processingStep1.nonQualifiedJobClass, "{-a: {aa: aa2}}")
        assertEquals("-a aa=aa2 ", pbsOptionMergingService.mergePbsOptions(processingStep1, realm))

        realm = createRealm("{-a: {aa: aa}}")
        processingOption = createProcessingOption(processingStep1.nonQualifiedJobClass, "{-a: {ab: ab}}")
        assertEquals("-a aa=aa -a ab=ab ", pbsOptionMergingService.mergePbsOptions(processingStep1, realm))

        realm = createRealm("{-l: {nodes: '1', walltime: '48:00:00'}}")
        processingOption = createProcessingOption(processingStep1.nonQualifiedJobClass, "{-l: {nodes: '1:ppn=6', walltime: '50:00:00', mem: '3g'}, -q: convey}")
        assertEquals("-q convey -l nodes=1:ppn=6 -l mem=3g -l walltime=50:00:00 ", pbsOptionMergingService.mergePbsOptions(processingStep1, realm))

        Realm realm_dkfz = createRealm("{-a: b}", Realm.Cluster.DKFZ)
        Realm realm_bq = createRealm("{-c: d}", Realm.Cluster.BIOQUANT)
        processingOption = createProcessingOption(processingStep1.nonQualifiedJobClass, "{-e: f}", Realm.Cluster.DKFZ)
        processingOption = createProcessingOption(processingStep1.nonQualifiedJobClass, "{-g: h}", Realm.Cluster.BIOQUANT)
        assertEquals("-a b -e f ", pbsOptionMergingService.mergePbsOptions(processingStep1, realm_dkfz))
        assertEquals("-c d -g h ", pbsOptionMergingService.mergePbsOptions(processingStep1, realm_bq))

        realm = createRealm("")
        processingOption = createProcessingOption(processingStep1.nonQualifiedJobClass, "{-a: b}")
        shouldFail(IllegalArgumentException.class, { pbsOptionMergingService.mergePbsOptions(processingStep1, realm) })

        realm = createRealm("{-a: b}")
        processingOption = createProcessingOption(processingStep1.nonQualifiedJobClass, "")
        assertEquals("-a b ", pbsOptionMergingService.mergePbsOptions(processingStep1, realm))

        realm = createRealm("{-w: x}", Realm.Cluster.BIOQUANT)
        processingOption = createProcessingOption(processingStep2.nonQualifiedJobClass, "{}", Realm.Cluster.DKFZ)
        assertEquals("-w x ", pbsOptionMergingService.mergePbsOptions(processingStep2, realm))

        realm = createRealm("{-y: z}", Realm.Cluster.DKFZ)
        processingOption = createProcessingOption(processingStep3.nonQualifiedJobClass, "{}", Realm.Cluster.BIOQUANT)
        assertEquals("-y z ", pbsOptionMergingService.mergePbsOptions(processingStep3, realm))

        processingOption = createProcessingOption(processingStep2.nonQualifiedJobClass, "{}", Realm.Cluster.DKFZ)
        String expected = pbsOptionMergingService.mergePbsOptions(processingStep2, realm_dkfz, QSUB_PARAMETERS)

        assert expected.contains('-q convey')
        assert expected.contains('-a b')
        assert expected.contains('-l nodes=1:ppn=6 -l mem=3g -l walltime=50:00:00')

        processingOption = createProcessingOption(processingStep2.nonQualifiedJobClass, "{-a: c}", Realm.Cluster.DKFZ)
        expected = pbsOptionMergingService.mergePbsOptions(processingStep2, realm_dkfz, QSUB_PARAMETERS)

        assert expected.contains('-q convey')
        assert expected.contains('-a c')
        assert expected.contains('-l nodes=1:ppn=6 -l mem=3g -l walltime=50:00:00')

        processingOption = createProcessingOption(processingStep2.nonQualifiedJobClass, "{-q: other_convey}", Realm.Cluster.DKFZ)
        expected = pbsOptionMergingService.mergePbsOptions(processingStep2, realm_dkfz, QSUB_PARAMETERS)

        assert expected.contains('-q convey')
        assert expected.contains('-a b')
        assert expected.contains('-l nodes=1:ppn=6 -l mem=3g -l walltime=50:00:00')

        realm = createRealm()
        assertEquals("-A FASTTRACK ", pbsOptionMergingService.mergePbsOptions(processingStep1, realm, '{"-A": "FASTTRACK"}'))

        realm = createRealm("{-a: b}")
        assertEquals("-a b -A FASTTRACK ", pbsOptionMergingService.mergePbsOptions(processingStep1, realm, '{"-A": "FASTTRACK"}'))

        realm = createRealm("{-a: b}")
        processingOption = createProcessingOption(processingStep2.nonQualifiedJobClass, "{}", Realm.Cluster.DKFZ)
        assertEquals("-a b -A FASTTRACK ", pbsOptionMergingService.mergePbsOptions(processingStep2, realm, '{"-A": "FASTTRACK"}'))

        realm = createRealm("{-a: b}")
        processingOption = createProcessingOption(processingStep2.nonQualifiedJobClass, '{"-q": "other_convey"}', Realm.Cluster.DKFZ)
        expected = pbsOptionMergingService.mergePbsOptions(processingStep2, realm, '{"-A": "FASTTRACK"}')

        assert expected.contains('-q other_convey')
        assert expected.contains('-A FASTTRACK')

        realm = createRealm("{-a: b}")
        processingOption = createProcessingOption(processingStep2.nonQualifiedJobClass, '{"-q": "other_convey"}', Realm.Cluster.DKFZ)
        expected = pbsOptionMergingService.mergePbsOptions(processingStep2, realm, QSUB_PARAMETERS, '{"-A": "FASTTRACK"}')

        assert expected.contains('-q convey')
        assert expected.contains('-a b')
        assert expected.contains('-A FASTTRACK')
        assert expected.contains('-l nodes=1:ppn=6 -l mem=3g -l walltime=50:00:00')
    }

    @Test
    void testMergePbsOptions_JobClassProcessingOptionOverridesRealm() {
        ProcessingStep processingStep = DomainFactory.createAndSaveProcessingStep(DomainFactory.createSeqTrack())
        Realm realm = createRealm('{-a: realm, -b: realm}')
        createProcessingOption(processingStep.nonQualifiedJobClass, '{-b: jobClass, -c: jobClass}')

        assertEquals('-a realm -b jobClass -c jobClass ', pbsOptionMergingService.mergePbsOptions(processingStep, realm))
    }

    @Test
    void testMergePbsOptions_JobClassSeqTypeProcessingOptionOverridesJobClassProcessingOption() {
        SeqTrack seqTrack = DomainFactory.createSeqTrack()
        ProcessingStep processingStep = DomainFactory.createAndSaveProcessingStep(seqTrack)
        Realm realm = createRealm()
        createProcessingOption(processingStep.nonQualifiedJobClass, '{-b: jobClass, -c: jobClass}')
        createProcessingOption("${processingStep.nonQualifiedJobClass}_${seqTrack.seqType.processingOptionName}",
                '{-c: jobClassSeqType, -d: jobClassSeqType}')

        assertEquals('-b jobClass -c jobClassSeqType -d jobClassSeqType ',
                pbsOptionMergingService.mergePbsOptions(processingStep, realm))
    }

    @Test
    void testMergePbsOptions_Additional1OverridesJobClassSeqTypeProcessingOption() {
        SeqTrack seqTrack = DomainFactory.createSeqTrack()
        ProcessingStep processingStep = DomainFactory.createAndSaveProcessingStep(seqTrack)
        Realm realm = createRealm()
        createProcessingOption("${processingStep.nonQualifiedJobClass}_${seqTrack.seqType.processingOptionName}",
                '{-c: jobClassSeqType, -d: jobClassSeqType}')
        String additional1 = '{-d: additional1, -e: additional1}'

        assertEquals('-c jobClassSeqType -d additional1 -e additional1 ',
                pbsOptionMergingService.mergePbsOptions(processingStep, realm, additional1))
    }

    @Test
    void testMergePbsOptions_Additional2OverridesAdditional1() {
        ProcessingStep processingStep = DomainFactory.createAndSaveProcessingStep(DomainFactory.createSeqTrack())
        Realm realm = createRealm()
        String additional1 = '{-d: additional1, -e: additional1}'
        String additional2 = '{-e: additional2, -f: additional2}'

        assertEquals('-d additional1 -e additional2 -f additional2 ',
                pbsOptionMergingService.mergePbsOptions(processingStep, realm, additional1, additional2))
    }

    private Realm createRealm(String pbsOptions = "{}", Realm.Cluster cluster = Realm.Cluster.DKFZ) {
        Realm realm = DomainFactory.createRealmDataManagement([
            cluster: cluster,
            pbsOptions: pbsOptions,
        ])
        assertNotNull(realm.save(flush: true))
        return realm
    }

    private ProcessingOption createProcessingOption(String jobKey, String pbsOptions = "{}", Realm.Cluster cluster = Realm.Cluster.DKFZ) {
        String name = PbsOptionMergingService.PBS_PREFIX + jobKey

        //check, if already a option with this name exist and if yes, delete it
        ProcessingOption oldProcessingOption = processingOptionService.findStrict(name, cluster.toString(), null)
        if (oldProcessingOption!= null) {
            oldProcessingOption.delete(flush: true)
        }

        ProcessingOption processingOption = new ProcessingOption(
                name: name,
                type: cluster.toString(),
                value: pbsOptions,
                comment: 'comment'
                )
        assertNotNull(processingOption.save(flush: true))
        return processingOption
    }
}
