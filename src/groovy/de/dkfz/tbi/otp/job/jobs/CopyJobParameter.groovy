package de.dkfz.tbi.otp.job.jobs


/**
 * After many jobs within the workflows we have to copy the results to the final folder and sometimes also link the data.
 * To have one variable solution there will be a copy job which gets as input the source location, the final location and
 * the link location. With these parameters it can copy the files correctly.
 * The projects are split between the bioquant and the dkfz lsdf, therefore it is also needed to know to which project
 * the results belong to.
 * To prevent misspelling this enum was created for the input parameter.
 *
 */
enum CopyJobParameter {
    SOURCE_LOCATION,
    TARGET_LOCATION,
    LINK_LOCATION,
    PROJECT
}
