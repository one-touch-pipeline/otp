package de.dkfz.tbi.otp.job.jobs.utils

/**
 * An {@link Enum} that contains all valid keys for job input and output parameters.
 *
 */
enum JobParameterKeys {

    PBS_ID_LIST ('__pbsIds'),
    REALM       ('__pbsRealm'),

    private final String name

    private JobParameterKeys(String name) {
        this.name = name
    }

    /**
     * Convenience method that returns the key name for use in {@link GString}s.
     */
    @Override
    String toString() {
        return this.name
    }
}
