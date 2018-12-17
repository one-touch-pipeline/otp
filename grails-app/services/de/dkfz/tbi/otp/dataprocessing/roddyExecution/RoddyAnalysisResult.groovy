package de.dkfz.tbi.otp.dataprocessing.roddyExecution

import de.dkfz.tbi.otp.dataprocessing.*


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