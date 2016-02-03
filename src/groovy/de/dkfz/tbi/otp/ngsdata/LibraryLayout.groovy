package de.dkfz.tbi.otp.ngsdata

import groovy.transform.TupleConstructor

@TupleConstructor
enum LibraryLayout {

    SINGLE(1),
    PAIRED(2),
    MATE_PAIR(2)

    final int mateCount
}
