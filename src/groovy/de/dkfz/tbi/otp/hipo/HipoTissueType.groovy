package de.dkfz.tbi.otp.hipo

/**
 * Tissue types as defined by the HIPO project.
 */
public enum HipoTissueType {
    TUMOR                     ('T'),
    METASTASIS                ('M'),
    SPHERE                    ('S'),
    XENOGRAFT                 ('X'),
    BIOPSY                    ('Y'),
    BLOOD                     ('B'),
    CONTROL                   ('N'),
    CELL                      ('C'),
    INVASIVE_MARGINS          ('I'),
    PATIENT_DERIVED_CULTURE   ('P'),
    CULTURE_DERIVED_XENOGRAFT ('Q'),
    LIQUID_BIOPSY             ('L'),
    BUFFY_COAT                ('F'),

    final char key

    private HipoTissueType(String key) {
        this.key = key
    }

    /**
     * Returns the corresponding {@link HipoTissueType} for a key or <code>null</code> if no
     * {@link HipoTissueType} with that key exists.
     */
    public static HipoTissueType fromKey(String key) {
        return values().find { it.key == key }
    }
}
