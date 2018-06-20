package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.TimeStamped
import de.dkfz.tbi.otp.utils.Entity
import groovy.transform.ToString


/**
 * To receive more structure in the sample types it was decided to ask for the samples types which are expected to occur within a project.
 * These sample types are then stored in this domain per project.
 * Furthermore it is relevant to know if a sample type represents a DISEASE or a CONTROL.
 * This information will be requested via the SNV-GUI.
 *
 */
@ToString(excludes=['dateCreated','lastUpdated'], includePackage = false)
class SampleTypePerProject implements TimeStamped, Entity {

    Project project

    SampleType sampleType

    /**
     * Holds the information if the specified sampleType is a DISEASE or a CONTROL in this project.
     */
    SampleType.Category category


    static constraints = {
        sampleType unique: 'project'
    }

    /**
     * Finds distinct pairs of [project, sampleType] with this criteria:
     * <ul>
     *     <li>At least one non-withdrawn SeqTrack exists for that combination with a sequencing type which OTP can process.</li>
     *     <li>The category of the sample type is unknown (but not {@link SampleType.Category#IGNORED}) for the project,
     *         i.e. no SampleTypePerProject instance exists for that combination.</li>
     * </ul>
     */
    static Collection findMissingCombinations() {
        return SampleTypePerProject.executeQuery(
            "SELECT DISTINCT project as project, sampleType as sampleType " +
            "FROM SeqTrack st " +
            "    join st.sample.individual.project project " +
            "    join st.sample.sampleType sampleType " +
            "WHERE st.seqType IN (:seqTypes) " +
            "AND NOT EXISTS (FROM DataFile WHERE seqTrack = st AND fileType.type = :fileType AND fileWithdrawn = true) " +
            "AND NOT EXISTS (FROM SampleTypePerProject stpp WHERE stpp.project = st.sample.individual.project AND stpp.sampleType = st.sample.sampleType)",
            [seqTypes: SeqType.getAllAnalysableSeqTypes(), fileType: FileType.Type.SEQUENCE], [readOnly: true])
    }
}
