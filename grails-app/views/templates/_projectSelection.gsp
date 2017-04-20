<form class="blue_label" id="projectsGroupbox" action="${g.createLink(controller: 'projectSelection', action: 'select')}">
    <label style="color: black" for="project">${message(code: "home.projectfilter")} :</label>
    <g:hiddenField name="displayName" value=""/>
    <g:hiddenField name="type" value="PROJECT"/>
    <g:hiddenField name="redirect" value="${request.forwardURI - request.contextPath}"/>
    <g:select class="criteria" id="project" name='id'
              from='${projects}' value='${project.id}' optionKey='id' optionValue='name' onChange='submit();' />
</form>
