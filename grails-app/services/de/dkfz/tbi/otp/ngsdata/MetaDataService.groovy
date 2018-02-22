package de.dkfz.tbi.otp.ngsdata

import org.springframework.security.access.prepost.PostAuthorize
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.access.prepost.PreFilter
import de.dkfz.tbi.otp.utils.ReferencedClass
import de.dkfz.tbi.otp.utils.StringUtils

class MetaDataService {

    static transactional = true

    /**
     * Retries a MetaDataEntry in an ACL aware manner.
     * @param id The id of the MetaDataEntry to retrieve
     * @return The MetaDataEntry if present, otherwise null
     */
    @PostAuthorize("hasRole('ROLE_OPERATOR') or (returnObject == null) or ((returnObject.dataFile.project != null) and hasPermission(returnObject.dataFile.project, 'OTP_READ_ACCESS'))")
    MetaDataEntry getMetaDataEntryById(Long id) {
        return MetaDataEntry.get(id)
    }

    /**
     * Updates the given MetaDataEntry's value to the new given value.
     * Creates a ChangeLog entry for this update.
     * @param entry The MetaDataEntry to update
     * @param value The new value to set
     * @throws ChangelogException In case the Changelog Entry could not be created
     * @throws MetaDataEntryUpdateException In case the MetaDataEntry could not be updated
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR') or ((#entry.dataFile.project != null) and hasPermission(#entry.dataFile.project.id, 'de.dkfz.tbi.otp.ngsdata.Project', write))")
    boolean updateMetaDataEntry(MetaDataEntry entry, String value) throws ChangelogException, MetaDataEntryUpdateException {
        ReferencedClass clazz = ReferencedClass.findOrSaveByClassName(MetaDataEntry.class.getName())
        ChangeLog changelog = new ChangeLog(rowId: entry.id, referencedClass: clazz, columnName: "value", fromValue: entry.value, toValue: value, comment: "-", source: ChangeLog.Source.MANUAL)
        if (!changelog.save()) {
            throw new ChangelogException("Creation of changelog failed, errors: " + changelog.errors.toString())
        }
        entry.value = value
        if (!entry.save(flush: true)) {
            throw new MetaDataEntryUpdateException(entry)
        }
        return true
    }

    /**
     * Checks for the list of given Meta Data Entries whether there exists at least one ChangeLog element.
     * A map is created with the MetaDataEntry as key and a boolean as value. True means there is at least
     * one ChangeLog entry, false means there is none.
     *
     * @param entries The MetaDataEntries for which it should be checked whether there is a ChangeLog
     * @return Map of MetaDataEntries with boolean information as value whether there is a ChangeLog
     */
    @PreFilter("hasRole('ROLE_OPERATOR') or (hasPermission(filterObject.dataFile.project, 'read') or hasPermission(filterObject.dataFile.run?.seqCenter, 'read'))")
    Map<MetaDataEntry, Boolean> checkForChangelog(List<MetaDataEntry> entries) {
        ReferencedClass clazz = ReferencedClass.findByClassName(MetaDataEntry.class.getName())
        if (!clazz) {
            Map<MetaDataEntry, Boolean> results = [:]
            entries.each { MetaDataEntry entry ->
                results.put(entry, false)
            }
            return results
        }
        List<ChangeLog> changelogs = ChangeLog.findAllByRowIdInListAndReferencedClass(entries.collect { it.id }, clazz)
        Map<MetaDataEntry, Boolean> results = [:]
        entries.each { MetaDataEntry entry ->
            results.put(entry, changelogs.find { it.rowId == entry.id } ? true : false)
        }
        return results
    }

    /**
     * Retrieves the ChangeLog for the given MetaDataEntry.
     *
     * @param entry The MetaDataEntry for which the ChangeLog should be retrieved
     * @return List of ChangeLog entries
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR') or ((#entry.dataFile.project != null) and hasPermission(#entry.dataFile.project, 'OTP_READ_ACCESS'))")
    List<ChangeLog> retrieveChangeLog(MetaDataEntry entry) {
        ReferencedClass clazz = ReferencedClass.findByClassName(MetaDataEntry.class.getName())
        if (!clazz) {
            return []
        }
        return ChangeLog.findAllByRowIdAndReferencedClass(entry.id, clazz)
    }

    /**
     * Retrieves the DataFile identified by the given ID in an ACL aware manner.
     * @param id The Id of the DataFile.
     * @return DataFile if it exists, otherwise null
     */
    @PostAuthorize("hasRole('ROLE_OPERATOR') or (returnObject == null) or ((returnObject.project != null) and hasPermission(returnObject.project.id, 'de.dkfz.tbi.otp.ngsdata.Project', 'OTP_READ_ACCESS')) or ((returnObject.run != null) and hasPermission(returnObject.run.seqCenter.id, 'de.dkfz.tbi.otp.ngsdata.SeqCenter', 'read'))")
    DataFile getDataFile(Long id) {
        return DataFile.get(id)
    }

