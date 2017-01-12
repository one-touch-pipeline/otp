package de.dkfz.tbi.otp.dataprocessing

class RnaRoddyBamFile extends RoddyBamFile {

    @Override
    File getWorkMergedQADirectory() {
        return workQADirectory
    }

    @Override
    File getFinalMergedQADirectory() {
        return finalQADirectory
    }
}
