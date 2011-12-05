package de.dkfz.tbi.otp.ngsdata

class SeqTrackService {

    /**
    *
    * A sequence track corresponds to one lane in Illumina
    * and one slide in Solid.
    *
    * This method build sequenc tracks for a given run.
    * To each sequence track there are raw data and alignment
    * data attached
    *
    * @param runId
    */
   void buildSequenceTracks(long runId) {
       Run run = Run.get(runId)
       // find out present lanes/slides
       // lines/ slides could by identifiers not only numbers
       MetaDataKey key = MetaDataKey.findByName("LANE_NO")
       List<MetaDataEntry> entries = []
       // get the list of unique lanes identifiers
       run.dataFiles.each {DataFile dataFile ->
           if (!dataFile.metaDataValid) {
               return
           }
           if (dataFile.fileWithdrawn) {
               return
           }
           if (dataFile.fileType.type != FileType.Type.SEQUENCE) {
               return
           }
           dataFile.metaDataEntries.each { MetaDataEntry entry ->
               if (entry.key != key) {
                   return
               }
               // check if exists
               for(int i=0; i<entries.size(); i++) {
                   if (entries[i].value == entry.value) {
                       return
                   }
               }
               entries << entry
           }
       }
       // run track creation for each lane
       for(int i=0; i<entries.size(); i++) {
           log.debug("LANE ${entries[i].value}")
           buildOneSequenceTrack(run, entries[i].value)
       }
   }

   /**
    * Builds one sequence track identified by a lane id
    *
    * @param run - Run obejct
    * @param lane - lane identifier string
    */
   private void buildOneSequenceTrack(Run run, String lane) {
       // find sequence files
       def laneDataFiles =
           getRunFilesWithTypeAndLane(run, FileType.Type.SEQUENCE, lane)
       // check if metadata consistent
       List<String> keyNames = [
           "SAMPLE_ID",
           "SEQUENCING_TYPE",
           "LIBRARY_LAYOUT",
           "PIPELINE_VERSION",
           "READ_COUNT"
       ]
       List<MetaDataKey> keys = []
       keyNames.each {
           MetaDataKey key = MetaDataKey.findByName(it)
           keys << key
       }

       def values = []

       //println keys
       boolean consistent = checkIfConsistent(laneDataFiles, keys, values)

       // error handling
       if (!consistent) return

       // check if complete
       // to be implemented

       // build structure

       //println values
       SampleIdentifier sampleId = SampleIdentifier.findByName(values[0])
       Sample sample = sampleId.sample
       if (sample == null) return

       SeqType seqType = SeqType.findByNameAndLibraryLayout(values[1], values[2])
       if (seqType == null) return

       SeqTrack seqTrack = new SeqTrack(
           run : run,
           sample : sample,
           seqType : seqType,
           seqTech : run.seqTech,
           laneId : lane,
           hasFinalBam : false,
           hasOriginalBam : false,
           usingOriginalBam : false
       )
       laneDataFiles.each {
           seqTrack.addToDataFiles(it)
       }
       fillReadsForSeqTrack(seqTrack)
       seqTrack.save()
       // alignment part
       // get files
       def alignFiles =
           getRunFilesWithTypeAndLane(run, FileType.Type.ALIGNMENT, lane)
       // no alignment files
       if (!alignFiles) {
           return
       }
       // find out if data complete
       List<String> alignKeyNames = ["SAMPLE_ID", "ALIGN_TOOL"]
       List<MetaDataKey> alignKeys = []
       alignKeyNames.each {
           MetaDataKey key = MetaDataKey.findByName(it)
           alignKeys << key
       }
       def alignValues = []
       consistent = checkIfConsistent(alignFiles, alignKeys, alignValues)
       if (!consistent || values[0] != alignValues[0]) {
           return
       }
       log.debug("alignment data found")
       // create or find aligment params object
       String alignProgram = alignValues[1] ?: values[3]
       AlignmentParams alignParams = AlignmentParams.findByProgramName(alignProgram)
       if (!alignParams) {
           alignParams = new AlignmentParams(programName: alignProgram)
       }
       alignParams.save()

       // create alignment log
       AlignmentLog alignLog = new AlignmentLog(
           alignmentParams : alignParams,
           seqTrack : seqTrack,
           executedBy : AlignmentLog.Execution.INITIAL
       )

       // attach data files
       alignFiles.each {
           alignLog.addToDataFiles(it)
       }
       seqTrack.hasOriginalBam = true
       // save
       alignLog.save()
       alignParams.save()
       seqTrack.save()
   }

