package workflows

import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.ngsdata.*
import org.junit.Before
import org.junit.Ignore

import static org.junit.Assert.assertNotNull

@Ignore
class ProcessedMergedBamFileSnvWorkflowTests extends AbstractSnvWorkflowTests {

    @Before
    void prepare() {
        project = Project.build(
                dirName: "test",
                realmName: realm.name
        )
        assertNotNull(project.save(flush: true))

        realm.pbsOptions = '{"-l": {nodes: "1", walltime: "29:00"}, "-j": "oe"}'
        assertNotNull(realm.save(flush: true))

        individual = Individual.build(
                project: project,
                type: Individual.Type.REAL,
                pid: "stds",
                mockPid: "stds",
        )

        seqType = SeqType.exomePairedSeqType

        LibraryPreparationKit.build()

        bamFileTumor = DomainFactory.createProcessedMergedBamFile(
                MergingWorkPackage.build(
                        sample: Sample.build(individual: individual),
                        seqType: seqType,
                        libraryPreparationKit: LibraryPreparationKit.build(),
                ),
                PROCESSED_BAM_FILE_PROPERTIES)
        bamFileTumor.workPackage.bamFileInProjectFolder = bamFileTumor
        assert bamFileTumor.workPackage.save(flush: true)
        bamFileControl = DomainFactory.createProcessedMergedBamFile(
                DomainFactory.createMergingWorkPackage(bamFileTumor.mergingWorkPackage),
                PROCESSED_BAM_FILE_PROPERTIES)
        bamFileControl.workPackage.bamFileInProjectFolder = bamFileControl
        assert bamFileControl.workPackage.save(flush: true)

        sampleTypeTumor = bamFileTumor.sampleType
        assertNotNull(sampleTypeTumor.save(flush: true))
        sampleTypeControl = bamFileControl.sampleType
        assertNotNull(sampleTypeControl.save(flush: true))

        createSnvSpecificSetup()
        createThresholds()
        createExternalScripts()
        fileSystemSetup()
    }
}
