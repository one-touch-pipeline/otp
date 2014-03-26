package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.ngsdata.Individual.Type
import de.dkfz.tbi.otp.ngsdata.Run.StorageRealm
import de.dkfz.tbi.otp.ngsdata.SeqTrack.DataProcessingState
import de.dkfz.tbi.otp.ngsdata.SeqTrack.QualityEncoding

/**
 * This domain class represents a database view called "sequences".
 *
 * It is mostly a join of the fields from:
 * <ul>
 * <li>SeqTrack</li>
 * <li>SeqType</li>
 * <li>SeqPlatform</li>
 * <li>Sample</li>
 * <li>Run</li>
 * <li>PipelineVersion</li>
 * <li>SeqCenter</li>
 * <li>SampleType</li>
 * <li>Individual</li>
 * <li>Project</li>
 * </ul>
 */
class Sequence implements Serializable {
    // ids
    long seqTrackId
    long seqTypeId
    long seqPlatformId
    long sampleId
    long runId
    long pipelineVersionId
    long seqCenterId
    long sampleTypeId
    long individualId
    long projectId

    // fields from SeqTrack
    String laneId
    boolean hasFinalBam
    boolean hasOriginalBam
    boolean usingOriginalBam
    long nBasePairs
    long nReads
    int insertSize

    SeqTrack.QualityEncoding qualityEncoding
    SeqTrack.DataProcessingState alignmentState
    SeqTrack.DataProcessingState fastqcState

    // fields from Run
    /**
     * Run name
     */
    String name
    Date dateExecuted
    Date dateCreated
    boolean blacklisted
    boolean multipleSource
    Run.StorageRealm storageRealm
    Double dataQuality
    boolean qualityEvaluated

    // fields from SeqPlatform
    /**
     * e.g. solid, illumina
     */
    String seqPlatformName
    String model

    // fields from SeqType
    String seqTypeName
    String libraryLayout
    String dirName

    // fields from SampleType
    String sampleTypeName

    // fields from Individual
    /**
     * real pid from iChip
     */
    String pid
    /**
     * pid used in the project
     */
    String mockPid
    /**
     * mnemonic used in the project
     */
    String mockFullName
    Individual.Type type

    // fields from Project
    String projectName
    String projectDirName
    String realmName

    // fields from SeqCenter
    String seqCenterName
    String seqCenterDirName

    // derived property for calculating the creation date without time
    Date dayCreated

    static mapping = {
        table 'sequences'
        version false
        cache 'read-only'
        seqTrackId column: 'seq_track_id'
        seqTypeId column: 'seq_type_id'
        seqPlatformId column: 'seq_platform_id'
        sampleId column: 'sample_id'
        runId column: 'run_id'
        pipelineVersionId column: 'pipeline_version_id'
        seqCenterId column: 'seq_center_id'
        sampleTypeId column: 'sample_type_id'
        individualId column: 'individual_id'
        projectId column: 'project_id'
        id composite: ['seqTrackId', 'seqTypeId', 'seqPlatformId', 'sampleId', 'runId', 'pipelineVersionId', 'seqCenterId', 'sampleTypeId', 'individualId', 'projectId']
        dayCreated formula: 'DATE(date_created)'
    }

    static constraints = {
        // nullable constraints from Run
        storageRealm(nullable: true)
        dateExecuted(nullable: true)
        dataQuality(nullable: true)
        qualityEvaluated(nullable: true)
        // nullable constraints from SeqPlatform
        model(nullable: true)
    }
}
