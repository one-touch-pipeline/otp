<div id="loginBox" style="position: absolute;">
    <div id="loginFormBox" style="position: absolute;">
        <form id="loginForm" method="POST" action="${createLink(controller: 'j_spring_security_check')}">

            <input name="j_username" id="account" placeholder="${g.message(code: "login.account")}" value="${account}" required>
            <input type="password" name="j_password" id="password" placeholder="${g.message(code: "login.password")}" required><br/>

            <input id="loginButton" type="submit" value="Login"/>
        </form>
    </div>
</div>
