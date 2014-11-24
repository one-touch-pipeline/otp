package de.dkfz.tbi.otp.job.jobs.snvcalling

import org.joda.time.LocalDate

import de.dkfz.tbi.otp.dataprocessing.snvcalling.SampleTypeCombinationPerIndividual
import de.dkfz.tbi.otp.job.processing.AbstractEndStateAwareJobImpl
import de.dkfz.tbi.otp.job.processing.ResumableJob
import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.ngsdata.SampleTypePerProject

@ResumableJob
class SamplePairDiscoveryJob extends AbstractEndStateAwareJobImpl {

    @Override
    void execute() throws Exception {

        final boolean sampleTypesNeedCategorization = logUncategorizedSampleTypes()

        createMissingDiseaseControlCombinations()

        if (sampleTypesNeedCategorization) {
            throw new RuntimeException('Some sample types are not categorized. See the job log for details.')
        } else {
            succeed()
        }
    }

    /**
     * @return Whether there are sample types which need categorization
     */
    boolean logUncategorizedSampleTypes() {
        final String uncategorizedSampleTypes = findUncategorizedSampleTypes()
        if (uncategorizedSampleTypes != null) {
            log.warn "The following sample types need to be categorized:\n${uncategorizedSampleTypes}"
            return true
        } else {
            log.info 'Did not find any sample type which needs categorization. :-)'
            return false
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

    void createMissingDiseaseControlCombinations() {
        final Collection<SampleTypeCombinationPerIndividual> combinations =
                SampleTypeCombinationPerIndividual.findMissingDiseaseControlCombinations(new LocalDate(2014, 12, 1).toDate())
        log.info "Creating ${combinations.size()} new ${SampleTypeCombinationPerIndividual.simpleName} instance(s)."
        SampleTypeCombinationPerIndividual.setNeedsProcessing(combinations, true)
    }
}
