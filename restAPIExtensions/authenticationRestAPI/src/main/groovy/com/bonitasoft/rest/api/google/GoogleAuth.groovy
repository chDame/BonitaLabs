package com.bonitasoft.rest.api.google

import java.security.GeneralSecurityException

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpSession

import org.bonitasoft.console.common.server.login.LoginFailedException
import org.bonitasoft.console.common.server.utils.PermissionsBuilder
import org.bonitasoft.console.common.server.utils.PermissionsBuilderAccessor
import org.bonitasoft.console.common.server.utils.SessionUtil
import org.bonitasoft.engine.identity.User
import org.bonitasoft.engine.identity.UserNotFoundException
import org.bonitasoft.engine.platform.LoginException
import org.bonitasoft.engine.platform.UnknownUserException
import org.bonitasoft.engine.profile.Profile
import org.bonitasoft.engine.profile.ProfileMemberCreator
import org.bonitasoft.engine.profile.ProfileSearchDescriptor
import org.bonitasoft.engine.search.SearchOptionsBuilder
import org.bonitasoft.engine.search.SearchResult
import org.bonitasoft.engine.session.APISession
import org.bonitasoft.web.extension.ResourceProvider
import org.bonitasoft.web.extension.rest.RestApiResponse
import org.bonitasoft.web.extension.rest.RestApiResponseBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.bonitasoft.engine.api.IdentityAPI
import com.bonitasoft.engine.api.LoginAPI
import com.bonitasoft.engine.api.ProfileAPI
import com.bonitasoft.web.extension.rest.RestAPIContext
import com.bonitasoft.web.extension.rest.RestApiController
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory

import groovy.json.JsonBuilder

class GoogleAuth implements RestApiController {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleAuth.class);

	private static Properties props = null;
	
	private static final String PWD_PREFIX = "pWdSocPre_"
	
	private static final String PROFILE="User";
	
	@Override
	RestApiResponse doHandle(HttpServletRequest request, RestApiResponseBuilder responseBuilder, RestAPIContext context) {
		if (props==null) {
			props = loadProperties("configuration.properties", context.resourceProvider);
		}

		def authCode = request.getParameter("authCode");
		if (authCode == null) {
			 return buildResponse(responseBuilder, HttpServletResponse.SC_BAD_REQUEST,"""{"error" : "the parameter authCode is missing"}""")
		}
		def redirect = request.getParameter("redirect");
		try {
			GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(),
					  JacksonFactory.getDefaultInstance())
					.setAudience(Collections.singletonList(props.get("googleClientId")))
					.build();

				GoogleIdToken idToken = verifier.verify(authCode);
				if (idToken != null) {
					Payload payload = idToken.getPayload();
					String email = payload.getEmail();
					String familyName = (String) payload.get("family_name");
					String givenName = (String) payload.get("given_name");
					
					IdentityAPI identityApi = context.getApiClient().getIdentityAPI();
					ProfileAPI profileApi = context.getApiClient().getProfileAPI();
					LoginAPI loginAPI = context.getApiClient().getLoginAPI();
					try {
						User u = identityApi.getUserByUserName(email);
					} catch(UserNotFoundException e) {
						//user doesn't exist we create him
					
						User user = identityApi.createUser(email, generatePwd(email, givenName), givenName, familyName);
						
						SearchOptionsBuilder searchOptionsBuilder = new SearchOptionsBuilder(0,1);
						searchOptionsBuilder.filter(ProfileSearchDescriptor.NAME, PROFILE);
						SearchResult<Profile> searchResultProfile = profileApi.searchProfiles(searchOptionsBuilder.done());
						
						ProfileMemberCreator profileMemberCreator = new ProfileMemberCreator( searchResultProfile.getResult().get(0).getId() );
						profileMemberCreator.setUserId( user.getId() );
						profileApi.createProfileMember(profileMemberCreator);
					}
					
					
					HttpSession httpSession = request.getSession();
					
					APISession userApiSession = loginAPI.login(email, generatePwd(email, givenName));
					httpSession.setAttribute(SessionUtil.API_SESSION_PARAM_KEY, userApiSession);
					PermissionsBuilder permissionsBuilder = PermissionsBuilderAccessor.createPermissionBuilder(userApiSession);
					def permissions = permissionsBuilder.getPermissions();
					httpSession.setAttribute(SessionUtil.PERMISSIONS_SESSION_PARAM_KEY, permissions);
			
					def result=["status":"ok", "userId" : userApiSession.getUserId(), "redirect" : "private"]
				
					if (redirect != null) {
						return responseBuilder.with {
							withResponseStatus(HttpServletResponse.SC_MOVED_TEMPORARILY)
							withResponse(new JsonBuilder(result).toString())
							withAdditionalHeader("Location", redirect)
							build()
						}
					}
				    return buildResponse(responseBuilder, HttpServletResponse.SC_OK, new JsonBuilder(result).toString())
				} else {
					return buildResponse(responseBuilder, HttpServletResponse.SC_BAD_REQUEST,"""{"error" : "Google login failed"}""")
				}
		} catch(IOException | GeneralSecurityException e) {
			return buildResponse(responseBuilder, HttpServletResponse.SC_BAD_REQUEST,"""{"error" : "Google login failed"}""")
		}
		catch (LoginException | UnknownUserException | LoginFailedException e) {
			return buildResponse(responseBuilder, HttpServletResponse.SC_UNAUTHORIZED,"""{"error" : "Invalid credentials"}""")
		}

	}
	
	private String generatePwd(String email, String lastname) {
		return ""+(PWD_PREFIX+email+lastname).hashCode();
	}
	
	RestApiResponse buildResponse(RestApiResponseBuilder responseBuilder, int httpStatus, Serializable body) {
		return responseBuilder.with {
			withResponseStatus(httpStatus)
			withResponse(body)
			build()
		}
	}

	/**
	 * Load a property file into a java.util.Properties
	 */
	Properties loadProperties(String fileName, ResourceProvider resourceProvider) {
		Properties props = new Properties()
		resourceProvider.getResourceAsStream(fileName).withStream { InputStream s ->
			props.load s
		}
		props
	}
}
