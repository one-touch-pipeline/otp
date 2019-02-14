package de.dkfz.tbi.otp.ngsdata

import java.text.SimpleDateFormat

class RunDateParserService {

    Date parseDateFromRunName(String runName) {
        return parseDate("yyMMdd", runName.substring(0, 6))
    }

   /**
    * Best effort to parse a text with a format
    * @param format
    * @param text
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
