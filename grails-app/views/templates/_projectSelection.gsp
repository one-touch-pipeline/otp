<form class="blue_label" id="projectsGroupbox" action="${g.createLink(controller: 'projectSelection', action: 'select')}">
    <div style="color: black"><strong>&nbsp;${message(code: "home.projectfilter.project")} : ${project.name}</strong></div>
    <label style="color: black" for="project">&nbsp;${message(code: "home.projectfilter.select")} : </label>
    <g:hiddenField name="displayName" value=""/>
    <g:hiddenField name="type" value="PROJECT"/>
    <g:hiddenField name="redirect" value="${request.forwardURI - request.contextPath}"/>
    <g:select class="criteria" id="project" name='id'
              from='${projects}' value='${project.id}' optionKey='id' optionValue='name' onChange='submit();' />
</form>
