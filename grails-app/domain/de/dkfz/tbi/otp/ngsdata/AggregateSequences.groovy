package de.dkfz.tbi.otp.ngsdata
/**
 * This domain class represents a database view called "aggregate_sequences".
 */
class AggregateSequences implements Serializable {
    /** IDs (primary keys) */
    long seqTypeId
    long seqPlatformId
    Long seqPlatformModelLabelId
    Long sequencingKitLabelId
    long sampleId
    long seqCenterId
    long sampleTypeId
    long individualId
    long projectId
    long realmId


    /** aggregated fields from {@link SeqTrack} */
    long laneCount
    Long sum_N_BasePairs
    Long sum_N_BasePairsGb

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

    /** fields from {@link Realm} */
    String realmName

    /** fields from {@link SeqCenter} */
    String seqCenterName
    String seqCenterDirName

    static mapping = {
        table 'aggregate_sequences'
        version false
        cache 'read-only'
        seqTypeId column: 'seq_type_id'
        seqPlatformId column: 'seq_platform_id'
        seqPlatformModelLabelId column: 'seq_platform_model_label_id'
        sequencingKitLabelId column: 'sequencing_kit_label_id'
        sampleId column: 'sample_id'
        seqCenterId column: 'seq_center_id'
        sampleTypeId column: 'sample_type_id'
        individualId column: 'individual_id'
        projectId column: 'project_id'
        realmId column: 'realm_id'
        id composite: ['seqTypeId', 'seqPlatformId', 'sampleId', 'seqCenterId', 'sampleTypeId', 'individualId', 'projectId']
    }

    static constraints = {
        // nullable constraints from SeqPlatform
        seqPlatformModelLabelId(nullable: true)
        sequencingKitLabelId(nullable: true)
        // nullable constraints from SeqPlatformModelLabel
        seqPlatformModelLabelName(nullable: true)
        // nullable constraints from SequencingKitLabel
        sequencingKitLabelName(nullable: true)
    }
}
