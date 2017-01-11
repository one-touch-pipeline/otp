package de.dkfz.tbi.otp.job.jobs.snvcalling

import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair.ProcessingStatus
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*


@ResumableJob
class SamplePairDiscoveryJob extends AbstractEndStateAwareJobImpl {

    @Override
    void execute() throws Exception {
        logUncategorizedSampleTypes()
        createMissingDiseaseControlSamplePairs()
        succeed()
    }

    /**
     * @return Whether there are sample types which need categorization
     */
    void logUncategorizedSampleTypes() {
        final String uncategorizedSampleTypes = findUncategorizedSampleTypes()
        if (uncategorizedSampleTypes != null) {
            log.warn "The following sample types need to be categorized:\n${uncategorizedSampleTypes}"
        } else {
            log.info 'Did not find any sample type which needs categorization. :-)'
        }
    }

    static String findUncategorizedSampleTypes() {
        final Collection missingCombinations = SampleTypePerProject.findMissingCombinations()
        if (missingCombinations.empty) {
            return null
        }
        final StringBuilder buffer = new StringBuilder()
        missingCombinations.groupBy { it[0] }.sort( { p1, p2 -> p1.name <=> p2.name } as Comparator<Project>).each {
            Project project, Collection combinations ->
                buffer.append("${project.name}\n")
                combinations.collect { it[1].name }.sort().each {
                    buffer.append("  ${it}\n")
                }
        }
        return buffer.toString()
    }

    void createMissingDiseaseControlSamplePairs() {
        final Collection<SamplePair> samplePairs =
                SamplePair.findMissingDiseaseControlSamplePairs()
        log?.info "Creating ${samplePairs.size()} new ${SamplePair.simpleName} instance(s)."
        SamplePair.setSnvProcessingStatus(samplePairs, ProcessingStatus.NEEDS_PROCESSING)
    }
}
