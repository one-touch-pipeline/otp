package de.dkfz.tbi.otp.job.jobs.indelCalling

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.jobs.*
import de.dkfz.tbi.otp.job.jobs.roddyAlignment.*
import de.dkfz.tbi.otp.ngsdata.*
import org.springframework.beans.factory.annotation.*

import java.nio.file.*

class ExecuteRoddyIndelJob extends AbstractExecutePanCanJob<IndelCallingInstance> implements AutoRestartableJob {

    @Autowired
    ReferenceGenomeService referenceGenomeService

    @Autowired
    IndelCallingService indelCallingService

    @Override
    protected List<String> prepareAndReturnWorkflowSpecificCValues(IndelCallingInstance indelCallingInstance) {
        assert indelCallingInstance

        indelCallingService.validateInputBamFiles(indelCallingInstance)

        AbstractMergedBamFile bamFileDisease = indelCallingInstance.sampleType1BamFile
        AbstractMergedBamFile bamFileControl = indelCallingInstance.sampleType2BamFile
        File bamFileDiseasePath = bamFileDisease.pathForFurtherProcessing
        File bamFileControlPath = bamFileControl.pathForFurtherProcessing

        ReferenceGenome referenceGenome = indelCallingInstance.referenceGenome
        File referenceGenomeFastaFile = referenceGenomeService.fastaFilePath(referenceGenome)
        assert referenceGenomeFastaFile: "Path to the reference genome file is null"
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(referenceGenomeFastaFile)

        Path individualPath = Paths.get(indelCallingInstance.individual.getViewByPidPath(indelCallingInstance.seqType).absoluteDataManagementPath.path)
        Path resultDirectory = Paths.get(indelCallingInstance.instancePath.absoluteDataManagementPath.path)

        List<String> cValues = []
        cValues.add("bamfile_list:${bamFileControlPath};${bamFileDiseasePath}")
        cValues.add("sample_list:${bamFileControl.sampleType.dirName};${bamFileDisease.sampleType.dirName}")
        cValues.add("possibleTumorSampleNamePrefixes:${bamFileDisease.sampleType.dirName}")
        cValues.add("possibleControlSampleNamePrefixes:${bamFileControl.sampleType.dirName}")
        cValues.add("REFERENCE_GENOME:${referenceGenomeFastaFile.path}")
        cValues.add("CHR_SUFFIX:${referenceGenome.chromosomeSuffix}")
        cValues.add("CHR_PREFIX:${referenceGenome.chromosomePrefix}")
        cValues.add("analysisMethodNameOnOutput:${individualPath.relativize(resultDirectory).toString()}")

        return cValues
    }

    @Override
    protected String prepareAndReturnWorkflowSpecificParameter(IndelCallingInstance indelCallingInstance) {
        return ""
    }


    @Override
    protected void validate(IndelCallingInstance indelCallingInstance) throws Throwable {
        assert indelCallingInstance : "The input indelCallingInstance must not be null"

        executeRoddyCommandService.correctPermissionsAndGroups(indelCallingInstance, configService.getRealmDataManagement(indelCallingInstance.project))

        List<File> directories = [
                indelCallingInstance.workExecutionStoreDirectory
        ]
        directories.addAll(indelCallingInstance.workExecutionDirectories)

        List<File> files = [
                indelCallingInstance.getCombinedPlotPath(),
        ]

        files.addAll(indelCallingInstance.getResultFilePathsToValidate())

        directories.each {
            LsdfFilesService.ensureDirIsReadableAndNotEmpty(it)
        }

        files.each {
            LsdfFilesService.ensureFileIsReadableAndNotEmpty(it)
        }

        indelCallingService.validateInputBamFiles(indelCallingInstance)

        indelCallingInstance.updateProcessingState(AnalysisProcessingStates.FINISHED)
    }
}
