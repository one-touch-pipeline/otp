package de.dkfz.tbi.otp.dataprocessing

interface MergeConstants {

    static final String MERGE_TOOL_PICARD = "picard"

    static final String MERGE_TOOL_BIOBAMBAM = "biobambam"

    static final String MERGE_TOOL_SAMBAMBA = "sambamba"

    static final List<String> ALL_MERGE_TOOLS = [
            MERGE_TOOL_PICARD,
            MERGE_TOOL_BIOBAMBAM,
            MERGE_TOOL_SAMBAMBA,
    ].asImmutable()

}
