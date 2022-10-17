/*
 * Copyright 2011-2020 The OTP authors
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

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.qcTrafficLight.TableCellValue
import de.dkfz.tbi.otp.security.SecurityService

class OtpTagLib {
    static namespace = "otp"

    ConfigService configService
    ProcessingOptionService processingOptionService
    SecurityService securityService

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
     * <asset:javascript src="taglib/EditorSwitch.js"/>
     * </code>
     *
     * @attr link REQUIRED The link to update the data
     * @attr value REQUIRED The data to render
     * @attr roles optional comma delimited String of role names to check the users role against
     * @attr template optional The template to be used, if null the default one is taken
     */
    def editorSwitch = { attrs, body ->
        String template = editorSwitchTemplate(attrs.template)
        String roles = attrs.remove("roles")
        attrs.bodyContent = body
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
        out << configService.environmentName
    }

    /**
     * Returns an environment depending favicon
     */
    def favicon = {
        out << "<link rel=\"shortcut icon\" href=\"${g.assetPath(src: configService.pseudoEnvironment.ico)}\" type=\"image/x-icon\">"
    }

    /**
     * Renders the markup for a datatable consisting of a header and footer section.
     *
     * @attr id REQUIRED The id for the datatable
     * @attr codes REQUIRED List of message codes to be used as table headers
     */
    def dataTable = { attrs ->
        out << render(template: "/templates/dataTable", model: [id: attrs.id, codes: attrs.codes, classes: attrs.classes])
    }

    private static String editorSwitchTemplate(String template) {
        switch (template) {
            case "remove":
                return "/templates/editorSwitchRemove"
            case "dropDown":
                return "/templates/editorSwitchDropDown"
            case "dropDownMulti":
                return "/templates/editorSwitchDropDownMulti"
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
            case "integer":
                return "/templates/editorSwitchInteger"
            case "multiText":
                return "/templates/editorSwitchMultiText"
            case "multiDate":
                return "/templates/editorSwitchMultiDate"
            case "multiInteger":
                return "/templates/editorSwitchMultiInteger"
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
        if (!roles || (roles && securityService.ifAnyGranted(roles))) {
            model.checkBoxes = model.checkBoxes ?: [:]
            model.dropDowns = model.dropDowns ?: [:]

            return render(template: template, model: model)
        }
        return render(template: "/templates/readonly", model: model)
    }

    /**
     * Helper function to get the value from the model.
     */
    def getValueWithFallbacks = { attrs ->
        def value = attrs.remove("value")
        def values = attrs.remove("values")
        def optionKey = attrs.remove("optionKey")
        def optionValue = attrs.remove("optionValue")
        def noSelection = attrs.remove("noSelection")
        if (value != null) {
            def result = optionKey ? values.find { it[optionKey] == value } : value
            if (result != null) {
                out << (optionValue ? result[optionValue] : result)
            } else {
                out << getNoSelectionValueWithFallback(noSelection)
            }
        } else {
            out << getNoSelectionValueWithFallback(noSelection)
        }
    }

    /**
     * Helper function to mimic the way g:select extracts the value of the noSelection attribute
     */
    private String getNoSelectionValueWithFallback(Object noSelectionValue) {
        return noSelectionValue?.entrySet()?.iterator()?.next()?.value?.encodeAsHTML() ?: "-"
    }

    /**
     * Boostrap modal dialog
     *
     * Works on the bootstrap pages only.
     */
    def otpModal = { attrs, body ->
        String backdrop = attrs.closable == "true" ? "" : 'data-backdrop="static" data-keyboard="false"'
        String confirmText = attrs.confirmText ?: "Confirm"
        String closeText = attrs.closeText ?: "Close"

        out << "<div id='${attrs.modalId}' tabindex='-1' class='modal fade' role='dialog' ${backdrop}>"
        out << "<div class='modal-dialog'>"
        out << "<div class='modal-content'>"
        if (attrs.title || attrs.closable) {
            out << "<div class='modal-header'>"
            if (attrs.title) {
                out << "<h3 class='modal-title'>${attrs.title}</h3>"
            }
            if (attrs.closable == "true") {
                out << "<button class='close closeModal' onclick='${attrs.onClose}' type='button' data-dismiss='modal'>&times;</button>"
            }
            out << "</div>"
        }
        out << "<div class='modal-body'>"
        out << body.call()
        out << "</div>"
        if (attrs.type == "dialog") {
            out << "<div class='modal-footer'>"
            out << "<button type='button' class='btn btn-secondary closeModal' data-dismiss='modal' onclick='${attrs.onClose}'>${closeText}</button>"
            if (attrs.submit) {
                out << "<form id='modal-form' method='${attrs.submitMethod ?: "POST"}' action='${attrs.action}'>"
                if (attrs.hiddenInput) {
                    (attrs.hiddenInput as Map<String, String>).each {
                        out << "<input type='hidden' name='${it.key}' value='${it.value}' id='${it.key}'>"
                    }
                }
                out << "<button type='submit' class='btn btn-primary confirm'>${confirmText}</button>"
                out << "</form>"
            } else {
                out << "<button id='confirmModal' type='button' class='btn btn-primary confirm' data-dismiss='modal' onclick='${attrs.onConfirm}'>" +
                        "${confirmText}</button>"
            }
            out << "</div>"
        }
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
        out << g.submitButton(class: "save", name: "Save", style: "display: none;")
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

        String result = cell.value.encodeAsHTML()
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

    /**
     * Allows collapsing and expanding wrapped html via a button.
     *
     * To be used in conjunction with: taglib/Expandable.js
     *
     * @attrs
     * - value       : text to use for the button, defaults to 'expand'
     * - collapsed   : starting state when loading the page, defaults to being collapsed
     * - title       : pass-through for title attribute of button
     * - wrapperClass: css class to apply to the div containing the wrapped content
     */
    def expandable = { attrs, body ->
        String buttonText = attrs.value ?: "expand"
        boolean collapsed = attrs.collapsed.toString().toBoolean()
        String cssClass = collapsed ? "collapsed" : "expanded"

        out << "<button"
        out << "    class='expandable-button'"
        out << "    title='${attrs.title ?: g.message(code: 'default.expandable.title')}'"
        out << ">"
        out << buttonText
        out << "</button>"
        out << "<div class='expandable-container ${cssClass} ${attrs.wrapperClass}'>"
        out << body()
        out << "</div>"
    }

    /**
     * A simple wrapper for expandable text with show more/less collapsing
     *
     * To be used in conjunction with: taglib/ExpandableText.js
     *
     * @attr shortened REQUIRED the shortened version of the text to be expanded
     * @attr full REQUIRED the full text to be shown after expanding
     * @attr expandable REQUIRED should the expand-buttons be added
     */
    def expandableText = { attrs, body ->
        String shortened = attrs.shortened ?: ""
        String full = attrs.full ?: ""
        String expandable = attrs.expandable
        if (expandable.toBoolean()) {
            out << """\
            |<span>
            |<span class='expandable-shortened'>${shortened} <a class='expandable-more' href=''>[show more]</a></span>
            |<span class='expandable-full keep-whitespace' style='display: none'>${full} <a class='expandable-less' href='#'>[show less]</a></span>
            |</span>""".stripMargin()
        } else {
            out << full
        }
    }

    /**
     * A simple colored wrapper to highlight information.
     * @attr type REQUIRED one of info, warning, or error
     * @attr variant OPTIONAL switches the look of the element to match different contexts. Options: 'standalone' (the default), 'inline'.
     */
    def annotation = { attrs, body ->
        String type = attrs.remove("type")
        assert type: "attribute `type` must be given"
        String variant = attrs.remove("variant") ?: "standalone"

        out << "<div ${attrs.collect { "${it.key}=\"${it.value}\"" }.join(" ")} class=\"annotation-box type-${type} variant-${variant}\">"
        out << body()
        out << "</div>"
    }

    /**
     * Display an annotation stored in the GUI_OPTIONAL_ANNOTATION ProcessingOption.
     *
     * It retrieves the content of the given subtype of the GUI_OPTIONAL_ANNOTATION ProcessingOption and
     * uses the otp:annotation to display the it.
     *
     * The content is rendered as HTML.
     *
     * @attr option-type REQUIRED The type of the ProcessingOption, see enum GuiAnnotation
     * @attr type REQUIRED implicit requirement as it uses otp:annotation
     */
    def annotationPO = { attrs, body ->
        String optionType = attrs.remove("option-type")
        assert optionType: "attribute `option-type` must be given"
        assert !body: "the tag contains a body but does not use it"

        String content = raw(processingOptionService.findOptionAsString(ProcessingOption.OptionName.GUI_ANNOTATION, optionType))
        if (content) {
            out << otp.annotation(attrs, content).toString()
        }
    }
}
