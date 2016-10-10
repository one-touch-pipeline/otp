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
                            <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                template="newFreeTextValues"
                                fields="${["Alias"]}"
                                link="${g.createLink(controller: 'metaDataFields', action: 'createLibraryPreparationKitAlias', id: libraryPreparationKit.name)}"
                                value=""/>
                        </td>
                        <td>
                            ${libraryPreparationKit.referenceGenomesWithBedFiles}
                        </td>
                    </tr>
                </g:each>
                <td colspan="3">
                    <otp:editorSwitch
                        roles="ROLE_OPERATOR"
                        fields="${["Name", "Short Display Name"]}"
                        template="newFreeTextValues"
                        link="${g.createLink(controller: 'metaDataFields', action: 'createLibraryPreparationKit')}"
                        value=""/>
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
                    <otp:editorSwitch
                        roles="ROLE_OPERATOR"
                        template="newFreeTextValues"
                        fields="${["Name"]}"
                        link="${g.createLink(controller: 'metaDataFields', action: 'createAntibodyTarget')}"
                        value=""/>
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
                    <otp:editorSwitch
                        roles="ROLE_OPERATOR"
                        template="newFreeTextValues"
                        fields="${["Name", "Directory"]}"
                        link="${g.createLink(controller: 'metaDataFields', action: 'createSeqCenter')}"
                        value=""/>
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
                        <th><g:message code="dataFields.listPlatformGroup"/></th>
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
                            ${seqPlatform.platformGroup}
                        </td>
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
                            <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                template="newFreeTextValues"
                                fields="${["Alias"]}"
                                link="${g.createLink(controller: 'metaDataFields', action: 'createModelAlias', id: seqPlatform.model)}"
                                value=""/>
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
                            <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                template="newFreeTextValues"
                                fields="${["Alias"]}"
                                link="${g.createLink(controller: 'metaDataFields', action: 'createSequencingKitAlias', id: seqPlatform.seqKit)}"
                                value=""/>
                        </g:if>
                        </td>
                    </tr>
                </g:each>
                <td colspan="8">
                    <otp:editorSwitch
                        roles="ROLE_OPERATOR"
                        template="newFreeTextValues"
                        fields="${["Group", "Platform", "Model", "Kit"]}"
                        link="${g.createLink(controller: 'metaDataFields', action: 'createSeqPlatform')}"
                        value=""/>
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
                            <g:if  test="${!(seqType.layouts.SINGLE&&seqType.layouts.PAIRED&&seqType.layouts.MATE_PAIR)}">
                            <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                template="newFreeTextValues"
                                check="true"
                                checkBoxes="${seqType.layouts}"
                                link="${g.createLink(controller: 'metaDataFields', action: 'createLayout', id: seqType.name)}"
                                value=""/>
                            </g:if>

                        </td>
                        <td>
                            ${seqType.alias}
                        </td>
                    </tr>
                </g:each>
                <td colspan="4">
                    <otp:editorSwitch
                        roles="ROLE_OPERATOR"
                        template="newFreeTextValues"
                        fields="${["Type","Directory","Alias"]}"
                        check="true"
                        checkBoxes="${[SINGLE: false,PAIRED: false,MATE_PAIR: false]}"
                        link="${g.createLink(controller: 'metaDataFields', action: 'createSeqType')}"
                        value=""/>
                </td>
            </tbody>
        </table>
        <div style="width: 20px; height: 40px;"></div>
        <h3>
            <g:message code="dataFields.title.adapter" />
        </h3>
        <table>
            <thead>
            <tr>
                <th><g:message code="dataFields.adapter"/></th>
            </tr>
            </thead>
            <tbody>
            <g:each var="adapterFile" in="${adapterFiles}" >
                <tr>
                    <td>
                        ${adapterFile.fileName}
                    </td>
                </tr>
            </g:each>
            <td colspan="1">
                <otp:editorSwitch
                        roles="ROLE_OPERATOR"
                        template="newFreeTextValue"
                        label="Adapter file:"
                        link="${g.createLink(controller: 'metaDataFields', action: 'createAdapterFile')}"
                        value=""/>
            </td>
            </tbody>
        </table>
    </div>
</body>
</html>
