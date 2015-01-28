package de.dkfz.tbi.otp.job.jobs.qualityAssessment

import grails.converters.JSON
import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*

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
        ReferenceGenome referenceGenome = pass.referenceGenome
        Map<String, String> chromosomeIdentifierMap = chromosomeIdentifierMappingService.mappingAll(referenceGenome)
        List<String> filterChromosomes = chromosomeIdentifierFilteringService.filteringCoverage(referenceGenome)
        List<String> sortedChromosomeIdentifiers = chromosomeIdentifierSortingService.sortIdentifiers(filterChromosomes)
        Map data = [
            chromosomeIdentifierMap: chromosomeIdentifierMap,
            filterChromosomes: filterChromosomes,
            sortedChromosomeIdentifiers: sortedChromosomeIdentifiers
        ]
        String fileContents = (data as JSON).toString(true)
        String filePath = processedBamFileQaFileService.chromosomeMappingFilePath(pass)
        Realm realm = qualityAssessmentPassService.realmForDataProcessing(pass)
        String cmd = "echo '${fileContents}' > ${filePath}"
        cmd += "; chmod 440 ${filePath}"
        String standardOutput = executionService.executeCommand(realm, cmd)
        log.debug "creating file finished with standard output " + standardOutput
        boolean fileCreated = validate(filePath)
        fileCreated ? succeed() : fail()
    }

    private boolean validate(String filepath) {
        File file = new File(filepath)
        return file.canRead() && file.size() != 0
    }
}
