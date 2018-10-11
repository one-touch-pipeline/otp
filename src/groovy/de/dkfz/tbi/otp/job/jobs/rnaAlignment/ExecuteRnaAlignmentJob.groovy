package de.dkfz.tbi.otp.job.jobs.rnaAlignment

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.ast.*
import de.dkfz.tbi.otp.job.jobs.roddyAlignment.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*

@Component
@Scope("prototype")
@UseJobLog
class ExecuteRnaAlignmentJob extends ExecutePanCanJob {

    @Override
    protected List<String> prepareAndReturnWorkflowSpecificCValues(RoddyBamFile roddyBamFile) {
        assert roddyBamFile : "roddyBamFile must not be null"
        List<File> filesToMerge = getFilesToMerge(roddyBamFile)
        List<String> cValues = prepareAndReturnAlignmentCValues(roddyBamFile)

        cValues.add("fastq_list:${filesToMerge.join(";")}")

        String adapterSequence = CollectionUtils.exactlyOneElement(roddyBamFile.containedSeqTracks*.libraryPreparationKit*.reverseComplementAdapterSequence.unique().findAll(),
                "There is not exactly one reverse complement adapter sequence available for BAM file ${roddyBamFile}")
        assert adapterSequence : "There is exactly one reverse complement adapter sequence available for BAM file ${roddyBamFile}, but it is null"

        cValues.add("ADAPTER_SEQ:${adapterSequence}")
        // the following two variables need to be provided since Roddy does not use the normal path definition for RNA
        cValues.add("ALIGNMENT_DIR:${roddyBamFile.workDirectory}")
        cValues.add("outputBaseDirectory:${roddyBamFile.workDirectory}")

        if (roddyBamFile.seqType.libraryLayout == LibraryLayout.SINGLE) {
            cValues.add("useSingleEndProcessing:true")
        } else if (roddyBamFile.seqType.libraryLayout == LibraryLayout.PAIRED) {
            cValues.add("useSingleEndProcessing:false")
        }

        return cValues
    }
}
