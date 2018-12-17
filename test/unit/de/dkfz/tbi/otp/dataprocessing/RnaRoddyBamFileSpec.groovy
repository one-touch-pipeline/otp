package de.dkfz.tbi.otp.dataprocessing

import grails.test.mixin.Mock
import spock.lang.Specification

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.domainFactory.pipelines.roddyRna.RoddyRnaFactory
import de.dkfz.tbi.otp.ngsdata.*

@Mock([
        AbstractMergedBamFile,
        DataFile,
        FileType,
        Individual,
        LibraryPreparationKit,
        MergingCriteria,
        Pipeline,
        Project,
        Realm,
        ReferenceGenome,
        ReferenceGenomeProjectSeqType,
        RnaRoddyBamFile,
        RoddyWorkflowConfig,
        Run,
        RunSegment,
        Sample,
        SampleType,
        SeqCenter,
        SeqPlatform,
        SeqPlatformGroup,
        SeqPlatformModelLabel,
        SeqTrack,
        SeqType,
        SoftwareTool,
        MergingWorkPackage,
])
class RnaRoddyBamFileSpec extends Specification implements RoddyRnaFactory {

    void setup() {
        new TestConfigService()
    }

    void "test method getCorrespondingWorkChimericBamFile"() {
        given:
        RnaRoddyBamFile roddyBamFile = createBamFile()
        String testDir = "${roddyBamFile.individual.getViewByPidPath(roddyBamFile.seqType).absoluteDataManagementPath.path}/${roddyBamFile.sampleType.dirName}/${roddyBamFile.seqType.libraryLayoutDirName}/merged-alignment"
        AbstractMergedBamFileService.metaClass.static.destinationDirectory = { AbstractMergedBamFile bamFile -> return testDir }

        expect:
        "${testDir}/${roddyBamFile.workDirectoryName}/${roddyBamFile.sampleType.dirName}_${roddyBamFile.individual.pid}_${RnaRoddyBamFile.CHIMERIC_BAM_SUFFIX}" == roddyBamFile.correspondingWorkChimericBamFile.path
    }
}
