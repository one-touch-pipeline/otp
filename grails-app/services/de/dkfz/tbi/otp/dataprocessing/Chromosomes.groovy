package de.dkfz.tbi.otp.dataprocessing

/**
 * ENUM for the chromosome names
 */
enum Chromosomes {
     CHR_1("1"), CHR_2("2"), CHR_3("3"), CHR_4("4"), CHR_5("5"), CHR_6("6"), CHR_7("7"), CHR_8("8"), CHR_9("9"), CHR_10("10"), CHR_11("11"),
     CHR_12("12"), CHR_13("13"), CHR_14("14"), CHR_15("15"), CHR_16("16"), CHR_17("17"), CHR_18("18"), CHR_19("19"), CHR_20("20"),
     CHR_21("21"), CHR_22("22"), CHR_X("X"), CHR_Y("Y"), CHR_M("M"), CHR_ASTERISK("*")

     final String chr

     public Chromosomes(String chr) {
         this.chr = chr
     }

     static List<Chromosomes> numeric() {
         return Arrays.asList(Chromosomes.values()).subList(0, 22)
     }

     static List<Chromosomes> character() {
         return Arrays.asList(Chromosomes.values()).subList(22, 26)
     }

     static List<String> numericValues() {
         return numeric()*.chr
     }

     static List<String> characterValues() {
         return character()*.chr
     }

     static List<String> filterConditions() {
         return [CHR_M, CHR_ASTERISK]*.chr
     }

     static int numberOfNumericChromosomes() {
         return numeric().size()
     }
 }
