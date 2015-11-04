package de.dkfz.tbi.otp.ngsdata


enum InformTissueType {
    TUMOR                     ('T'),
    METASTASIS                ('M'),
    CONTROL                   ('C'),
    FFPE                      ('F'),
    PLASMA                    ('L'),

    final char key

    private InformTissueType(String key) {
        this.key = key
    }
    public static InformTissueType fromKey(String key) {
        InformTissueType informTissueType = values().find { it.key == key }
        if (informTissueType == null) {
            throw new IllegalArgumentException()
        }
        return informTissueType
    }

}