    /**
     * Ensures that the two file names are equal except for one character, and that this character is '1' for the first
     * file name and '2' for the second file name.
     */
    static void ensurePairedSequenceFileNameConsistency(final String mate1FileName, final String mate2FileName) {
        Map<String, Character> differentCharacters = StringUtils.extractDistinguishingCharacter([mate1FileName, mate2FileName])
        if (!differentCharacters ||
                differentCharacters.get(mate1FileName) != '1' ||
                differentCharacters.get(mate2FileName) != '2') {
            throw new RuntimeException("${mate1FileName} and ${mate2FileName} are not consistent as paired sequence file names.")
        }
    }

    /**
     * tries to find out mate number of a fastq file.
     * @param dataFileName name of fastq file
     * @return mate number, 1 or 2
     * @throws RuntimeException if the mate number cannot be found.
     */
    public static int findOutMateNumber(String dataFileName) {
        assert dataFileName, "for non single-mate fastq files, file name must be provided"
        def patterns = [
            //SOMEPID_L001_R2.fastq.gz
            /.+_L00\d{1,2}.R([12]).+/,
            //s_101202_7_1.fastq.gz
            //s_101202_7_2.fastq.gz
            //s_110421_3.read2.fastq.gz
            //s_3_1_sequence.txt.gz
            //s_140304_3_001_2_sequence.txt.gz
            //s_111201_2a_1_sequence.txt.gz
            //SOMEPID_s_6_1_sequence.txt.gz
            //SOMEPID_s_3_1_sequence.txt.gz
            /.*s(?:_\d{6})?_\d{1,2}(?:[a-z])?(?:_|\.read|_\d{3}_)([12]).+/,
            /^[A-Z]{2}-\d{4}_.+_lib\d{5,6}(?:_\d{4})?(?:_\d{1,2})?_([12])(?:_sequence)?\.fastq.+/,
            /^NB_E_\d{3}_[A-Z0-9]{1,3}(?:_lane\d)?\.([12])\.fastq.+/,
            /^00_MCF10A.+\.\d{6}\.[A-Z0-9]{9}\.\d{1,2}\.([12])\.fastq.+/,
            /^RB\d{1,2}_(?:Blut|Tumor)_R([12])\.fastq.+/,
            /.*[HP]\d\d[A-Z0-9]_[A-Z0-9]{4}_L\d_.+_([12])\.fastq.+/,
            //lane6mp25PE2_2_sequence.txt.gz
            //lane211s003107_1_sequence.txt.gz
            //lane8wwmp44PE2_1_sequence.txt.gz
            //SOMEPID_lane511s003237_1_sequence.txt.gz
            /.*[Ll]ane\d.+_([12])_sequence.txt.gz$/,
            /^\d{6}_I\d{3}_[A-Z0-9]{11}_L\d_WHAIPI\d{6}-\d{2}(?:\+1)?_([12]).raw.fq.gz$/,
            /^.*ATRT\d+_lib\d*_([12]).fastq.gz$/,
            //AS-78215-LR-10213_R1.fastq.gz
            /^AS-.*-LR-.*_R([12]).fastq.gz$/,
            //SOMEPID_control_0097062_1.fastq.gz
            /^.*_(?:control|tumor)_.*_(\d).fastq.gz$/,
            /^EGAR\d*_.*_.*_([12]).fq.gz$/,
            //K002000023_42438_1.fq.gz
            /^K\d{9}_\d{5}_([12]).fq.gz$/,
        ]



        def mateNumbers = patterns.collect { pattern ->
            def matches = dataFileName =~ pattern
            matches ? matches[0][1] : null
        }.findAll { it }
        if (!mateNumbers) {
            throw new RuntimeException("cannot find mateNumber for $dataFileName")
        }
        assert mateNumbers.size() == 1, "$dataFileName matches to more then one pattern"
        return mateNumbers.first() as int // without the "as" the conversion is wrong
    }
}
