package de.dkfz.tbi.otp.tracking

import groovy.transform.*

import static de.dkfz.tbi.otp.tracking.ProcessingStatus.Done.*

public class ProcessingStatus {

    @TupleConstructor
    static enum WorkflowProcessingStatus {
        NOTHING_DONE_WONT_DO(NOTHING, false),
        NOTHING_DONE_MIGHT_DO(NOTHING, true),
        PARTLY_DONE_WONT_DO_MORE(PARTLY, false),
        PARTLY_DONE_MIGHT_DO_MORE(PARTLY, true),
        ALL_DONE(ALL, false)

        Done done
        boolean mightDoMore
    }

    static enum Done {
        NOTHING,
        PARTLY,
        ALL,
    }

    WorkflowProcessingStatus installationProcessingStatus
    WorkflowProcessingStatus fastqcProcessingStatus
    WorkflowProcessingStatus alignmentProcessingStatus
    WorkflowProcessingStatus snvProcessingStatus

    @Override
    public String toString() {
        return """
Installation: ${installationProcessingStatus}
FastQC:       ${fastqcProcessingStatus}
Alignment:    ${alignmentProcessingStatus}
SNV:          ${snvProcessingStatus}
"""
    }
}
