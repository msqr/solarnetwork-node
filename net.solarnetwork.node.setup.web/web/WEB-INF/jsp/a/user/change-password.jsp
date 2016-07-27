<div class="hidden alert alert-success" id="change-password-success">
	<fmt:message key='user.changePassword.success'/>
</div>

<section class="intro">
	<p>
		<fmt:message key="user.changePassword.intro"/>
	</p>
</section>

<form:form commandName="user" cssClass="form-horizontal" id="change-password-form" action="/a/user/change-password" method="post" >
	<fieldset>
		<div class="control-group">
			<label class="control-label" for="old-password"><fmt:message key="user.oldPassword.label"/></label>
			<div class="controls">
				<form:password path="oldPassword" showPassword="true" id="old-password" maxlength="255" required="required" />
			</div>
		</div>
		<div class="control-group">
			<label class="control-label" for="login-password"><fmt:message key="user.newPassword.label"/></label>
			<div class="controls">
				<input type="password" name="password" id="login-password" maxlength="255" required="required"/>
			</div>
		</div>
		<div class="control-group">
			<label class="control-label" for="login-password-again"><fmt:message key="user.newPasswordAgain.label"/></label>
			<div class="controls">
				<input type="password" name="passwordAgain" id="login-password-again" maxlength="255" required="required"/>
			</div>
		</div>
	</fieldset>
	<div class="control-group">
		<div class="controls">
			<button type="submit" class="btn btn-primary"><fmt:message key='user.action.changePassword'/></button>
		</div>
	</div>
</form:form>
