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
            <g:message code="dataFields.title.libraryPreparationKitTable" />
        </h3>
        <table>
            <thead>
                    <tr>
                        <th><g:message code="dataFields.libraryPreparationKit"/></th>
                        <th><g:message code="dataFields.libraryPreparationKit.shortDisplayName"/></th>
                        <th><g:message code="dataFields.libraryPreparationKit.alias"/></th>
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
                            ${libraryPreparationKit.alias}
                        </td>
                        <td align="right">
                            <otp:editorSwitchNewValues
                                roles="ROLE_OPERATOR"
                                labels="${[g.message(code: "dataFields.libraryPreparationKit.alias")]}"
                                textFields="${["alias"]}"
                                link="${g.createLink(controller: 'metaDataFields', action: 'createLibraryPreparationKitAlias', id: libraryPreparationKit.name)}"
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
                    </tr>
            </thead>
            <tbody>
                    <g:each var="antiBodyTarget" in="${antiBodyTargets}" >
                    <tr>
                        <td>
                            ${antiBodyTarget.name}
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
                        <th><g:message code="dataFields.listPlatformModelLabelAlias"/></th>
                        <th></th>
                        <th><g:message code="dataFields.listSequeningKitLabel"/></th>
                        <th><g:message code="dataFields.listSequeningKitLabelAlias"/></th>
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
                            ${seqPlatform.modelAlias}
                        </td>
                        <td>
                        <g:if  test="${seqPlatform.hasModel}">
                            <otp:editorSwitchNewValues
                                roles="ROLE_OPERATOR"
                                labels="${["Alias"]}"
                                textFields="${["alias"]}"
                                link="${g.createLink(controller: 'metaDataFields', action: 'createModelAlias', id: seqPlatform.model)}"
                            />
                        </g:if>
                        </td>
                        <td>
                            ${seqPlatform.seqKit}
                        </td>
                        <td>
                            ${seqPlatform.seqKitAlias}
                        </td>
                        <td>
                        <g:if  test="${seqPlatform.hasSeqKit}">
                            <otp:editorSwitchNewValues
                                roles="ROLE_OPERATOR"
                                labels="${["Alias"]}"
                                textFields="${["alias"]}"
                                link="${g.createLink(controller: 'metaDataFields', action: 'createSequencingKitAlias', id: seqPlatform.seqKit)}"
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
                        <th><g:message code="dataFields.listSeqTypeDir"/></th>
                        <th><g:message code="dataFields.listSeqTypeLayouts"/></th>
                        <th></th>
                        <th><g:message code="dataFields.listSeqTypeAlias"/></th>
                    </tr>
            </thead>
            <tbody>
                    <g:each var="seqType" in="${seqTypes}" >
                    <tr>
                        <td>
                            ${seqType.name}
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
                                link="${g.createLink(controller: 'metaDataFields', action: 'createLayout', id: seqType.name)}"
                            />
                            </g:if>

                        </td>
                        <td>
                            ${seqType.displayName}
                        </td>
                    </tr>
                </g:each>
                <td colspan="4">
                    <otp:editorSwitchNewValues
                        roles="ROLE_OPERATOR"
                        labels="${["Type", "Directory", "Display Name", "SINGLE", "PAIRED", "MATE_PAIR"]}"
                        textFields="${["type", "dirName", "displayName"]}"
                        checkBoxes="${[single: false, paired: true, mate_pair: false]}"
                        link="${g.createLink(controller: 'metaDataFields', action: 'createSeqType')}"
                    />
                </td>
            </tbody>
        </table>
    </div>
</body>
</html>
