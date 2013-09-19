package de.dkfz.tbi.otp.job.jobs.qualityAssessmentMerged

import grails.converters.JSON
import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*

class CreateMergedChromosomeMappingFileJob extends AbstractEndStateAwareJobImpl {

    @Autowired
    ReferenceGenomeService referenceGenomeService

    @Autowired
    ProcessedMergedBamFileService processedMergedBamFileService

    @Autowired
    QualityAssessmentMergedPassService qualityAssessmentMergedPassService

    @Autowired
    ProcessedMergedBamFileQaFileService processedMergedBamFileQaFileService

    @Autowired
    ChromosomeIdentifierMappingService chromosomeIdentifierMappingService

    @Autowired
    ChromosomeIdentifierFilteringService chromosomeIdentifierFilteringService

    @Autowired
    ChromosomeIdentifierSortingService chromosomeIdentifierSortingService

    @Autowired
    ExecutionService executionService

    @Override
    public void execute() throws Exception {
        long passId = getProcessParameterValue() as long
        QualityAssessmentMergedPass pass = QualityAssessmentMergedPass.get(passId)
        Project project = processedMergedBamFileService.project(pass.processedMergedBamFile)
        SeqType seqType = processedMergedBamFileService.seqType(pass.processedMergedBamFile)
        ReferenceGenome referenceGenome = referenceGenomeService.referenceGenome(project, seqType)
        Map<String, String> chromosomeIdentifierMap = chromosomeIdentifierMappingService.mappingAll(referenceGenome)
        List<String> filterChromosomes = chromosomeIdentifierFilteringService.filteringCoverage(referenceGenome)
        List<String> sortedChromosomeIdentifiers = chromosomeIdentifierSortingService.sortIdentifiers(filterChromosomes)
        Map data = [
            chromosomeIdentifierMap: chromosomeIdentifierMap,
            filterChromosomes: filterChromosomes,
            sortedChromosomeIdentifiers: sortedChromosomeIdentifiers
        ]
        String chromosomeMappingFileContents = (data as JSON).toString(true)
        String chromosomeMappingFilePath = processedMergedBamFileQaFileService.chromosomeMappingFilePath(pass)
        Realm realm = qualityAssessmentMergedPassService.realmForDataProcessing(pass)
        execute(chromosomeMappingFileContents, chromosomeMappingFilePath, realm)
    }

    private void execute(String fileContents, String filepath, Realm realm) {
        String cmd = "echo '${fileContents}' > ${filepath}"
        cmd += "; chmod 440 ${filepath}"
        log.debug cmd
        String standartOutput = executionService.executeCommand(realm, cmd)
        log.debug "creating file finished with standart output " + standartOutput
        boolean fileCreated = validate(filepath)
        fileCreated ? succeed() : fail()
    }

    private boolean validate(String filepath) {
        File file = new File(filepath)
        return file.canRead() && file.size() != 0
    }
}
