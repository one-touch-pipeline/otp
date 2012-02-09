package de.dkfz.tbi.otp.ngsdata
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

class MetaDataValidationService {

    private final Lock validateMetaDataLock = new ReentrantLock()

    /**
    * Checks if values of MetaDataEntry table are correct
    *
    * the following keys are checked
    * RunID, sample identifier, sequencing center
    * sequencing type and library layout
    *
    * @param runId
    */
   boolean validateMetadata(long runId) {
       Run run = Run.get(runId)
       boolean allValid = true
       validateMetaDataLock.lock()
       try {
           DataFile.findAllByRun(run).each { DataFile dataFile ->
               dataFile.metaDataValid = true
               MetaDataEntry.findAllByDataFile(dataFile).each { MetaDataEntry entry ->
                   boolean isValid = validateMetaDataEntry(run, entry)
                   if (!isValid) {
                       dataFile.metaDataValid = false
                       allValid = false
                   }
               }
               dataFile.save()
           }
       } finally {
           validateMetaDataLock.unlock()
       }
       run.save(flush: true)
       return allValid
   }

   private boolean validateMetaDataEntry(Run run, MetaDataEntry entry) {
       MetaDataEntry.Status valid = MetaDataEntry.Status.VALID
       MetaDataEntry.Status invalid = MetaDataEntry.Status.INVALID
       switch(entry.key.name) {
           case "RUN_ID":
               entry.status = (run.name == entry.value) ? valid : invalid
               break
           case "SAMPLE_ID":
               SampleIdentifier sample = SampleIdentifier.findByName(entry.value)
               entry.status = (sample != null) ? valid : invalid
               break
           case "CENTER_NAME":
               entry.status = invalid
               SeqCenter center = run.seqCenter
               if (center.dirName == entry.value.toLowerCase()) {
                   entry.status = valid
               } else if (center.name == entry.value) {
                   entry.status = valid
               }
               break
           case "SEQUENCING_TYPE":
               SeqType seqType = SeqType.findByName(entry.value)
               entry.status = (seqType != null) ? valid : invalid
               break
           case "LIBRARY_LAYOUT":
               SeqType seqType = SeqType.findByLibraryLayout(entry.value)
               entry.status = (seqType != null) ? valid : invalid
               break
       }
       entry.save(flush: true)
       return (entry.status == invalid)? false : true
   }
}
