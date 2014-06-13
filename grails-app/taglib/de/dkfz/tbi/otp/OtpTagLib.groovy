package de.dkfz.tbi.otp

import grails.util.Environment

import grails.plugin.springsecurity.SpringSecurityUtils

class OtpTagLib {
    static namespace = "otp"

    /**
     * Dependency Injection of GrailsApplication
     */
    def grailsApplication

    /**
     * Renders a text field with editor functionality. The editor renders the text with a button next to it.
     * When clicking the button an editor field is shown with a save and cancel button. The save button will
     * perform an AJAX request to update the data field to the given field. The new value will be uploaded
     * as value=newValue.
     *
     * It is possible to have the editor functionality user protected. In the case that this is used and the
     * currently logged in user does not have any of the roles a read only text is rendered.
     *
     * In order to use this tag, you need to require the resource "editorSwitch".
     * <code>
     * <r:require module="editorSwitch"/>
     * </code>
     *
     * @attr link REQUIRED The link to update the data
     * @attr value REQUIRED The data to render
     * @attr roles optional comma delimited String of role names to check the users role against
     * @attr template optional The template to be used, if null the default one is taken
     */
    def editorSwitch = { attrs ->
        String template = editorSwitchTemplate(attrs.template)
        if (attrs.roles) {
            if (SpringSecurityUtils.ifAnyGranted(attrs.roles)) {
                out << render(template: template, model: [link: attrs.link, value: attrs.value])
            } else {
                // read only
                out << "<div>"
                out << attrs.value
                out << "</div>"
            }
        } else {
            // no roles passed in, just render
            out << render(template: template, model: [link: attrs.link, value: attrs.value])
        }
    }

    /**
     * Renders a button to open the ChangeLog for the ChangeLog data retrieved from provided JSON URL.
     * When the button is clicked an AJAX call is performed to the specified controller action and the
     * retrieved data is rendered in a table presented in a dialog.
     *
     * The expected JSON format the controller action has to be a list of elements with following key/value pairs:
     * <ul>
     * <li><strong>to</strong>: the new value</li>
     * <li><strong>from</strong>: the old value</li>
     * <li><strong>source</strong>: whether system or manual change</li>
     * <li><strong>comment</strong>: the comment of the change</li>
     * <li><strong>timestamp</strong>: the time stamp of the change</li>
     * </ul>
     *
     * The required JavaScript code is part of the resource "changeLog", so this one has to be required.
     * <code>
     * <r:require module="changeLog"/>
     * </code>
     *
     * @attr controller REQUIRED The controller where to fetch the changelog
     * @attr action REQUIRED The action where to fetch the changelog
     * @attr id REQUIRED The data id
     */
    def showChangeLog = { attrs ->
        out << render(template: "/templates/showChangeLog", model: [link: g.createLink(controller: attrs.controller, action: attrs.action, id: attrs.id)])
    }

    /**
     * Renders the enable/disable auto-refresh buttons.
     * Depending on the current state in the session either enable or disable is pre-selected.
     */
    def autoRefresh = {
        out << render(template: "/templates/autoRefresh", model: [enabled: Boolean.valueOf(session["auto-refresh"])])
    }

    /**
     * Renders either the configured environment name or the name of the
     * current Grails environment.
     *
     * This name can be used to have environment or instance specific CSS classes.
     */
    def environmentName = {
        if (!(grailsApplication.config.otp.environment.name instanceof ConfigObject)) {
            out << grailsApplication.config.otp.environment.name
        } else {
            out << Environment.getCurrent().name
        }
    }

    /**
     * Renders the markup for a datatable consisting of a header and footer section.
     *
     * @attr id REQUIRED The id for the datatable
     * @attr codes REQUIRED List of message codes to be used as table headers
     */
    def dataTable = { attrs ->
        out << render(template: "/templates/dataTable", model: [id: attrs.id, codes: attrs.codes])
    }

    /**
     * Renders the main information of SeqTrack to be included into several views.
     */
    def seqTrackMainPart = { attrs ->
        out << render(template: "/templates/seqTrackMainPart", model: [seqTrack: attrs.seqTrack])
    }

    private String editorSwitchTemplate(String template) {
        switch (template) {
            case "dropDown":
                return "/templates/editorSwitchDropDown"
            case "newValue":
                return "/templates/editorSwitchNewValue"
            case "newFreeTextValue":
                return "/templates/editorSwitchNewFreeTextValue"
            case "sampleIdentifier":
                return "/templates/editSampleIdentifiers"
            default:
                return "/templates/editorSwitch"
        }
    }
}
