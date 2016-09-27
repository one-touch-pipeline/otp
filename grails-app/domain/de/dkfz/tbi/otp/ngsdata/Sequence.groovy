package de.dkfz.tbi.otp.ngsdata
/**
 * This domain class represents a database view called "sequences".
 *
 * It is mostly a join of the fields from:
 * <ul>
 * <li>SeqTrack</li>
 * <li>SeqType</li>
 * <li>SeqPlatform</li>
 * <li>SeqPlatformModelLabel</li>
 * <li>SequencingKitLabel</li>
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
    Long seqPlatformModelLabelId
    Long sequencingKitLabelId
    long sampleId
    long runId
    long pipelineVersionId
    long seqCenterId
    long sampleTypeId
    long individualId
    long projectId
    String ilseId

    // fields from SeqTrack
    String laneId
    String libraryName
    boolean hasOriginalBam
    Long nBasePairs
    int insertSize

    SeqTrack.QualityEncoding qualityEncoding
    SeqTrack.DataProcessingState fastqcState

    // fields from Run
    /**
     * Run name
     */
    String name
    Date dateExecuted
    Date dateCreated
    boolean blacklisted

    // fields from SeqPlatform
    /**
     * e.g. solid, illumina
     */
    String seqPlatformName

    // fields from SeqPlatformModelLabel
    String seqPlatformModelLabelName

    // fields from SequencingKitLabel
    String sequencingKitLabelName

    // fields from SeqType
    String seqTypeName
    String seqTypeAlias
    String seqTypeDisplayName
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
        seqPlatformModelLabelId column: 'seq_platform_model_label_id'
        sequencingKitLabelId column: 'sequencing_kit_label_id'
        sampleId column: 'sample_id'
        runId column: 'run_id'
        pipelineVersionId column: 'pipeline_version_id'
        seqCenterId column: 'seq_center_id'
        sampleTypeId column: 'sample_type_id'
        individualId column: 'individual_id'
        projectId column: 'project_id'
        ilseId column: 'ilse_id'
        id composite: ['seqTrackId', 'seqTypeId', 'seqPlatformId', 'sampleId', 'runId', 'pipelineVersionId', 'seqCenterId', 'sampleTypeId', 'individualId', 'projectId']
        dayCreated formula: 'DATE(date_created)'
    }

    static constraints = {
        // nullable constraints from Run
        dateExecuted(nullable: true)
        // nullable constraints from SeqPlatform
        seqPlatformModelLabelId(nullable: true)
        sequencingKitLabelId(nullable: true)
        // nullable constraints from SeqPlatformModelLabel
        seqPlatformModelLabelName(nullable: true)
        // nullable constraints from SequencingKitLabel
        sequencingKitLabelName(nullable: true)
        // nullable constraints from SeqType
        seqTypeAlias(nullable: true)
        seqTypeDisplayName(nullable: true)
        nBasePairs(nullable: true)
    }

    String getLibraryLayoutDirName() {
        return SeqType.getLibraryLayoutDirName(libraryLayout)
    }
}
