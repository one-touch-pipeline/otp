package de.dkfz.tbi.otp.job.jobs.aceseq

import de.dkfz.tbi.*
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
        AceseqInstance,
        AceseqQc,
        DataFile,
        FileType,
        Individual,
        LibraryPreparationKit,
        MergingCriteria,
        MergingWorkPackage,
        Pipeline,
        Project,
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
        SeqPlatformModelLabel,
        SeqTrack,
        SeqType,
        SoftwareTool,
])
class ParseAceseqQcJobSpec extends Specification {

    @Rule
    TemporaryFolder temporaryFolder

    void "test execute"() {
        given:
        File temporaryFile = temporaryFolder.newFolder()
        File aceseqOutputFile = new File(temporaryFile, "aceseqOutputFile.txt")
        CreateFileHelper.createFile(aceseqOutputFile)
        Realm realm = DomainFactory.createRealmDataManagement(rootPath: temporaryFile)

        AceseqInstance instance = DomainFactory.createAceseqInstanceWithRoddyBamFiles()
        instance.project.realmName = realm.name
        instance.project.save(flush: true)

        instance.metaClass.getAllFiles = {
            return [aceseqOutputFile]

        }

        DomainFactory.createAceseqQaFileOnFileSystem(instance.getQcJsonFile())

        ParseAceseqQcJob job = [
                getProcessParameterObject: { -> instance },
        ] as ParseAceseqQcJob

        when:
        job.execute()

        then:
        AceseqQc.findAllByAceseqInstance(instance).size() == 2
        TestCase.containSame(AceseqQc.findAllByAceseqInstance(instance)*.number, [1, 2])
        def qc1 = AceseqQc.findByAceseqInstanceAndNumber(instance, 1)
        qc1.tcc == 0.5d
        qc1.ploidyFactor == "2.27"
        qc1.ploidy == 2
        qc1.goodnessOfFit == 0.904231625835189d
        qc1.gender == "male"
        qc1.solutionPossible == 3
        def qc2 = AceseqQc.findByAceseqInstanceAndNumber(instance, 2)
        qc2.tcc == 0.7d
        qc2.ploidyFactor == "1.27"
        qc2.ploidy == 5
        qc2.goodnessOfFit == 0.12345d
        qc2.gender == "female"
        qc2.solutionPossible == 4

        instance.processingState == AnalysisProcessingStates.FINISHED
    }
}
