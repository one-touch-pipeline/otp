/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.dkfz.tbi.otp

import grails.plugin.springsecurity.SpringSecurityUtils

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.qcTrafficLight.TableCellValue

class OtpTagLib {
    static namespace = "otp"

    ConfigService configService

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
     * <asset:javascript src="modules/editorSwitch"/>
     * </code>
     *
     * @attr link REQUIRED The link to update the data
     * @attr value REQUIRED The data to render
     * @attr roles optional comma delimited String of role names to check the users role against
     * @attr template optional The template to be used, if null the default one is taken
     */
    def editorSwitch = { attrs ->
        String template = editorSwitchTemplate(attrs.template)
        String roles = attrs.remove("roles")
        out << editorSwitchRender(roles, template, attrs)
    }

    /**
     * @attr selectedValues REQUIRED
     * @attr availableValues REQUIRED
     * @attr link REQUIRED
     * @attr roles optional
     */
    def editorSwitchCheckboxes = { Map attrs ->
        String template = "/templates/editorSwitchCheckboxes"
        String roles = attrs.remove("roles")
        attrs.put("value", attrs.selectedValues.join(", "))
        out << editorSwitchRender(roles, template, attrs)
    }

    /**
     * @attr labels REQUIRED
     * @attr textFields optional
     * @attr checkBoxes optional
     * @attr dropDowns optional
     * @attr link REQUIRED
     * @attr roles optional
     */
    def editorSwitchNewValues = { Map attrs ->
        String template = "/templates/editorSwitchNewFreeTextValues"
        String roles = attrs.remove("roles")
        out << editorSwitchRender(roles, template, attrs)
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
     * <asset:javascript src="modules/changeLog"/>
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
        out << configService.getEnvironmentName()
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

    private static String editorSwitchTemplate(String template) {
        switch (template) {
            case "dropDown":
                return "/templates/editorSwitchDropDown"
            case "urlValue":
                return "/templates/editorSwitchUrl"
            case "newValue":
                return "/templates/editorSwitchNewValue"
            case "newFreeTextValue":
                return "/templates/editorSwitchNewFreeTextValue"
            case "sampleIdentifier":
                return "/templates/editSampleIdentifiers"
            case "textArea":
                return "/templates/editorSwitchTextArea"
            case "toggle":
                return "/templates/editorSwitchToggle"
            case "date":
                return "/templates/editorSwitchDate"
            default:
                return "/templates/editorSwitch"
        }
    }

    /**
     * @param template name of the template to use
     * @param roles required roles as a comma-separated list
     * @param model values used in the template
     * @return html to show
     */
    private String editorSwitchRender(String roles, String template, Map model) {
        if (!roles || (roles && SpringSecurityUtils.ifAnyGranted(roles))) {
            model.checkBoxes = model.checkBoxes ?: [:]
            model.dropDowns = model.dropDowns ?: [:]

            return render(template: template, model: model)
        } else {
            // read only
            return "<div><span class='wordBreak keep-whitespace'>${model.value != null ? model.value : ""}</span></div>"
        }
    }

    def modal = { attrs, body ->
        out << "<div id='${attrs.modalId}' class='modal fade' role='dialog'>"
        out << "<div class='modal-dialog'>"
        out << "<div class='modal-content'>"
        out << "<div class='modal-header'>"
        out << "<button type='button' class='close' data-dismiss='modal'>&times;</button>"
        out << "<h4 class='modal-title'>${attrs.title}</h4>"
        out << "</div>"
        out << "<div class='modal-body'>"
        out << body.call()
        out << "</div>"
        out << "<div class='modal-footer'>"
        out << "<button type='button' class='btn btn-primary' data-dismiss='modal'>Close</button>"
        out << "</div>"
        out << "</div>"
        out << "</div>"
        out << "</div>"
    }


    def tableAdd = { attrs, body ->
        out << '<tr class="add-table-fields" style="display: none;">'
        out << body.call()
        out << '</tr>'
        out << '<tr>'
        out << '  <td class="add-table-buttons" colspan="100">'
        out << '    <button class="add">+</button>'
        out <<      g.submitButton(class: "save", name: "Save", style: "display: none;")
        out << '    <button class="cancel" style="display: none;">Cancel</button>'
        out << '  </td>'
        out << '</tr>'
    }

    def editTable = { attrs, body ->
        out << '<tr class="edit-table-buttons">'
        out << body.call("insideEdiTable": true)
        out << '</tr>'

    }

    def editTableButtons = { attrs ->
       out << '<button class="button-edit">Edit</button>'
       out << g.submitButton(class: "save", style: "display: none;", name: "Save")
       out << '<button class="cancel" style="display: none;">Cancel</button>'
    }


    def tableCell = { attrs ->
        TableCellValue cell = attrs.cell

        String result = cell.value
        if (cell.icon) {
            result = "<span class='icon-${cell.icon}'>${result}</span>"
        }
        if (cell.warnColor) {
            result = "<span class='text-${cell.warnColor}'>${result}</span>"
        }
        if (cell.link) {
            result = "<a href='${cell.link}'>${result}</a>"
        }
        out << "<span title='${cell.tooltip}'>${result}</span>"
    }
}
