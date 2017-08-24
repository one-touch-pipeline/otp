package de.dkfz.tbi.otp.dataprocessing.sophia

import de.dkfz.tbi.otp.dataprocessing.*
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
        MergingCriteria,
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
        SoftwareTool,
        SophiaInstance,
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
        instance.processingState = AnalysisProcessingStates.FINISHED
        assert instance.save(flush: true)

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


    void "getLatestValidSophiaInstanceForSamplePair, test if one instance exists, return instance"() {
        expect:
        instance == SophiaInstance.getLatestValidSophiaInstanceForSamplePair(instance.samplePair)
    }

    void "getLatestValidSophiaInstanceForSamplePair, test if no instance exists, return null"() {
        given:
        SamplePair samplePair = instance.samplePair
        instance.delete()

        expect:
        null == SophiaInstance.getLatestValidSophiaInstanceForSamplePair(samplePair)
    }

    void "getLatestValidSophiaInstanceForSamplePair, test if two instances exists, return latest instance"() {
        given:
        SamplePair samplePair = instance.samplePair

        samplePair.mergingWorkPackage1.bamFileInProjectFolder = instance.sampleType1BamFile
        assert samplePair.mergingWorkPackage1.save(flush: true)
        samplePair.mergingWorkPackage2.bamFileInProjectFolder = instance.sampleType2BamFile
        assert samplePair.mergingWorkPackage2.save(flush: true)
        SophiaInstance instance2 = DomainFactory.createSophiaInstance(samplePair)
        instance2.processingState = AnalysisProcessingStates.FINISHED
        assert instance2.save(flush: true)

        expect:
        instance2 == SophiaInstance.getLatestValidSophiaInstanceForSamplePair(samplePair)
    }

    void "getLatestValidSophiaInstanceForSamplePair, test if two instances exists but the latest is withdrawn, return first instance"() {
        given:
        SamplePair samplePair = instance.samplePair

        samplePair.mergingWorkPackage1.bamFileInProjectFolder = instance.sampleType1BamFile
        assert samplePair.mergingWorkPackage1.save(flush: true)
        samplePair.mergingWorkPackage2.bamFileInProjectFolder = instance.sampleType2BamFile
        assert samplePair.mergingWorkPackage2.save(flush: true)
        SophiaInstance instance2 = DomainFactory.createSophiaInstance(samplePair)
        instance2.withdrawn = true
        assert instance2.save(flush: true)

        expect:
        instance == SophiaInstance.getLatestValidSophiaInstanceForSamplePair(samplePair)
    }

    void "getLatestValidSophiaInstanceForSamplePair, test if two instances exists but the latest not finished yet, return first instance"() {
        given:
        SamplePair samplePair = instance.samplePair

        samplePair.mergingWorkPackage1.bamFileInProjectFolder = instance.sampleType1BamFile
        assert samplePair.mergingWorkPackage1.save(flush: true)
        samplePair.mergingWorkPackage2.bamFileInProjectFolder = instance.sampleType2BamFile
        assert samplePair.mergingWorkPackage2.save(flush: true)
        SophiaInstance instance2 = DomainFactory.createSophiaInstance(samplePair)
        instance2.processingState = AnalysisProcessingStates.IN_PROGRESS
        assert instance2.save(flush: true)

        expect:
        instance == SophiaInstance.getLatestValidSophiaInstanceForSamplePair(samplePair)
    }
}
