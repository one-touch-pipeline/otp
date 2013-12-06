package de.dkfz.tbi.otp.filehandling

/**
 * All filenames, which are not created on the fly, are stored here.
 *
 */
enum FileNames {
    FASTQ_FILES_IN_MERGEDBAMFILE("FastqFiles.tsv"),
    QA_RESULT_OVERVIEW("QAResultOverview.tsv"),
    QA_RESULT_OVERVIEW_EXTENDED("QAResultOverviewExtended.tsv")

    private final String fileName

    private FileNames(String fileName) {
        this.fileName = fileName
    }

    @Override
    String toString() {
        return this.fileName
    }
}
