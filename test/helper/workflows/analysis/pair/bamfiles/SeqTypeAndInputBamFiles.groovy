package workflows.analysis.pair.bamfiles

import de.dkfz.tbi.otp.ngsdata.*

interface SeqTypeAndInputBamFiles {

    SeqType seqTypeToUse()

    File getBamFilePairBaseDirectory()

    BamFileSet getBamFileSet()
}

