package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.*

abstract class AbstractConfigurePipelineController {

    ProjectService projectService
    ProcessingOptionService processingOptionService

    abstract Pipeline getPipeline()
}
