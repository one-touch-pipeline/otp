package de.dkfz.tbi.otp

import org.codehaus.groovy.grails.plugins.springsecurity.SpringSecurityUtils

class OtpTagLib {
    static namespace = "otp"

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
     */
    def editorSwitch = { attrs ->
        if (attrs.roles) {
            if (SpringSecurityUtils.ifAnyGranted(attrs.roles)) {
                out << render(template: "/templates/editorSwitch", model: [link: attrs.link, value: attrs.value])
            } else {
                // read only
                out << "<div>"
                out << attrs.value
                out << "</div>"
            }
        } else {
            // no roles passed in, just render
            out << render(template: "/templates/editorSwitch", model: [link: attrs.link, value: attrs.value])
        }
    }
}
