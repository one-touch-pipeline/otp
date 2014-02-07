package de.dkfz.tbi.otp.job.jobs.qualityAssessment

import static org.springframework.util.Assert.*
import org.springframework.beans.factory.annotation.Autowired

import groovy.text.SimpleTemplateEngine

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.common.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*

class ExecuteBamFileQaAnalysisJob extends AbstractJobImpl {

    @Autowired
    ProcessedBamFileQaFileService processedBamFileQaFileService

    @Autowired
    QualityAssessmentPassService qualityAssessmentPassService

    @Autowired
    ProcessedBamFileService processedBamFileService

    @Autowired
    ExecutionHelperService executionHelperService

    @Autowired
    ProcessingOptionService processingOptionService

    @Autowired
    ReferenceGenomeService referenceGenomeService

    @Autowired
    BedFileService bedFileService

    @Override
    public void execute() throws Exception {
        long passId = getProcessParameterValue() as long
        QualityAssessmentPass pass = QualityAssessmentPass.get(passId)
        Realm realm = qualityAssessmentPassService.realmForDataProcessing(pass)
        ProcessedBamFile processedBamFile = pass.processedBamFile
        String processedBamFilePath = processedBamFileService.getFilePath(processedBamFile)
        String processedBaiFilePath = processedBamFileService.baiFilePath(processedBamFile)
        String qualityAssessmentFilePath = processedBamFileQaFileService.qualityAssessmentDataFilePath(pass)
        String coverageDataFilePath = processedBamFileQaFileService.coverageDataFilePath(pass)
        String insertSizeDataFilePath = processedBamFileQaFileService.insertSizeDataFilePath(pass)
        String allChromosomeName = Chromosomes.overallChromosomesLabel()
        Project project = processedBamFileService.project(processedBamFile)
        SeqType seqType = processedBamFileService.seqType(processedBamFile)
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
        Map binding = [
            processedBamFilePath: processedBamFilePath,
            processedBaiFilePath: processedBaiFilePath,
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
            ReferenceGenome referenceGenome = referenceGenomeService.referenceGenome(project, seqType)
            ExomeEnrichmentKit exomeEnrichmentKit = processedBamFileService.exomeEnrichmentKit(processedBamFile)
            BedFile bedFile = BedFile.findByReferenceGenomeAndExomeEnrichmentKit(referenceGenome, exomeEnrichmentKit)
            String bedFilePath = bedFileService.filePath(realm, bedFile)
            String refGenMetaInfoFilePath = referenceGenomeService.referenceGenomeMetaInformationPath(realm, referenceGenome)
            binding.bedFilePath = bedFilePath
            binding.refGenMetaInfoFilePath = refGenMetaInfoFilePath
        }

        String cmdTemplate = processingOptionService.findOptionAssure("qualityAssessment", seqTypeNaturalId, project)
        SimpleTemplateEngine engine = new SimpleTemplateEngine()
        String cmd = engine.createTemplate(cmdTemplate).make(binding).toString().trim()
        cmd += "; chmod 440 ${qualityAssessmentFilePath} ${coverageDataFilePath} ${insertSizeDataFilePath}"
        String pbsID = executionHelperService.sendScript(realm, cmd)
        addOutputParameter("__pbsIds", pbsID)
        addOutputParameter("__pbsRealm", realm.id.toString())
    }
}
