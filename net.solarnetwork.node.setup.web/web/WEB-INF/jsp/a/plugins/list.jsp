<section class="intro">
	<p><fmt:message key="plugins.intro"/></p>
	<fmt:message key="plugins.loading.message" var="msgLoading"/>
	<fmt:message key="plugins.refresh.button" var="msgRefresh"/>
	<c:url value="/plugins/refresh" var="urlPluginRefresh"/>
	<a id="plugins-refresh" class="btn btn-primary ladda-button expand-right" href="${urlPluginRefresh}"
		data-loading-text="${msgLoading}">
		${msgRefresh}
	</a>
</section>
<section id="plugins">
</section>

<c:url value="/plugins/install" var="urlPluginInstall"/>
<fmt:message key="plugin.install.button" var="msgInstall"/>
<form id="plugin-preview-install-modal" class="modal dynamic hide fade" action="${urlPluginInstall}" method="post">
	<div class="modal-header">
		<button type="button" class="close" data-dismiss="modal">&times;</button>
		<h3 data-msg-install="${msgInstall}"><fmt:message key="plugin.install.title"/></h3>
	</div>
	<div class="modal-body">
		<p><fmt:message key="plugin.install.intro"/></p>
		<div id="plugin-preview-install-list"></div>
		<div class="progress progress-striped active hide">
			<div class="bar"></div>
	    </div>
	    <div id="plugin-install-error"></div>
	</div>
	<div class="modal-footer">
		<input type="hidden" name="uid" value=""/>
		<a href="#" class="btn" data-dismiss="modal"><fmt:message key="close.label"/></a>
		<fmt:message key="plugin.installing.message" var="msgInstalling"/>
		<fmt:message key="plugin.install.error" var="msgInstallError"/>
		<fmt:message key="plugin.install.success" var="msgInstallSuccess"/>
		<button type="submit" class="btn btn-primary ladda-button expand-right"
			data-msg-error="${msgInstallError}"
			data-msg-success="${msgInstallSuccess}"
			data-loading-text="${msgInstalling}">${msgInstall}</button>
	</div>
</form>