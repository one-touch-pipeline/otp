package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.utils.Entity

class IndelQualityControl implements Entity {

    IndelCallingInstance indelCallingInstance

    String file

    int numIndels

    int numIns

    int numDels

    int numSize1_3

    int numSize4_10

    int numSize11plus

    int numInsSize1_3

    int numInsSize4_10

    int numInsSize11plus

    int numDelsSize1_3

    int numDelsSize4_10

    int numDelsSize11plus

    double percentIns

    double percentDels

    double percentSize1_3

    double percentSize4_10

    double percentSize11plus

    double percentInsSize1_3

    double percentInsSize4_10

    double percentInsSize11plus

    double percentDelsSize1_3

    double percentDelsSize4_10

    double percentDelsSize11plus


    static constraints = {
        file(validator: { OtpPath.isValidPathComponent(it) })
    }

    static belongsTo = [
        indelCallingInstance: IndelCallingInstance
    ]
}