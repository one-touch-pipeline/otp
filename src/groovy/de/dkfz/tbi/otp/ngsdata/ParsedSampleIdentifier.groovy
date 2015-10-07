package de.dkfz.tbi.otp.ngsdata

interface ParsedSampleIdentifier {

    /**
     * @see Project#name
     */
    String getProjectName()

    /**
     * @see Individual#pid
     */
    String getPid()

    /**
     * @see SampleType#name
     */
    String getSampleTypeDbName()

    /**
     * @see SampleIdentifier#name
     */
    String getFullSampleName()
}
