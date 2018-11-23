package de.dkfz.tbi.otp.ngsdata


import groovy.transform.*

@TupleConstructor
enum QcThresholdHandling {
    NO_CHECK(false, false, false),
    CHECK_AND_NOTIFY(true, true, false),
    CHECK_NOTIFY_AND_BLOCK(true, true, true),

    final boolean checksThreshold
    final boolean notifiesUser
    final boolean blocksBamFile
}
