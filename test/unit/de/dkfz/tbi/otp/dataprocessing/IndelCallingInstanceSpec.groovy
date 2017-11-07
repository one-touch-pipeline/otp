package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import grails.test.mixin.*
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
        SoftwareTool
])
class IndelCallingInstanceSpec extends Specification {


    void testGetIndelInstancePath() {
        given:
        IndelCallingInstance instance = DomainFactory.createIndelCallingInstanceWithRoddyBamFiles()

        when:
        OtpPath indelInstancePath = instance.getInstancePath()

        then:
        instance.project == indelInstancePath.project
        File expectedRelativePath = new File("${instance.project.dirName}/sequencing/${instance.seqType.dirName}/view-by-pid/${instance.individual.pid}/indel_results/" +
                "${instance.seqType.libraryLayoutDirName}/${instance.sampleType1BamFile.sampleType.dirName}_${instance.sampleType2BamFile.sampleType.dirName}/${instance.instanceName}")
        expectedRelativePath.path == indelInstancePath.relativePath.path
    }

}
