package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.AlignmentPassService
import groovy.transform.ToString


/**
 * To receive more structure in the sample types it was decided to ask for the samples types which are expected to occur within a project.
 * These sample types are then stored in this domain per project.
 * Furthermore it is relevant to know if a sample type represents a DISEASE or a CONTROL.
 * This information will be requested via the SNV-GUI.
 *
 */
@ToString
class SampleTypePerProject {

    Project project

    SampleType sampleType

    /**
     * Holds the information if the specified sampleType is a DISEASE or a CONTROL in this project.
     */
    Category category = Category.UNKNOWN

    /**
     * This property is handled automatically by grails.
     */
    Date dateCreated

    /**
     * This property is handled automatically by grails.
     */
    Date lastUpdated


    static constraints = {
        sampleType unique: 'project'
    }

    /**
     * This enum specifies if the sample type belongs to a disease or a control.
     * In the beginning this information is not available in OTP, therefore it is set to UNKNOWN
     */
    enum Category {
        UNKNOWN,
        DISEASE,
        CONTROL
    }

    /**
     * Finds distinct pairs of [project, sampleType] with this criteria:
     * <ul>
     *     <li>At least one non-withdrawn SeqTrack exists for that combination with a sequencing type which OTP can process.</li>
     *     <li>No SampleTypePerProject exists for that combination.</li>
     * </ul>
     */
    static Collection findMissingCombinations() {
        return SampleTypePerProject.executeQuery(
            "SELECT DISTINCT st.sample.individual.project as project, st.sample.sampleType as sampleType FROM SeqTrack st " +
            "WHERE st.seqType IN (:seqTypes) " +
            "AND NOT EXISTS (FROM DataFile WHERE ${AlignmentPassService.DATA_FILE_CRITERIA} AND fileWithdrawn = true) " +
            "AND NOT EXISTS (FROM SampleTypePerProject stpp WHERE stpp.project = st.sample.individual.project AND stpp.sampleType = st.sample.sampleType)",
            [seqTypes: SeqTypeService.alignableSeqTypes(), fileType: FileType.Type.SEQUENCE], [readOnly: true])
    }
}
