package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import grails.test.mixin.*
import org.junit.*
import org.junit.rules.*
import spock.lang.*


@Mock([
        DataFile,
        ExternalScript,
        FileType,
        Individual,
        LibraryPreparationKit,
        MergingSet,
        MergingWorkPackage,
        Pipeline,
        Project,
        ProjectCategory,
        Realm,
        ReferenceGenome,
        RoddyBamFile,
        RoddyWorkflowConfig,
        Run,
        RunSegment,
        Sample,
        SamplePair,
        SampleType,
        SampleTypePerProject,
        SeqCenter,
        SeqPlatform,
        SeqPlatformGroup,
        SeqTrack,
        SeqType,
        SoftwareTool
])
class SophiaInstanceSpec extends Specification {
    @Rule
    TemporaryFolder temporaryFolder

    SophiaInstance instance
    File instancePath

    /**
     * Creates Temporary File for Data Management path
     * so later on temp files can be generated and paths tested
     */
    void setup() {
        File temporaryFile = temporaryFolder.newFolder()
        Realm realm = DomainFactory.createRealmDataManagement(rootPath: temporaryFile)

        this.instance = DomainFactory.createSophiaInstanceWithRoddyBamFiles()
        instance.project.realmName = realm.name
        instance.project.save(flush: true)

        instancePath = new File(
                temporaryFile, "${instance.project.dirName}/sequencing/${instance.seqType.dirName}/view-by-pid/" +
                "${instance.individual.pid}/sv_results/${instance.seqType.libraryLayoutDirName}/" +
                "${instance.sampleType1BamFile.sampleType.dirName}_${instance.sampleType2BamFile.sampleType.dirName}/" +
                "${instance.instanceName}"
        )
    }

    void "getSophiaInstancePath, tests if path is in a valid form"() {

        when:
        OtpPath sophiaInstancePath = instance.getInstancePath()

        then:
        instance.project == sophiaInstancePath.project
        instancePath == sophiaInstancePath.absoluteDataManagementPath
    }


    void "getFinalAceseqInputFile, tests if path is in a valid form"() {
        given:
        File expectedPath = new File(instancePath, "svs_${instance.individual.pid}_${instance.SOPHIA_OUTPUT_FILE_SUFFIX}")

        expect:
        instance.getFinalAceseqInputFile() == expectedPath
    }
}
