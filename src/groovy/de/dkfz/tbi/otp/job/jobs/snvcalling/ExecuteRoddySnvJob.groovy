package de.dkfz.tbi.otp.job.jobs.snvcalling

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.job.jobs.*
import de.dkfz.tbi.otp.job.jobs.roddyAlignment.*
import de.dkfz.tbi.otp.ngsdata.*
import org.springframework.beans.factory.annotation.*

import java.nio.file.*

class ExecuteRoddySnvJob extends AbstractExecutePanCanJob<RoddySnvCallingInstance> implements AutoRestartableJob {

    @Autowired
    ReferenceGenomeService referenceGenomeService

    @Autowired
    SnvCallingService snvCallingService

    @Override
    protected List<String> prepareAndReturnWorkflowSpecificCValues(RoddySnvCallingInstance roddySnvCallingInstance) {
        assert roddySnvCallingInstance

        snvCallingService.validateInputBamFiles(roddySnvCallingInstance)

        AbstractMergedBamFile bamFileDisease = roddySnvCallingInstance.sampleType1BamFile
        AbstractMergedBamFile bamFileControl = roddySnvCallingInstance.sampleType2BamFile
        File bamFileDiseasePath = bamFileDisease.pathForFurtherProcessing
        File bamFileControlPath = bamFileControl.pathForFurtherProcessing

        ReferenceGenome referenceGenome = roddySnvCallingInstance.referenceGenome
        File referenceGenomeFastaFile = referenceGenomeService.fastaFilePath(roddySnvCallingInstance.project, referenceGenome) as File
        assert referenceGenomeFastaFile: "Path to the reference genome file is null"
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(referenceGenomeFastaFile)

        Path individualPath = Paths.get(roddySnvCallingInstance.individual.getViewByPidPath(roddySnvCallingInstance.seqType).absoluteDataManagementPath.path)
        Path resultDirectory = Paths.get(roddySnvCallingInstance.snvInstancePath.absoluteDataManagementPath.path)

        List<String> cValues = []
        cValues.add("bamfile_list:${bamFileControlPath};${bamFileDiseasePath}")
        cValues.add("sample_list:${bamFileControl.sampleType.dirName};${bamFileDisease.sampleType.dirName}")
        cValues.add("possibleTumorSampleNamePrefixes:${bamFileDisease.sampleType.dirName}")
        cValues.add("possibleControlSampleNamePrefixes:${bamFileControl.sampleType.dirName}")
        cValues.add("REFERENCE_GENOME:${referenceGenomeFastaFile.path}")
        cValues.add("CHROMOSOME_LENGTH_FILE:${referenceGenomeService.chromosomeLengthFile(bamFileControl.mergingWorkPackage).path}")
        cValues.add("CHR_SUFFIX:${referenceGenome.chromosomeSuffix}")
        cValues.add("CHR_PREFIX:${referenceGenome.chromosomePrefix}")
        cValues.add("${getChromosomeIndexParameterWithoutMitochondrium(roddySnvCallingInstance.referenceGenome)}")
        cValues.add("analysisMethodNameOnOutput:${individualPath.relativize(resultDirectory).toString()}")

        return cValues
    }

    @Override
    protected String prepareAndReturnWorkflowSpecificParameter(RoddySnvCallingInstance roddySnvCallingInstance) {
        return ""
    }


    @Override
    protected void validate(RoddySnvCallingInstance roddySnvCallingInstance) throws Throwable {
        assert roddySnvCallingInstance : "The input roddyResult must not be null"

        executeRoddyCommandService.correctPermissionsAndGroups(roddySnvCallingInstance, configService.getRealmDataManagement(roddySnvCallingInstance.project))

        List<File> directories = [
                roddySnvCallingInstance.workExecutionStoreDirectory
        ]
        directories.addAll(roddySnvCallingInstance.workExecutionDirectories)

        List<File> files = [
                roddySnvCallingInstance.getAllSNVdiagnosticsPlots().absoluteDataManagementPath,
        ]

        [SnvCallingStep.CALLING, SnvCallingStep.SNV_DEEPANNOTATION].each {
            files.add(new OtpPath(roddySnvCallingInstance.snvInstancePath, it.getResultFileName(roddySnvCallingInstance.individual)).absoluteDataManagementPath)
        }

        directories.each {
            LsdfFilesService.ensureDirIsReadableAndNotEmpty(it)
        }

        files.each {
            LsdfFilesService.ensureFileIsReadableAndNotEmpty(it)
        }

        snvCallingService.validateInputBamFiles(roddySnvCallingInstance)

        roddySnvCallingInstance.updateProcessingState(AnalysisProcessingStates.FINISHED)

    }
}
