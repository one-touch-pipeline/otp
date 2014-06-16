package de.dkfz.tbi.otp.infrastructure

import de.dkfz.tbi.otp.job.processing.ProcessingStep

class ProcessingStepThreadLocal {

    private static final ThreadLocal<ProcessingStep> processingStepHolder = new ThreadLocal<ProcessingStep>()

    public static ProcessingStep getProcessingStep() {
        return processingStepHolder.get()
    }

    public static void setProcessingStep(ProcessingStep step) {
        notNull(step, "The processingStep may not be null")
        processingStepHolder.set(step)
    }

    public static void removeProcessingStep() {
        processingStepHolder.remove()
    }
}
