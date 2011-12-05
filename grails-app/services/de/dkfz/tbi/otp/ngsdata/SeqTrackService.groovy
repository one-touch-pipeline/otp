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

       /*
       def entries = MetaDataEntry.findAll (
       "from MetaDataEntry mde " +
       "where mde.dataFile.run.id = ? and mde.dataFile.fileType.type = ? " +
       "and mde.dataFile.metaDataValid = ? " +
       "and mde.key = ? order by mde.value",
       run.id, FileType.Type.SEQUENCE, true, key
       )
       */

       def entries = []

       // get the list of unique lanes identifiers
       run.dataFiles.each {DataFile dataFile ->

           if (!dataFile.metaDataValid) return
           if (dataFile.fileWithdrawn) return
           if (dataFile.fileType.type != FileType.Type.SEQUENCE) return

           dataFile.metaDataEntries.each {entry ->

               if (entry.key != key) return

               // check if exists
               for(int i=0; i<entries.size(); i++) {
                   if (entries[i].value == entry.value)
                   return
               }

               entries << entry
           }
       }

       // run track creation for each lane
       for(int i=0; i<entries.size(); i++) {
           println "LANE ${entries[i].value}"
           buildOneSequenceTrack(run, entries[i].value)
       }
   }



   /**
    *
    * Builds one sequence track identified by a lane id
    *
    * @param run - Run obejct
    * @param lane - lane identifier string
    */

   private void buildOneSequenceTrack(Run run, String lane) {
       //
       // this function build one sequence track for a given run
       //

       //println "Building lane ${lane}"

       // find sequence files
       def laneDataFiles =
           getRunFilesWithTypeAndLane(run, FileType.Type.SEQUENCE, lane)

       // check if metadata consistent

       def keyNames = [
           "SAMPLE_ID",
           "SEQUENCING_TYPE",
           "LIBRARY_LAYOUT",
           "PIPELINE_VERSION",
           "READ_COUNT"
       ]

       def keys = []
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

       fillReadsForSeqTrack(seqTrack);
       safeSave(seqTrack)

       // alignment part

       // get files
       def alignFiles =
           getRunFilesWithTypeAndLane(run, FileType.Type.ALIGNMENT, lane)

       // no alignment files
       if (!alignFiles) return

       // find out if data complete
       def alignKeyNames = ["SAMPLE_ID", "ALIGN_TOOL"]
       def alignKeys = []
       alignKeyNames.each {
           MetaDataKey key = MetaDataKey.findByName(it)
           alignKeys << key
       }

       def alignValues = []
       consistent = checkIfConsistent(alignFiles, alignKeys, alignValues)

       //println "${alignValues} ${consistent}"
       if (!consistent) return
       if (values[0] != alignValues[0]) return


       println "alignment data found"

       // create or find aligment params object

       String alignProgram = alignValues[1] ?: values[3]
       AlignmentParams alignParams = AlignmentParams.findByProgramName(alignProgram)

       if (!alignParams)
       alignParams = new AlignmentParams(programName: alignProgram)
       safeSave(alignParams)

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
       safeSave(alignLog)
       safeSave(alignParams)
       safeSave(seqTrack)
   }



   /**
    *
    * @param run
    * @param type
    * @param lane
    * @return
    */

   private def getRunFilesWithTypeAndLane(Run run, FileType.Type type, String lane) {
       //
       // helper function
       // return all dataFiles for a given run, type and lane
       //

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

   private boolean checkIfConsistent(def dataFiles, def keys, def values) {
       //
       // helper function
       // checks if metadata entries for a given dataFiles
       // and keys are consistent, fill values collection
       //

       if (dataFiles == null) return false;

       boolean consistent = true
       for(int iKey=0; iKey<keys.size; iKey++) {

           MetaDataEntry reference =
                   getMetaDataEntry(dataFiles[0], keys[iKey])
           //MetaDataEntry.findByDataFileAndKey(dataFiles[0], keys[iKey])

           values[iKey] = reference?.value

           for(int iFile = 1; iFile < dataFiles.size; iFile++) {
               MetaDataEntry entry = getMetaDataEntry(dataFiles[iFile], keys[iKey])
               //MetaDataEntry.findByDataFileAndKey(dataFiles[iFile], keys[iKey])

               if (entry?.value != reference?.value) {
                   println entry?.value
                   println reference?.value
                   consistent = false
               }
           }
       }

       consistent
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

           def dataFiles = seqTrack.dataFiles

           def dbKeys = ["BASE_COUNT", "READ_COUNT", "INSERT_SIZE"]
           def dbFields = ["nBasePairs", "nReads", "insertSize"]
           def add = [true, false, false]

           dataFiles.each {file ->

               if (file.fileType.type != FileType.Type.SEQUENCE) return

               file.metaDataEntries.each {entry ->

                   for(int iKey=0; iKey < dbKeys.size(); iKey++) {

                       if (entry.key.name == dbKeys[iKey]) {

                           long value = 0;
                           if  (entry.value.isLong())
                               value = entry.value as long

                           if (add[iKey])
                               seqTrack."${dbFields[iKey]}" += value
                           else
                               seqTrack."${dbFields[iKey]}" = value
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

       run.dataFiles.each { dataFile ->

           if (dataFile.fileType.type == FileType.Type.SEQUENCE) {
               dataFile.used = (dataFile.seqTrack != null)
               if (!dataFile.used) {
                   println dataFile
                   run.allFilesUsed = false
               }
           }

           if (dataFile.fileType.type == FileType.Type.ALIGNMENT) {
               dataFile.used = (dataFile.alignmentLog != null)
               if (!dataFile.used) {
                   println dataFile
                   run.allFilesUsed = false
               }
           }
       }

       println "All files used: ${run.allFilesUsed}\n"
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
              //println entry.value
          }
      }

      return entry
  }

   /**
   *
   * probably will go to separate static class
   * no formal exception, information only
   *
   * @param obj
   */

  private void safeSave(def obj) {

      obj.validate()
      if (obj.hasErrors()) {
          println obj.errors
          return
      }

      if (!obj.save())
      println "can not save ${obj}"
  }

}
