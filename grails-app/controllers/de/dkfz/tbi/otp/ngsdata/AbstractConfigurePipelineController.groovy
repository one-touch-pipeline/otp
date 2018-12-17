package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService

abstract class AbstractConfigurePipelineController {

    ProjectService projectService
    ProcessingOptionService processingOptionService

    abstract Pipeline getPipeline()
}
