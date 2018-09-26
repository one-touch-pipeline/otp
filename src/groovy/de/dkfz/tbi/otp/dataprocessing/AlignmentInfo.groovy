package de.dkfz.tbi.otp.dataprocessing

abstract class AlignmentInfo {
    String alignmentProgram
    String alignmentParameter

    abstract Map<String, Object> getAlignmentSpecificMessageAttributes()
}

class RoddyAlignmentInfo extends AlignmentInfo {
    String samToolsCommand
    String mergeCommand
    String mergeOptions
    String pluginVersion

    @Override
    Map<String, Object> getAlignmentSpecificMessageAttributes() {
        return [
                code  : "notification.template.alignment.processing.roddy",
                params: [
                        mergingProgram  : this.mergeCommand,
                        mergingParameter: this.mergeOptions,
                        samtoolsProgram : this.samToolsCommand,
                ],
        ]
    }
}

class SingleCellAlignmentInfo extends AlignmentInfo {

    @Override
    Map<String, Object> getAlignmentSpecificMessageAttributes() {
        return [
                code  : "notification.template.alignment.processing.singleCell",
                params: [:],
        ]
    }
}
