package de.dkfz.tbi.otp.dataprocessing

import groovy.transform.TupleConstructor

@TupleConstructor
enum CellRangerParameters {
    ID('--id', true),
    FASTQ('--fastqs', true),
    TRANSCRIPTOME('--transcriptome', true),
    SAMPLE('--sample', true),
    EXPECT_CELLS('--expect-cells', true),
    LOCAL_CORES('--localcores', true),
    LOCAL_MEM('--localmem', true),
    FORCE_CELLS('--force-cells', false),

    final String parameterName
    final boolean required
}
