var url = new URL(document.location.href);
	var c = url.searchParams.get("redirectUrl");

	function onSignIn(googleUser) {
		var url = new URL(document.location.href);
		var redirectUrl = url.searchParams.get("redirectUrl");
		top.location.href=url.protocol+'//'+url.host+'/bonita/API/extension/googleAuth?authCode='+googleUser.getAuthResponse().id_token+'&redirect='+url.protocol+'//'+url.host+'/bonita/apps/private';
	}
	
	
function loadGoogle(clientId) {	
    var meta = document.createElement('meta');
    console.log(meta);
    meta.name = "google-signin-client_id";
    meta.content =clientId;
    document.getElementsByTagName('head')[0].appendChild(meta);
    console.log( document.getElementsByTagName('head')[0]);
    
    document.getElementsByClassName('googleBtnContainer')[0].innerHTML = '<div class="g-signin2" data-onsuccess="onSignIn"></div>';
    
    var script = document.createElement('script');
    script.src = "https://apis.google.com/js/platform.js";
    script.defer = true;
    document.getElementsByTagName('head')[0].appendChild(script);
    
}