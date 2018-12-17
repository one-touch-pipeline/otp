package workflows.analysis.pair.bamfiles

import de.dkfz.tbi.otp.ngsdata.SeqType

interface SeqTypeAndInputBamFiles {

    SeqType seqTypeToUse()

    @SuppressWarnings('JavaIoPackageAccess')
    File getBamFilePairBaseDirectory()

    BamFileSet getBamFileSet()
}

