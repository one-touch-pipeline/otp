package de.dkfz.tbi.otp.ngsdata

/**
 * Defines the columns of the metadata file
 *
 */
enum MetaDataColumn {
    FASTQ_FILE,
    MD5,
    CENTER_NAME,
    RUN_ID,
    RUN_DATE,
    LANE_NO,
    BASE_COUNT,
    READ_COUNT,
    CYCLE_COUNT,
    SAMPLE_ID,
    SEQUENCING_TYPE,
    ANTIBODY_TARGET,
    ANTIBODY,
    INSTRUMENT_PLATFORM,
    INSTRUMENT_MODEL,
    PIPELINE_VERSION,
    INSERT_SIZE,
    LIBRARY_LAYOUT,
    WITHDRAWN,
    WITHDRAWN_DATE,
    COMMENT,
    BARCODE,
    LIB_PREP_KIT
}
