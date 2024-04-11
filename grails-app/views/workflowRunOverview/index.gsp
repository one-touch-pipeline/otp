%{--
  - Copyright 2011-2024 The OTP authors
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
<%@ page import="grails.util.Pair" %>

<html>
<head>
    <title>${g.message(code: "workflowRun.overview.title")}</title>
    <asset:javascript src="pages/workflowRunOverview/index.js"/>
</head>

<body>
<div class="container-fluid otp-main-container">
    <g:render template="/templates/quickNavigationBar" model="[
            linkText : g.message(code: 'workflowSystem.configPage.link'),
            link : g.createLink(controller: 'workflowSystemConfig', action: 'index'),
            tooltip : g.message(code: 'workflowSystem.configPage.tooltip'),
    ]"/>

    <h1>${g.message(code: "workflowRun.overview.title")}</h1>
    <button class="btn btn-primary toggleButton">${g.message(code: "workflowRun.overview.show.detailed")}</button>
    <button class="btn btn-primary toggleButton" style="display: none">${g.message(code: "workflowRun.overview.show.condensed")}</button>
    <table id="runs" class="w-100 table table-sm table-striped table-hover">
        <thead>
        <tr>
            <th></th>
            <th></th>
            <th></th>
            <g:each in="${states}" var="state">
                <th></th>
                <th colspan="${state.value.size()}">${state.key}</th>
            </g:each>
            <th></th>
            <th></th>
        </tr>
        <tr>
            <th></th>
            <th>${g.message(code: "workflowRun.overview.workflow")}</th>
            <th>${g.message(code: "workflowRun.overview.all")}</th>
            <g:each in="${states}" var="stateAndSubStates">
                <th>${stateAndSubStates.key}</th>
                <g:each in="${stateAndSubStates.value}" var="subState">
                    <th title="${subState.description}">${subState}</th>
                </g:each>
            </g:each>
            <th>${g.message(code: "workflowRun.overview.lastRun")}</th>
            <th>${g.message(code: "workflowRun.overview.lastFail")}</th>
        </tr>
        </thead>

        <tbody>
        <g:each in="${workflows}" var="workflow">
            <tr>
                <td><div class="${workflow.enabled ? "dot green" : "dot grey"} small" title="${workflow.enabled ? "Enabled" : "Disabled"}"></div></td>
                <td><g:link controller="workflowRunList" action="index" params="${["workflow.id": workflow.id]}">${workflow}</g:link></td>
                <td><g:link controller="workflowRunList" action="index" params="${["workflow.id": workflow.id]}">
                    ${states.collect { state -> state.value.sum { runs[new Pair(it, workflow)] ?: 0 } }.sum()}
                </g:link></td>
                <g:each in="${states}" var="stateAndSubStates">
                    <td><g:link controller="workflowRunList" action="index" params="${["workflow.id": workflow.id, state: stateAndSubStates.value]}">
                        ${stateAndSubStates.value.sum { runs[new Pair(it, workflow)] ?: 0 }}
                    </g:link></td>
                    <g:each in="${stateAndSubStates.value}" var="subState">
                        <td><g:link controller="workflowRunList" action="index"
                                    params="${["workflow.id": workflow.id, state: subState]}">${runs[new Pair(subState, workflow)] ?: "0"}</g:link></td>
                    </g:each>
                </g:each>
                <td>${lastRuns[workflow]}</td>
                <td>${lastFails[workflow]}</td>
            </tr>
        </g:each>
        </tbody>
    </table>
</div>
</body>
</html>
