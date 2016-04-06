package de.dkfz.tbi.otp.ngsdata

import java.text.SimpleDateFormat

class RunDateParserService {

    def FileTypeService

    /**
    * This method tries to assign execution data for a run
    *
    * this class knows different standards of encoding data
    * in meta-data. If there is no MetaDataEntry with "RUN_DATE"
    * key, the run date is build from run name. The method
    * knows Solid and Illumina run naming standards.
    *
    * @param runId - database ID of Run object
    */
   public void buildExecutionDate(long runId) {
       Run run = Run.get(runId)
       Date exDate = null
       boolean consistant = true
       DataFile.findAllByRun(run).each { DataFile dataFile ->
           if (!fileTypeService.isSequenceDataFile(dataFile)) {
               return //continue
           }
           Date date = getDateFromMetaData(dataFile)
           if (!exDate) {
               exDate = date
           }
           if (exDate && !exDate.equals(date)) {
               consistant = false
           }
           dataFile.dateExecuted = date
           dataFile.save(flush: true)
       }
       // fill if all files have the same executions date
       if (exDate && consistant) {
           run.dateExecuted = exDate
       } else {
           run.dateExecuted = getDateFromRunName(run)
       }
       run.save(flush: true)
   }

   /**
    * Try to get data object from meta data text.
    * If not possible returns null
    * @param dataFile
    * @return
    */
   private Date getDateFromMetaData(DataFile dataFile) {
       MetaDataKey key = MetaDataKey.findByName(MetaDataColumn.RUN_DATE.name())
       MetaDataEntry entry = MetaDataEntry.findByDataFileAndKey(dataFile, key)
       if (!entry) {
           return null
       }
       Date date = null
       if (entry.value.size() == 6) {
           date = parseDate("yyMMdd", entry.value)
       } else if (entry.value.size() == 10) {
           date = parseDate("yyyy-MM-dd", entry.value)
       }
       entry.status = (date == null)? MetaDataEntry.Status.INVALID : MetaDataEntry.Status.VALID
       entry.save(flush: true)
       return date
   }

   /**
    * Try to get date object from run name, if not possible
    * returns null object
    * @param runName
    * @return
    */
   private Date getDateFromRunName(Run run) {
       Date date = null
       if (run.seqPlatform.name == "Illumina") {
           String subname = run.name.substring(0, 6)
           date = parseDate("yyMMdd", subname)
       } else if (run.seqPlatform.name == "SOLiD") {
           String subname = run.name.substring(10, 18)
           date = parseDate("yyyyMMdd", subname)
       }
       return date
   }

    public Date parseDateFromRunName(String runName) {
        return parseDate("yyMMdd", runName.substring(0, 6))
    }

   /**
    * Best effort to parse a text with a format
    * @param format
    * @param text
    * @return
    */
   static Date parseDate(String format, String text) {
       Date date = null
       try {
           SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format)
           date = simpleDateFormat.parse(text)
       } catch (Exception e) {
       // no exception
       }
       return date
   }
}
