package de.dkfz.tbi.otp.dataprocessing.roddyExecution

import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.ngsdata.ReferenceGenome


trait RoddyAnalysisResult extends RoddyResult {

    @Override
    Pipeline getPipeline() {
        return config.pipeline
    }

    @Override
    File getBaseDirectory() {
        return getWorkDirectory().parentFile
    }
}