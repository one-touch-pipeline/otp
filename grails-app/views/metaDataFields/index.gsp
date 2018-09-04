<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="layout" content="main" />
    <title><g:message code="dataFields.title"/></title>
    <asset:javascript src="modules/editorSwitch"/>
</head>


<body>
    <div class="body">
        <h3>
            <g:message code="dataFields.title.caseInsensitive" />
        </h3>
        <h3>
            <g:message code="dataFields.title.libraryPreparationKitTable" />
        </h3>
        <table>
            <thead>
                    <tr>
                        <th><g:message code="dataFields.libraryPreparationKit"/></th>
                        <th><g:message code="dataFields.libraryPreparationKit.shortDisplayName"/></th>
                        <th><g:message code="dataFields.libraryPreparationKit.importAlias"/></th>
                        <th></th>
                        <th><g:message code="dataFields.libraryPreparationKit.adapterFile"/></th>
                        <th><g:message code="dataFields.libraryPreparationKit.reverseComplementAdapterSequenceShort"/></th>
                        <th><g:message code="dataFields.libraryPreparationKit.genomes"/></th>
                    </tr>
            </thead>
            <tbody>
                <g:each var="libraryPreparationKit" in="${libraryPreparationKits}" >
                    <tr>
                        <td>
                            ${libraryPreparationKit.name}
                        </td>
                        <td>
                            ${libraryPreparationKit.shortDisplayName}

                        </td>
                        <td>
                            ${libraryPreparationKit.importAliases}
                        </td>
                        <td align="right">
                            <otp:editorSwitchNewValues
                                roles="ROLE_OPERATOR"
                                labels="${[g.message(code: "dataFields.libraryPreparationKit.importAlias")]}"
                                textFields="${["importAlias"]}"
                                link="${g.createLink(controller: 'metaDataFields', action: 'createLibraryPreparationKitImportAlias', id: libraryPreparationKit.name)}"
                            />
                        </td>
                        <td>
                            <g:if test="${libraryPreparationKit.adapterFile}">
                                <span title="${libraryPreparationKit.adapterFile}">${new File(libraryPreparationKit.adapterFile).name}</span>
                            </g:if>
                            <g:else>
                                <otp:editorSwitchNewValues
                                        roles="ROLE_OPERATOR"
                                        labels="${[g.message(code: "dataFields.libraryPreparationKit.adapterFile")]}"
                                        textFields="${["adapterFile"]}"
                                        link="${g.createLink(controller: 'metaDataFields', action: 'addAdapterFileToLibraryPreparationKit', params: ["libraryPreparationKit.id": libraryPreparationKit.id])}"
                                />
                            </g:else>
                        </td>
                        <td>
                            <g:if test="${libraryPreparationKit.reverseComplementAdapterSequence}">
                                <asset:image src="ok.png" title="${libraryPreparationKit.reverseComplementAdapterSequence}" />
                            </g:if>
                            <g:else>
                                <otp:editorSwitchNewValues
                                        roles="ROLE_OPERATOR"
                                        labels="${[g.message(code: "dataFields.libraryPreparationKit.reverseComplementAdapterSequence")]}"
                                        textFields="${["reverseComplementAdapterSequence"]}"
                                        link="${g.createLink(controller: 'metaDataFields', action: 'addAdapterSequenceToLibraryPreparationKit', params: ["libraryPreparationKit.id": libraryPreparationKit.id])}"
                                />
                            </g:else>
                        </td>
                        <td>
                            ${libraryPreparationKit.referenceGenomesWithBedFiles}
                        </td>
                    </tr>
                </g:each>
                <td colspan="3">
                    <otp:editorSwitchNewValues
                        roles="ROLE_OPERATOR"
                        labels="${[
                                g.message(code: "dataFields.libraryPreparationKit"),
                                g.message(code: "dataFields.libraryPreparationKit.shortDisplayName"),
                                g.message(code: "dataFields.libraryPreparationKit.adapterFile"),
                                g.message(code: "dataFields.libraryPreparationKit.reverseComplementAdapterSequence"),
                        ]}"
                        textFields="${["name", "shortDisplayName", "adapterFile", "reverseComplementAdapterSequence"]}"
                        link="${g.createLink(controller: 'metaDataFields', action: 'createLibraryPreparationKit')}"
                    />
                </td>
            </tbody>
        </table>
        <div style="width: 20px; height: 40px;"></div>
        <h3>
            <g:message code="dataFields.title.antibodyTargetTable" />
        </h3>
        <table>
            <thead>
                    <tr>
                        <th><g:message code="dataFields.listAntibodyTarget"/></th>
                        <th><g:message code="dataFields.importAlias"/></th>
                        <th></th>
                    </tr>
            </thead>
            <tbody>
                    <g:each var="antibodyTarget" in="${antibodyTargets}" >
                    <tr>
                        <td>
                            ${antibodyTarget.name}
                        </td>
                        <td>
                            ${antibodyTarget.importAliases}
                        </td>
                        <td>
                            <otp:editorSwitchNewValues
                                roles="ROLE_OPERATOR"
                                labels="${["Import Alias"]}"
                                textFields="${["importAlias"]}"
                                link="${g.createLink(controller: 'metaDataFields', action: 'createAntibodyTargetImportAlias', id: antibodyTarget.name)}"
                            />
                        </td>
                    </tr>
                </g:each>
                <td colspan="1">
                    <otp:editorSwitchNewValues
                        roles="ROLE_OPERATOR"
                        labels="${["Name"]}"
                        textFields="${["name"]}"
                        link="${g.createLink(controller: 'metaDataFields', action: 'createAntibodyTarget')}"
                    />
                </td>
            </tbody>
        </table>
        <div style="width: 20px; height: 40px;"></div>
        <h3>
            <g:message code="dataFields.title.centersTable" />
        </h3>
        <table>
            <thead>
                    <tr>
                        <th><g:message code="dataFields.listSeqCenterName"/></th>
                        <th><g:message code="dataFields.listSeqCenterDirName"/></th>
                    </tr>
            </thead>
            <tbody>
                <g:each var="seqCenter" in="${seqCenters}" >
                    <tr>
                        <td>
                            ${seqCenter.name}
                        </td>
                        <td>
                            ${seqCenter.dirName}
                        </td>
                    </tr>
                </g:each>
                <td colspan="2">
                    <otp:editorSwitchNewValues
                        roles="ROLE_OPERATOR"
                        labels="${["Name", "Directory"]}"
                        textFields="${["name", "dirName"]}"
                        link="${g.createLink(controller: 'metaDataFields', action: 'createSeqCenter')}"
                    />
                </td>
            </tbody>
        </table>
        <div style="width: 20px; height: 40px;"></div>
        <h3>
            <g:message code="dataFields.title.listPlatformNameTable" />
        </h3>
        <table>
            <thead>
                    <tr>
                        <th><g:message code="dataFields.listPlatformName"/></th>
                        <th><g:message code="dataFields.listPlatformModelLabel"/></th>
                        <th><g:message code="dataFields.listPlatformModelLabelImportAlias"/></th>
                        <th></th>
                        <th><g:message code="dataFields.listSequeningKitLabel"/></th>
                        <th><g:message code="dataFields.listSequeningKitLabelImportAlias"/></th>
                        <th></th>
                    </tr>
            </thead>
            <tbody>
                    <g:each var="seqPlatform" in="${seqPlatforms}" >
                    <tr>
                        <td>
                            ${seqPlatform.name}
                        </td>
                        <td>
                            ${seqPlatform.model}
                        </td>
                        <td>
                            ${seqPlatform.modelImportAliases}
                        </td>
                        <td>
                        <g:if  test="${seqPlatform.hasModel}">
                            <otp:editorSwitchNewValues
                                roles="ROLE_OPERATOR"
                                labels="${["Import Alias"]}"
                                textFields="${["importAlias"]}"
                                link="${g.createLink(controller: 'metaDataFields', action: 'createModelImportAlias', id: seqPlatform.model)}"
                            />
                        </g:if>
                        </td>
                        <td>
                            ${seqPlatform.seqKit}
                        </td>
                        <td>
                            ${seqPlatform.seqKitImportAliases}
                        </td>
                        <td>
                        <g:if  test="${seqPlatform.hasSeqKit}">
                            <otp:editorSwitchNewValues
                                roles="ROLE_OPERATOR"
                                labels="${["Import Alias"]}"
                                textFields="${["importAlias"]}"
                                link="${g.createLink(controller: 'metaDataFields', action: 'createSequencingKitImportAlias', id: seqPlatform.seqKit)}"
                            />
                        </g:if>
                        </td>
                    </tr>
                </g:each>
                <td colspan="8">
                    <otp:editorSwitchNewValues
                        roles="ROLE_OPERATOR"
                        labels="${["Platform", "Model", "Kit"]}"
                        textFields="${["platform", "model", "kit"]}"
                        link="${g.createLink(controller: 'metaDataFields', action: 'createSeqPlatform')}"
                    />
                </td>
            </tbody>
        </table>
        <div style="width: 20px; height: 40px;"></div>
        <h3>
            <g:message code="dataFields.titlelistSeqTypeTable" />
        </h3>
        <table>
            <thead>
                    <tr>
                        <th><g:message code="dataFields.listSeqType"/></th>
                        <th><g:message code="dataFields.listSingleCell"/></th>
                        <th><g:message code="dataFields.listSeqTypeDir"/></th>
                        <th><g:message code="dataFields.listSeqTypeLayouts"/></th>
                        <th></th>
                        <th><g:message code="dataFields.listSeqTypeDisplayNames"/></th>
                        <th><g:message code="dataFields.listSeqTypeImportAlias"/></th>
                        <th></th>
                    </tr>
            </thead>
            <tbody>
                    <g:each var="seqType" in="${seqTypes}" >
                    <tr>
                        <td>
                            ${seqType.name}
                        </td>
                        <td>
                            ${seqType.singleCell}
                        </td>
                        <td>
                            ${seqType.dirName}
                        </td>
                        <td>
                            ${seqType.libraryLayouts}
                        </td>
                        <td>
                            <g:if test="${!(seqType.layouts.SINGLE && seqType.layouts.PAIRED && seqType.layouts.MATE_PAIR)}">
                            <otp:editorSwitchNewValues
                                roles="ROLE_OPERATOR"
                                labels="${seqType.layouts.findAll {!it.value}.collect {it.key}}"
                                checkBoxes="${seqType.layouts.findAll {!it.value}.collectEntries { [it.key.toLowerCase(), it.value] }}"
                                link="${g.createLink(controller: 'metaDataFields', action: 'createLayout', id: seqType.name,  params: ["singleCell": seqType.singleCell])}"
                            />
                            </g:if>
                        </td>
                        <td>
                            ${seqType.displayName}
                        </td>
                        <td>
                            ${seqType.importAliases}
                        </td>
                        <td>
                            <otp:editorSwitchNewValues
                                roles="ROLE_OPERATOR"
                                labels="${["Import Alias"]}"
                                textFields="${["importAlias"]}"
                                link="${g.createLink(controller: 'metaDataFields', action: 'createSeqTypeImportAlias', id: seqType.name)}"
                            />
                        </td>
                    </tr>
                </g:each>
                <td colspan="3">
                    <div class="edit-switch edit-switch-new-free-text-values">
                        <span class="edit-switch-editor" style="display: none">
                            <h4>
                                <g:message code="dataFields.seqType.addSeqTypeTitle" />
                            </h4>
                            <input type="hidden" name="target" value="${g.createLink(controller: 'metaDataFields', action: 'createSeqType')}"/>
                            <div class="dialog">
                                <table>
                                    <tbody>
                                    <tr class="prop">
                                        <td valign="top" class="name">
                                            <label for="type">
                                                <g:message code="dataFields.seqType.type"/>
                                            </label>
                                        </td>
                                        <td valign="top" class="value">
                                            <input name="type" id="type" type="text"/>
                                        </td>
                                        <td>
                                        </td>
                                    </tr>
                                    <tr class="prop">
                                        <td valign="top" class="name">
                                            <label for="directory">
                                                <g:message code="dataFields.seqType.directory"/>
                                            </label>
                                        </td>
                                        <td valign="top" class="value">
                                            <input name="dirName" id="directory" type="text"/>
                                        </td>
                                        <td>
                                        </td>
                                    </tr>
                                    <tr class="prop">
                                        <td valign="top" class="name">
                                            <label for="name">
                                                <g:message code="dataFields.seqType.name"/>
                                            </label>
                                        </td>
                                        <td valign="top" class="value">
                                            <input name="displayName" id="name" type="text"/>
                                        </td>
                                        <td>
                                        </td>
                                    </tr>
                                    <tr class="prop">
                                        <td colspan="1" valign="top" class="name">
                                            <label for="singleCellYes">
                                                <g:message code="dataFields.seqType.singleCell"/>
                                            </label>
                                        </td>
                                        <td colspan="1" valign="top" class="name">
                                            <input name="singleCell" id="singleCell" type="checkbox"/>
                                        </td>
                                    </tr>
                                    <tr class="prop">
                                        <td colspan="1" valign="top" class="name">
                                            <label for="single">
                                                <g:message code="dataFields.seqType.layout"/>
                                            </label>
                                        </td>
                                        <td colspan="1" valign="top" class="name">
                                            <label for="single">
                                                <g:message code="dataFields.seqType.single"/>
                                            </label>
                                            <input name="single" id="single" type="checkbox"/>
                                        </td>
                                    </tr>
                                    <tr class="prop">
                                        <td colspan="1">&nbsp;</td>
                                        <td colspan="1" valign="top" class="name">
                                            <label for="paired">
                                                <g:message code="dataFields.seqType.paired"/>
                                            </label>
                                            <input name="paired" id="paired" checked="checked" type="checkbox"/>
                                        </td>
                                    </tr>
                                    <tr class="prop">
                                        <td colspan="1">&nbsp;</td>
                                        <td colspan="1" valign="top" class="value">
                                            <label for="mate_pair">
                                                <g:message code="dataFields.seqType.mate"/>
                                            </label>
                                            <input name="type" id="mate_pair" type="checkbox"/>
                                        </td>
                                    </tr>
                                    </tbody>
                                </table>
                            </div>
                            <button class="buttons save"><g:message code="default.button.save.label"/></button>
                            <button class="buttons cancel"><g:message code="default.button.cancel.label"/></button>
                        </span>
                        <span class="edit-switch-label" style="display: inline;"><button class="add js-edit">+</button>
                        </span>
                    </div>
                </td>
            </tbody>
        </table>
    </div>
</body>
</html>
