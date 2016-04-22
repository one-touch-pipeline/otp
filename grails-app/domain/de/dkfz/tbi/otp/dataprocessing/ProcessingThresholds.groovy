package de.dkfz.tbi.otp.dataprocessing

import groovy.transform.ToString
import de.dkfz.tbi.otp.ngsdata.*

/**
 * For some processes, e.g. SNV pipeline, it is only reasonable to be started when a defined threshold, e.g. coverage, is reached.
 * These thresholds are stored in this domain.
 * It is project depending if the number of lanes or the coverage threshold is known. Therefore both possibilities are included in this domain.
 * At least one of the two properties have to be filled in.
 *
 */
@ToString
class ProcessingThresholds {

    Project project

    SeqType seqType

    SampleType sampleType

    /**
     * This property contains the coverage which has to be reached to start a process.
     */
    Double coverage

    /**
     * This property contains the number of lanes which have to be reached to start a process.
     */
    Integer numberOfLanes

    /**
     * These properties are handled automatically by grails.
     */
    Date dateCreated
    Date lastUpdated

    static constraints = {
        coverage nullable: true, validator: {val, obj ->
            return (val != null && val > 0) || (val == null && obj.numberOfLanes != null)
        }
        numberOfLanes nullable: true, validator: {val, obj ->
            return (val != null && val > 0) || (val == null && obj.coverage != null)
        }
    }

    boolean isAboveLaneThreshold(AbstractMergedBamFile bamFile) {
        assert bamFile : 'bam file may not be null'
        assert bamFile.numberOfMergedLanes : 'property numberOfMergedLanes of the bam has to be set'
        return numberOfLanes == null || numberOfLanes <= bamFile.numberOfMergedLanes
    }

    boolean isAboveCoverageThreshold(AbstractMergedBamFile bamFile) {
        assert bamFile : 'bam file may not be null'
        assert bamFile.coverage : 'property coverage of the bam has to be set'
        return coverage == null || coverage <= bamFile.coverage
    }
}
