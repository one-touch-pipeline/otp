<div id="loginBox">
    <div id="loginFormBox">
        <form id="loginForm" method="POST" action="${createLink(controller: 'j_spring_security_check')}">

            <label for="account">${g.message(code: "login.account")}</label>  <br/>
            <input name="j_username" id="account" value="${account}" required><br/>
            <label for="password">${g.message(code: "login.password")}</label><br/>
            <input name="j_password" id="password" type="password" required><br/>

            <div id="message-box"></div>

            <div class="loginButton isInvisible"><input type="submit" value="Login"/></div>
        </form>
    </div>

    <div class="loginButtonBox"></div>
</div>
