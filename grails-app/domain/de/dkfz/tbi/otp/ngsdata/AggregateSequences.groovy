package de.dkfz.tbi.otp.ngsdata


import de.dkfz.tbi.otp.ngsdata.Individual.Type

/**
 * This domain class represents a database view called "aggregate_sequences".
 */
class AggregateSequences implements Serializable {
    // ids
    long seqTypeId
    long seqPlatformId
    long sampleId
    long seqCenterId
    long sampleTypeId
    long individualId
    long projectId

    // fields from SeqTrack
    long laneCount
    long sum_N_BasePairs
    long sum_N_BasePairsGb

    // fields from SeqPlatform
    /**
     * e.g. solid, illumina
     */
    String seqPlatformName
    String model

    // fields from SeqType
    String seqTypeName
    String seqTypeAlias
    String seqTypeAliasOrName
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

    static mapping = {
        table 'aggregate_sequences'
        version false
        cache 'read-only'
        seqTypeId column: 'seq_type_id'
        seqPlatformId column: 'seq_platform_id'
        sampleId column: 'sample_id'
        seqCenterId column: 'seq_center_id'
        sampleTypeId column: 'sample_type_id'
        individualId column: 'individual_id'
        projectId column: 'project_id'
        id composite: ['seqTypeId', 'seqPlatformId', 'sampleId', 'seqCenterId', 'sampleTypeId', 'individualId', 'projectId']
    }

    static constraints = {
        // nullable constraints from SeqPlatform
        model(nullable: true)
        // nullable constraints from SeqType
        seqTypeAlias(nullable: true)
        seqTypeAliasOrName(nullable: true)
    }
}
