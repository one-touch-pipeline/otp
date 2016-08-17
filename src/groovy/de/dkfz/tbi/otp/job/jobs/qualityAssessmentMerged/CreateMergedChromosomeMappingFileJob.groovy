package de.dkfz.tbi.otp.job.jobs.qualityAssessmentMerged

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.ast.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import grails.converters.*
import org.springframework.beans.factory.annotation.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*

@Component
@Scope("prototype")
@UseJobLog
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
        String filePath = processedMergedBamFileQaFileService.chromosomeMappingFilePath(pass)
        Realm realm = qualityAssessmentMergedPassService.realmForDataProcessing(pass)
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
