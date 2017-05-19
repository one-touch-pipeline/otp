package de.dkfz.tbi.otp.job.jobs.aceseq

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.sophia.*
import de.dkfz.tbi.otp.job.jobs.*
import de.dkfz.tbi.otp.job.jobs.roddyAlignment.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import org.springframework.beans.factory.annotation.*

class ExecuteRoddyAceseqJob extends AbstractExecutePanCanJob<AceseqInstance> implements AutoRestartableJob {

    @Autowired
    AceseqService aceseqService

    @Autowired
    LinkFileUtils linkFileUtils

    @Autowired
    ReferenceGenomeService referenceGenomeService

    @Override
    protected List<String> prepareAndReturnWorkflowSpecificCValues(AceseqInstance aceseqInstance) {
        assert aceseqInstance

        aceseqService.validateInputBamFiles(aceseqInstance)

        RoddyBamFile bamFileDisease = aceseqInstance.sampleType1BamFile as RoddyBamFile
        RoddyBamFile bamFileControl = aceseqInstance.sampleType2BamFile as RoddyBamFile
        File bamFileDiseasePath = bamFileDisease.pathForFurtherProcessing
        File bamFileControlPath = bamFileControl.pathForFurtherProcessing

        final Realm realm = configService.getRealmDataProcessing(aceseqInstance.project)
        ReferenceGenome referenceGenome = bamFileDisease.referenceGenome
        referenceGenomeService.checkReferenceGenomeFilesAvailability(bamFileDisease.mergingWorkPackage)

        File referenceGenomeFastaFile = referenceGenomeService.fastaFilePath(referenceGenome)
        assert referenceGenomeFastaFile: "Path to the reference genome file is null"
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(referenceGenomeFastaFile)

        File chromosomeLengthFile = referenceGenomeService.chromosomeLengthFile(bamFileDisease.mergingWorkPackage)
        assert chromosomeLengthFile: "Path to the chromosome length file is null"
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(chromosomeLengthFile)

        File gcContentFile = referenceGenomeService.gcContentFile(bamFileDisease.mergingWorkPackage)
        assert gcContentFile: "Path to the gc content file is null"
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(gcContentFile)

        SophiaInstance sophiaInstance = SophiaInstance.getLatestValidSophiaInstanceForSamplePair(aceseqInstance.samplePair)
        File aceseqInputFile = sophiaInstance.finalAceseqInputFile
        assert aceseqInputFile : "Path to the ACEseq input file is null"
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(aceseqInputFile)

        linkFileUtils.createAndValidateLinks([(aceseqInputFile): new File(aceseqInstance.workDirectory, aceseqInputFile.name)], realm)

        List<String> cValues = []
        cValues.add("bamfile_list:${bamFileControlPath};${bamFileDiseasePath}")
        cValues.add("sample_list:${bamFileControl.sampleType.dirName};${bamFileDisease.sampleType.dirName}")
        cValues.add("possibleTumorSampleNamePrefixes:${bamFileDisease.sampleType.dirName}")
        cValues.add("possibleControlSampleNamePrefixes:${bamFileControl.sampleType.dirName}")
        cValues.add("REFERENCE_GENOME:${referenceGenomeFastaFile}")
        cValues.add("CHROMOSOME_LENGTH_FILE:${chromosomeLengthFile}")
        cValues.add("CHR_SUFFIX:${referenceGenome.chromosomeSuffix}")
        cValues.add("CHR_PREFIX:${referenceGenome.chromosomePrefix}")
        cValues.add("aceseqOutputDirectory:${aceseqInstance.workDirectory}")
        cValues.add("svOutputDirectory:${aceseqInstance.workDirectory}")
        cValues.add("MAPPABILITY_FILE:${referenceGenome.mappabilityFile}")
        cValues.add("REPLICATION_TIME_FILE:${referenceGenome.replicationTimeFile}")
        cValues.add("GC_CONTENT_FILE:${gcContentFile}")
        cValues.add("GENETIC_MAP_FILE:${referenceGenome.geneticMapFile}")
        cValues.add("KNOWN_HAPLOTYPES_FILE:${referenceGenome.knownHaplotypesFile}")
        cValues.add("KNOWN_HAPLOTYPES_LEGEND_FILE:${referenceGenome.knownHaplotypesLegendFile}")
        cValues.add("GENETIC_MAP_FILE_X:${referenceGenome.geneticMapFileX}")
        cValues.add("KNOWN_HAPLOTYPES_FILE_X:${referenceGenome.knownHaplotypesFileX}")
        cValues.add("KNOWN_HAPLOTYPES_LEGEND_FILE_X:${referenceGenome.knownHaplotypesLegendFileX}")

        return cValues
    }

    @Override
    protected String prepareAndReturnWorkflowSpecificParameter(AceseqInstance aceseqInstance) {
        return ""
    }


    @Override
    protected void validate(AceseqInstance aceseqInstance) throws Throwable {
        assert aceseqInstance : "The input aceseqInstance must not be null"

        executeRoddyCommandService.correctPermissionsAndGroups(aceseqInstance, configService.getRealmDataManagement(aceseqInstance.project))

        List<File> directories = [
                aceseqInstance.workExecutionStoreDirectory
        ]
        directories.addAll(aceseqInstance.workExecutionDirectories)


        directories.each {
            LsdfFilesService.ensureDirIsReadableAndNotEmpty(it)
        }

        aceseqService.validateInputBamFiles(aceseqInstance)

        aceseqInstance.updateProcessingState(AnalysisProcessingStates.FINISHED)
    }
}
