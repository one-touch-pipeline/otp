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
}
