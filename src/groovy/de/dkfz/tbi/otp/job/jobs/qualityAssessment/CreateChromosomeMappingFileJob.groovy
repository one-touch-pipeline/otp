package de.dkfz.tbi.otp.job.jobs.qualityAssessment

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.Autowired
import grails.converters.JSON

class CreateChromosomeMappingFileJob extends AbstractEndStateAwareJobImpl {

    @Autowired
    ReferenceGenomeService referenceGenomeService

    @Autowired
    ProcessedBamFileService processedBamFileService

    @Autowired
    QualityAssessmentPassService qualityAssessmentPassService

    @Autowired
    ProcessedBamFileQaFileService processedBamFileQaFileService

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
        QualityAssessmentPass pass = QualityAssessmentPass.get(passId)
        Project project = processedBamFileService.project(pass.processedBamFile)
        SeqType seqType = processedBamFileService.seqType(pass.processedBamFile)
        ReferenceGenome referenceGenome = referenceGenomeService.referenceGenome(project, seqType)
        Map<String, String> chromosomeIdentifierMap = chromosomeIdentifierMappingService.mappingAll(referenceGenome)
        List<String> filterChromosomes = chromosomeIdentifierFilteringService.filteringCoverage()
        List<String> sortedChromosomeIdentifiers = chromosomeIdentifierSortingService.sortIdentifiers(chromosomeIdentifierMap.values())
        Map data = [
            chromosomeIdentifierMap: chromosomeIdentifierMap,
            filterChromosomes: filterChromosomes,
            sortedChromosomeIdentifiers: sortedChromosomeIdentifiers
        ]
        String chromosomeMappingFileContents = (data as JSON).toString(true)
        String chromosomeMappingFilePath = processedBamFileQaFileService.chromosomeMappingFilePath(pass)
        Realm realm = qualityAssessmentPassService.realmForDataProcessing(pass)
        execute(chromosomeMappingFileContents, chromosomeMappingFilePath, realm)
    }

    private void execute(String fileContents, String filepath, Realm realm) {
        String cmd = "echo '${fileContents}' > ${filepath}"
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
