package workflows

import de.dkfz.tbi.otp.ngsdata.*
import org.junit.Before
import org.junit.Ignore

import static org.junit.Assert.assertNotNull

@Ignore
class RoddyBamFileSnvWorkflowTests extends AbstractSnvWorkflowTests {

    @Before
    void prepare() {
        project = Project.build(
                dirName: "test",
                realmName: realm.name
        )
        assertNotNull(project.save(flush: true))

        realm.pbsOptions = '{"-l": {nodes: "1", walltime: "29:00"}, "-j": "oe"}'
        assertNotNull(realm.save(flush: true))


        bamFileTumor = DomainFactory.createRoddyBamFile(PROCESSED_BAM_FILE_PROPERTIES)
        bamFileTumor.workPackage.bamFileInProjectFolder = bamFileTumor
        assert bamFileTumor.workPackage.save(flush: true)

        individual = bamFileTumor.individual
        individual.project = project
        individual.pid = "stds"
        individual.mockPid = "stds"
        individual.save(flush: true)

        seqType = bamFileTumor.seqType

        sampleTypeTumor = bamFileTumor.sampleType
        assertNotNull(bamFileTumor.save(flush: true))

        bamFileControl = DomainFactory.createRoddyBamFile(PROCESSED_BAM_FILE_PROPERTIES + [
                workPackage: DomainFactory.createMergingWorkPackage(bamFileTumor.mergingWorkPackage),
        ])

        bamFileControl.workPackage.bamFileInProjectFolder = bamFileControl
        assert bamFileControl.workPackage.save(flush: true)

        sampleTypeControl = bamFileControl.sampleType
        assertNotNull(bamFileControl.save(flush: true))


        createSnvSpecificSetup()
        createThresholds()
        createExternalScripts()
        fileSystemSetup()

    }
}
