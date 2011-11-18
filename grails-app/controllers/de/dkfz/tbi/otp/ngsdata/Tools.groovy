package de.dkfz.tbi.otp.ngsdata

class Tools {

    /**
    *
    *
    */

    public static String getStringFromNumber(long number) {

       final long billion = 1000000000
       final long million = 1000000
       final long kilo = 1000

       if (number/billion > 0) return String.format("%.1f G",(number/billion))
       if (number/million > 0) return String.format("%.1f M", (number/million))
       if (number/kilo > 0) return String.format("%.1f k", (number/kilo))

       return (number as String)
   }
}