   /**
    * Return all dataFiles for a given run, type and lane
    * 
    * @param run The Run
    * @param type The Type
    * @param lane The lane
    * @return
    */
   private def getRunFilesWithTypeAndLane(Run run, FileType.Type type, String lane) {
       MetaDataKey key = MetaDataKey.findByName("LANE_NO")

       def c = DataFile.createCriteria()
       def dataFiles = c.list {
           and {
               eq("run", run)
               eq("fileWithdrawn", false)
               fileType{
                   eq("type", type)
               }
               metaDataEntries {
                   and{
                       eq("key", key)
                       eq("value", lane)
                   }
               }
           }
       }
       return dataFiles
   }

   /**
    *
    * Check if meta-data values for DataFile objects belonging
    * presumably to the same lane are consistent
    * The function returns the values associated with the
    * keys to be checked
    *
    * @param dataFiles - array of data files
    * @param keys - keys for which the consistency have to be checked
    * @param values - this array will be filled with values for given keys
    * @return consistency status
    */

   private boolean checkIfConsistent(def dataFiles, List<MetaDataKey>keys, def values) {
       if (dataFiles == null) {
           return false
       }
       boolean consistent = true
       for(int iKey=0; iKey<keys.size; iKey++) {
           MetaDataEntry reference =
                   getMetaDataEntry(dataFiles[0], keys[iKey])
           values[iKey] = reference?.value

           for(int iFile = 1; iFile < dataFiles.size; iFile++) {
               MetaDataEntry entry = getMetaDataEntry(dataFiles[iFile], keys[iKey])
               if (entry?.value != reference?.value) {
                   log.debug(entry?.value)
                   log.debug(reference?.value)
                   consistent = false
               }
           }
       }
       return consistent
   }

   /**
    *
    * Fills the numbers in the SeqTrack object using MetaDataEntry
    * objects from the DataFile objects belonging to this SeqTrack.
    *
    * @param seqTrack
    */
   private void fillReadsForSeqTrack(SeqTrack seqTrack) {
       if (seqTrack.seqTech.name == "illumina") {
           List<DataFile> dataFiles = seqTrack.dataFiles
           List<String> dbKeys = ["BASE_COUNT", "READ_COUNT", "INSERT_SIZE"]
           List<String> dbFields = ["nBasePairs", "nReads", "insertSize"]
           List<Boolean> add = [true, false, false]
           dataFiles.each { DataFile file ->
               if (file.fileType.type != FileType.Type.SEQUENCE) {
                   return
               }
               file.metaDataEntries.each { MetaDataEntry entry ->
                   for(int iKey=0; iKey < dbKeys.size(); iKey++) {
                       if (entry.key.name == dbKeys[iKey]) {
                           long value = 0
                           if (entry.value.isLong()) {
                               value = entry.value as long
                           }
                           if (add[iKey]) {
                               seqTrack."${dbFields[iKey]}" += value
                           } else {
                               seqTrack."${dbFields[iKey]}" = value
                           }
                       }
                   }
               }
           }
       }
   }

   /**
    *
    * checks if all sequence (raw and aligned) data files are attached
    * to SeqTrack objects. If yes the field DataFile.used field is set to true.
    * if data file is a sequence or alignment type and is
    * not used it contains errors in meta-data
    *
    * @param runId
    */
   void checkSequenceTracks(long runId) {
       Run run = Run.get(runId)
       run.allFilesUsed = true
       run.dataFiles.each { DataFile dataFile ->
           if (dataFile.fileType.type == FileType.Type.SEQUENCE) {
               dataFile.used = (dataFile.seqTrack != null)
               if (!dataFile.used) {
                   log.debug(dataFile)
                   run.allFilesUsed = false
               }
           }
           if (dataFile.fileType.type == FileType.Type.ALIGNMENT) {
               dataFile.used = (dataFile.alignmentLog != null)
               if (!dataFile.used) {
                   log.debug(dataFile)
                   run.allFilesUsed = false
               }
           }
       }
       log.debug("All files used: ${run.allFilesUsed}\n")
   }

   /**
   *
   * Returns a metat data entry belonging the a given data file
   * with a key specified by the input parameter
   *
   * @param file
   * @param key
   * @return
   */
  private MetaDataEntry getMetaDataEntry(DataFile file, MetaDataKey key) {
      getMetaDataEntry(file, key.name)
  }

   /**
   *
   * Returns a metat data entry belonging the a given data file
   * with a key specified by the input parameter
   *
   * @param file
   * @param key
   * @return
   */
  private MetaDataEntry getMetaDataEntry(DataFile file, String key) {
      MetaDataEntry entry = null
      file.metaDataEntries.each { MetaDataEntry iEntry ->
          if (iEntry.key.name == key) {
              entry = iEntry
          }
      }
      return entry
  }
}
