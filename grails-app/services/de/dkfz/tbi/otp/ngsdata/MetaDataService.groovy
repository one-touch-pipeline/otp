package de.dkfz.tbi.otp.ngsdata

import org.springframework.security.access.prepost.*

import de.dkfz.tbi.otp.utils.ReferencedClass
import de.dkfz.tbi.otp.utils.StringUtils

class MetaDataService {

    static transactional = true

    /**
     * Retries a MetaDataEntry in an ACL aware manner.
     * @param id The id of the MetaDataEntry to retrieve
     * @return The MetaDataEntry if present, otherwise null
     */
    @SuppressWarnings('LineLength')
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
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    boolean updateMetaDataEntry(MetaDataEntry entry, String value) throws ChangelogException, MetaDataEntryUpdateException {
        ReferencedClass clazz = ReferencedClass.findOrSaveByClassName(MetaDataEntry.class.getName())
        ChangeLog changelog = new ChangeLog(
                rowId: entry.id,
                referencedClass: clazz,
                columnName: "value",
                fromValue: entry.value,
                toValue: value,
                comment: "-",
                source: ChangeLog.Source.MANUAL,
        )
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
    @PreFilter("hasRole('ROLE_OPERATOR')")
    Map<MetaDataEntry, Boolean> checkForChangelog(List<MetaDataEntry> entries) {
        ReferencedClass clazz = ReferencedClass.findByClassName(MetaDataEntry.class.getName())
        if (!clazz) {
            Map<MetaDataEntry, Boolean> results = [:]
            entries.each { MetaDataEntry entry ->
                results.put(entry, false)
            }
            return results
        }
        List<Long> rowIds = entries*.id
        List<ChangeLog> changelogs = rowIds ? ChangeLog.findAllByRowIdInListAndReferencedClass(rowIds, clazz) : []
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
    @SuppressWarnings('LineLength')
    @PostAuthorize("hasRole('ROLE_OPERATOR') or (returnObject == null) or ((returnObject.project != null) and hasPermission(returnObject.project, 'OTP_READ_ACCESS'))")
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
}
