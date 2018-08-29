package de.dkfz.tbi.otp.ngsdata;

/**
 * Reflects the ngs platform labels used in
 * the header of SAM file (specification version SAMv1, see @\RG PL)
 * Maps platform labels to SAM-header identifiers.
 */
 /*
  * It would be too much to modify the domain just to reflect
  * mapping for SAM-header labels. If there is a need in a some more
  * mappings for platform labels one can think about extension of the domain.
  */
enum SAMPlatformLabel {
    CAPILLARY("capillary"),
    LS454("ls454"),
    ILLUMINA("illumina"),
    SOLID("solid"),
    HELICOS("helicos"),
    IONTORRENT("torrent"),
    PACBIO("pacbio")

    private final String key

    private SAMPlatformLabel(String key) {
        this.key = key;
    }

    /**
     * Maps an platform label to SAM-header specific platform label.
     * Tries to apply logic to map values instead of hard-coded mapping,
     * which enables e.g. modify database without changing the code up to certain level.
     *
     * @param platformLabel - label of platform
     * @return label of platform used in SAM-header
     * @throws IllegalArgumentException if the input label can not be
     * mapped to any of SAM-header platform labels
     */
    public static SAMPlatformLabel map(String platformLabel) {
        String label = platformLabel.toLowerCase()
        List<SAMPlatformLabel> matchingLabels = []
        SAMPlatformLabel.values().each { SAMPlatformLabel samLabel ->
            if (label.contains(samLabel.key)) {
                matchingLabels << samLabel
            }
        }
        if (!matchingLabels) {
            throw new IllegalArgumentException("no mapping found for ${platformLabel}")
        }
        if (matchingLabels.size() > 1) {
            throw new IllegalArgumentException("more than one match found for ${platformLabel}: ${matchingLabels}")
        }
        return matchingLabels.first()
    }
}
