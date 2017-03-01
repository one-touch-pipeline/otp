package de.dkfz.tbi.otp.dataprocessing.rnaAlignment

import de.dkfz.tbi.otp.dataprocessing.*

class RnaRoddyBamFile extends RoddyBamFile {

    static final CHIMERIC_BAM_SUFFIX = "chimeric_merged.mdup.bam"

    @Override
    File getWorkMergedQADirectory() {
        return workQADirectory
    }

    @Override
    File getFinalMergedQADirectory() {
        return finalQADirectory
    }

    File getCorrespondingWorkChimericBamFile() {
        return new File(workDirectory, "${sampleType.dirName}_${individual.pid}_${CHIMERIC_BAM_SUFFIX}")
    }
}
