package de.dkfz.tbi.otp.job.jobs.qualityAssessmentMerged

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.common.*
import de.dkfz.tbi.otp.job.jobs.utils.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import groovy.text.*
import org.springframework.beans.factory.annotation.*

import static org.springframework.util.Assert.*

class ExecuteMergedBamFileQaAnalysisJob extends AbstractJobImpl {

    @Autowired
    ProcessedMergedBamFileQaFileService processedMergedBamFileQaFileService

    @Autowired
    QualityAssessmentMergedPassService qualityAssessmentMergedPassService

    @Autowired
    ProcessedMergedBamFileService processedMergedBamFileService

    @Autowired
    ClusterJobSchedulerService clusterJobSchedulerService

    @Autowired
    ReferenceGenomeService referenceGenomeService

    @Autowired
    BedFileService bedFileService

    @Override
    public void execute() throws Exception {
        long passId = getProcessParameterValue() as long
        QualityAssessmentMergedPass pass = QualityAssessmentMergedPass.get(passId)
        Realm realm = qualityAssessmentMergedPassService.realmForDataProcessing(pass)
        ProcessedMergedBamFile processedMergedBamFile = pass.abstractMergedBamFile as ProcessedMergedBamFile
        String processedMergedBamFilePath = processedMergedBamFileService.filePath(processedMergedBamFile)
        String processedMergedBaiFilePath = processedMergedBamFileService.filePathForBai(processedMergedBamFile)
        String qualityAssessmentFilePath = processedMergedBamFileQaFileService.qualityAssessmentDataFilePath(pass)
        String coverageDataFilePath = processedMergedBamFileQaFileService.coverageDataFilePath(pass)
        String insertSizeDataFilePath = processedMergedBamFileQaFileService.insertSizeDataFilePath(pass)
        String allChromosomeName = Chromosomes.overallChromosomesLabel()
        Project project = processedMergedBamFileService.project(processedMergedBamFile)
        SeqType seqType = processedMergedBamFileService.seqType(processedMergedBamFile)
        String seqTypeNaturalId = seqType.getNaturalId()

        List allowedSeqTypes = [
            [name:SeqTypeNames.EXOME.seqTypeName, lib:'PAIRED'],
            [name:SeqTypeNames.WHOLE_GENOME.seqTypeName, lib:'PAIRED']
        ]
        boolean isSupported = allowedSeqTypes.any { map ->
            [name: seqType.name, lib: seqType.libraryLayout ] == map
        }
        isTrue(isSupported, 'This sequencing type and library layout combination can not be processed')

        // The map contains all parameter, which are needed to run the qa.jar.
        // the command parameters (the map keys) have the same names in
        // both merged and not-merted cases to enable possibilty to
        // have one option configuring the command in both cases
        Map binding = [
            processedBamFilePath: processedMergedBamFilePath,
            processedBaiFilePath: processedMergedBaiFilePath,
            qualityAssessmentFilePath: qualityAssessmentFilePath,
            coverageDataFilePath: coverageDataFilePath,
            insertSizeDataFilePath: insertSizeDataFilePath,
            allChromosomeName: allChromosomeName,
            bedFilePath: '',
            refGenMetaInfoFilePath: '',
        ]

        // The qa.jar is started with different parameters, depending on the sequencing type -> check which seq type is the current one
        // In case the seqType is exome, there are two more parameter needed to run the qa.jar:
        // BedFile and File containing the names of the reference genome entries
        boolean isExonePaired = (seqType.name == SeqTypeNames.EXOME.seqTypeName && seqType.libraryLayout == 'PAIRED')
        if (isExonePaired) {
            ReferenceGenome referenceGenome = pass.referenceGenome
            LibraryPreparationKit libraryPreparationKit = processedMergedBamFileService.libraryPreparationKit(processedMergedBamFile)
            BedFile bedFile = BedFile.findByReferenceGenomeAndLibraryPreparationKit(referenceGenome, libraryPreparationKit)
            if (!bedFile) {
                throw new ProcessingException("Could not find a bed file for ${referenceGenome} and ${libraryPreparationKit}")
            }
            String bedFilePath = bedFileService.filePath(bedFile)
            String refGenMetaInfoFilePath = referenceGenomeService.referenceGenomeMetaInformationPath(referenceGenome).absolutePath
            binding.bedFilePath = bedFilePath
            binding.refGenMetaInfoFilePath = refGenMetaInfoFilePath
        }

        String cmdTemplate = ProcessingOptionService.findOptionAssure(ProcessingOption.OptionName.PIPELINE_OTP_ALIGNMENT_QUALITY_MERGED_ASSESSMENT, seqTypeNaturalId, project)
        SimpleTemplateEngine engine = new SimpleTemplateEngine()
        String cmd = engine.createTemplate(cmdTemplate).make(binding).toString().trim()
        cmd += "; chmod 440 ${qualityAssessmentFilePath} ${coverageDataFilePath} ${insertSizeDataFilePath}"
        String jobId = clusterJobSchedulerService.executeJob(realm, cmd)
        addOutputParameter(JobParameterKeys.JOB_ID_LIST, jobId)
        addOutputParameter(JobParameterKeys.REALM, realm.id.toString())
    }
}
