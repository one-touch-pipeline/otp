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
    /** IDs (primary keys) */
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


    /** fields from {@link SeqTrack} */
    String laneId
    String libraryName
    boolean hasOriginalBam
    Long nBasePairs
    int insertSize
    SeqTrack.QualityEncoding qualityEncoding
    SeqTrack.DataProcessingState fastqcState
    SeqTrack.Problem problem

    /** fields from {@link IlseSubmission} */
    /** actually ilseNumber */
    Integer ilseId

    /** fields from {@link Run} */
    /** actually run name */
    String name
    Date dateExecuted
    Date dateCreated
    boolean blacklisted

    /** fields from {@link SeqPlatform} */
    String seqPlatformName

    /** fields from {@link SeqPlatformModelLabel} */
    String seqPlatformModelLabelName

    /** fields from {@link SequencingKitLabel} */
    String sequencingKitLabelName

    /** fields from {@link SeqType} */
    String seqTypeName
    String seqTypeDisplayName
    String libraryLayout
    String dirName

    /** fields from {@link SampleType} */
    String sampleTypeName

    /** fields from {@link Individual} */
    String pid
    String mockPid
    String mockFullName
    Individual.Type type

    /** fields from {@link Project} */
    String projectName
    String projectDirName
    String realmName

    /** fields from {@link SeqCenter} */
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
        nBasePairs(nullable: true)
        problem nullable: true
    }

    String getLibraryLayoutDirName() {
        return SeqType.getLibraryLayoutDirName(libraryLayout)
    }
}
