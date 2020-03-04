%{--
  - Copyright 2011-2019 The OTP authors
  -
  - Permission is hereby granted, free of charge, to any person obtaining a copy
  - of this software and associated documentation files (the "Software"), to deal
  - in the Software without restriction, including without limitation the rights
  - to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  - copies of the Software, and to permit persons to whom the Software is
  - furnished to do so, subject to the following conditions:
  -
  - The above copyright notice and this permission notice shall be included in all
  - copies or substantial portions of the Software.
  -
  - THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  - IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  - FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  - AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  - LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  - OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  - SOFTWARE.
  --}%

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="metadataLayout" />
    <title><g:message code="otp.menu.sampleSwap"/></title>
    <asset:javascript src="modules/defaultPageDependencies.js"/>
</head>
<body>
<div class="body">
    <ul>
    <g:each var="body" in="${data}" >
        <g:each var="message" in="${body.sampleSwapInfos}" >
            <g:if test="${message.description!=''}"><li class="${message.level}"><span style="white-space: pre-line "> ${message.description}</span></li> </g:if>
        </g:each>
    </g:each>
    </ul>
    <div class="searchCriteriaTableSequences">
        <g:render template="/templates/projectSelection"/>

        <form class="rounded-page-header-box" action="${g.createLink(controller: 'sampleSwap', action: 'index')}">
            <div style="color: black"><strong>&nbsp;Individual : ${individual?.pid}</strong></div>
            <label style="color: black" for="project">&nbsp;${message(code: "home.projectFilter.select")} : </label>
            <g:select class="criteria" id="individual" name='individual'
                    from='${individuals}' value='${individual?.id}' optionKey='id' optionValue='pid' onChange='submit();' />
        </form>
    </div>
    <g:form>
    <div class="sampleSwapCommentBoxContainer">
        <div>Comment:</div>
        <g:textArea value="${comment}" placeholder="This must be filled out..." name="comment" rows="4" cols="125" />
    </div>
    <div>
        <table id="sampleSwap">
            <tr>
                <g:each var="header" in="${[
                    '',
                    'Project',
                    'Pid',
                    'SampleType',
                    'SeqType',
                    'LibPrepKit',
                    'AntibodyTarget',
                    'Antibody',
                    'LibraryLayout',
                    'Run',
                    'Lane',
                    'Ilse',
                ]}">
                    <th>${header}</th>
                </g:each>
                <g:if test="${numberOfFiles!=0}">
                    <g:each var="number" in="${1..numberOfFiles}" >
                        <th>Datafile ${number}</th>
                    </g:each>
                </g:if>
            </tr>
            <g:each var="body" in="${data}">
                <g:if test="${body.oldValues}">
                    <tr>
                        <td align="center"><button type=button name="${body.seqTrackId}">Edit</button></td>
                        <g:each var="value" in="${body.oldValues}">
                            <g:if test="${value.key == 'files'}">
                                <g:each var="file" in="${value.value}">
                                    <td>${file.value}</td>
                                </g:each>
                            </g:if>
                            <g:else>
                                <td>${value.value}</td>
                            </g:else>
                        </g:each>
                    </tr>
                    <tr id="${body.seqTrackId}" style="${!body.sampleSwapInfos?'display:none;':''}">
                    <td align="center">${body.rowNumber}<input type='hidden' name='seqTrackId' value='${body.seqTrackId}' readonly/></td>
                        <g:each var="value" in="${body.newValues}">
                            <g:if test="${value.key == 'files'}">
                                <g:each var="file" in="${value.value}">
                                    <td><input class="${body.getNormalizedLevel(value.key, file.value)}" name="${body.seqTrackId}!${value.key}!${file.key}" value="${file.value}" /></td>
                                </g:each>
                            </g:if>
                            <g:elseif test="${value.key == 'seqTrackId'}">
                                <td></td>
                            </g:elseif>
                            <g:elseif test="${value.key == 'project'}">
                                <td><g:select class="${body.getNormalizedLevel(value.key)}" name="${body.seqTrackId}!${value.key}" from="${availableProjects.name}" value="${value.value}" /></td>
                            </g:elseif>
                            <g:elseif test="${value.key == 'sampleType'}">
                                <td><g:select class="${body.getNormalizedLevel(value.key)}" name="${body.seqTrackId}!${value.key}" from="${sampleTypes}" value="${value.value}" /></td>
                            </g:elseif>
                            <g:elseif test="${value.key == 'seqType'}">
                                <td><g:select class="${body.getNormalizedLevel(value.key)}" name="${body.seqTrackId}!${value.key}" from="${seqTypes}" value="${value.value}" /></td>
                            </g:elseif>
                            <g:elseif test="${value.key == 'libPrepKit'}">
                                <td><g:select class="${body.getNormalizedLevel(value.key)}" name="${body.seqTrackId}!${value.key}" from="${libPrepKits}" value="${value.value}" /></td>
                            </g:elseif>
                            <g:elseif test="${value.key == 'libraryLayout'}">
                                <td><g:select class="${body.getNormalizedLevel(value.key)}" name="${body.seqTrackId}!${value.key}" from="${libraryLayouts}" value="${value.value}" /></td>
                            </g:elseif>
                            <g:elseif test="${value.key == 'antibodyTarget'}">
                                <td><g:select class="${body.getNormalizedLevel(value.key)}" name="${body.seqTrackId}!${value.key}" from="${antibodyTargets}" value="${value.value}" /></td>
                            </g:elseif>
                            <g:elseif test="${value.key == 'run' || value.key == 'lane' || value.key == 'ilse'}">
                                <td><input class="${body.getNormalizedLevel(value.key)}" name="${body.seqTrackId}!${value.key}" value="${value.value}"  readonly/></td>
                            </g:elseif>
                            <g:else>
                                <td><input class="${body.getNormalizedLevel(value.key)}" name="${body.seqTrackId}!${value.key}" value="${value.value}" /></td>
                            </g:else>
                        </g:each>
                    </tr>
                </g:if>
            </g:each>
        </table>
        <input type='hidden' name='individual' value='${individual?.id}' />
        <table class="options">
            <td>Hold Processing</td>
            <td><g:checkBox name="holdProcessing" checked="${holdProcessing}" value="true"/></td>
        </table>
        <table class="options">
            <td><g:submitButton name="submit" value="Cancel" /></td>
            <td><g:submitButton name="submit" value="Reset" /></td>
            <td><g:submitButton name="submit" value="Submit" /></td>
        </table>
    </div>
    </g:form>

</div>
<asset:script>
    $(function() {
        if (!${chipSeq}) {
            $('#sampleSwap tr > *:nth-child(7)').hide();
            $('#sampleSwap tr > *:nth-child(8)').hide();
        }
        $("button").click(function(e){
            $("#"+e.target.name).toggle();
         });
        $('select').change(function() {
            if ($(this).find('option:selected').val() == 'ChIP') {
                $('#sampleSwap tr > *:nth-child(7)').show();
                $('#sampleSwap tr > *:nth-child(8)').show();
        }});
    });
</asset:script>
</body>
</html>
