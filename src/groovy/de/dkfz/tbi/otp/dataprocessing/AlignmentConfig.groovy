package de.dkfz.tbi.otp.dataprocessing

interface AlignmentConfig {

    abstract AlignmentInfo getAlignmentInformation()

    abstract Pipeline getPipeline()
}
