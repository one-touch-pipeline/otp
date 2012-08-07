package de.dkfz.tbi.otp.utils

import grails.converters.JSON

/**
 * Controller for enabling/disabling the auto-refresh functionality
 * of List Views.
 *
 *
 */
class RefreshController {

    /**
     * Enables auto-refresh.
     *
     * @return JSON with field enabled containing boolean of state after performing the action
     */
    def enable() {
        session["auto-refresh"] = true
        def result = [enabled: session["auto-refresh"]]
        render result as JSON
    }

    /**
     * Disables auto-refresh.
     *
     * @return JSON with field enabled containing boolean of state after performing the action
     */
    def disable() {
        session["auto-refresh"] = false
        def result = [enabled: session["auto-refresh"]]
        render result as JSON
    }
}
