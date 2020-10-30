package com.bonitasoft.rest.api;

import java.nio.charset.StandardCharsets

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import org.apache.commons.io.IOUtils
import org.bonitasoft.engine.identity.User
import org.bonitasoft.engine.identity.UserNotFoundException
import org.bonitasoft.engine.profile.Profile
import org.bonitasoft.engine.profile.ProfileMemberCreator
import org.bonitasoft.engine.profile.ProfileSearchDescriptor
import org.bonitasoft.engine.search.SearchOptionsBuilder
import org.bonitasoft.engine.search.SearchResult
import org.bonitasoft.web.extension.ResourceProvider
import org.bonitasoft.web.extension.rest.RestApiResponse
import org.bonitasoft.web.extension.rest.RestApiResponseBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.bonitasoft.engine.api.IdentityAPI
import com.bonitasoft.engine.api.ProfileAPI
import com.bonitasoft.web.extension.rest.RestAPIContext
import com.bonitasoft.web.extension.rest.RestApiController

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper

class Index implements RestApiController {

    private static final Logger LOGGER = LoggerFactory.getLogger(Index.class)

	private static JsonSlurper jsonSlurper = new JsonSlurper();
	
	private static final String PROFILE="User"; 

    @Override
    RestApiResponse doHandle(HttpServletRequest request, RestApiResponseBuilder responseBuilder, RestAPIContext context) {

		def licenceRequest = jsonSlurper.parseText(IOUtils.toString(request.getInputStream(), StandardCharsets.UTF_8));
        if (licenceRequest.login == null) {
            return buildResponse(responseBuilder, HttpServletResponse.SC_BAD_REQUEST,"""{"error" : "the attribute login is missing"}""")
        }        
		
		if (licenceRequest.pwd == null) {
            return buildResponse(responseBuilder, HttpServletResponse.SC_BAD_REQUEST,"""{"error" : "the attribute pwd is missing"}""")
        }

		if (licenceRequest.firstname == null) {
			return buildResponse(responseBuilder, HttpServletResponse.SC_BAD_REQUEST,"""{"error" : "the attribute firstname is missing"}""")
        }
		if (licenceRequest.lastname == null) {
			return buildResponse(responseBuilder, HttpServletResponse.SC_BAD_REQUEST,"""{"error" : "the attribute lastname is missing"}""")
        }

		
		IdentityAPI identityApi = context.getApiClient().getIdentityAPI();
		try {
			User u = identityApi.getUserByUserName(licenceRequest.login);
			return buildResponse(responseBuilder, HttpServletResponse.SC_BAD_REQUEST,"{\"error\" : \"the user "+licenceRequest.login+" already exists\"}");
		} catch(UserNotFoundException e) {
			//everything's fine, user does not exists
		}
		ProfileAPI profileApi = context.getApiClient().getProfileAPI();
		
		User user = identityApi.createUser(licenceRequest.login, licenceRequest.pwd, licenceRequest.firstname, licenceRequest.lastname);
		// The user must now be registered in a profile. Let's choose the existing "User" profile:
		SearchOptionsBuilder searchOptionsBuilder = new SearchOptionsBuilder(0,1);
		searchOptionsBuilder.filter(ProfileSearchDescriptor.NAME, PROFILE);
		SearchResult<Profile> searchResultProfile = profileApi.searchProfiles(searchOptionsBuilder.done());
		
		ProfileMemberCreator profileMemberCreator = new ProfileMemberCreator( searchResultProfile.getResult().get(0).getId() );
		profileMemberCreator.setUserId( user.getId() );
		profileApi.createProfileMember(profileMemberCreator);

		def result=["status":"ok", "userId" : user.getId()]

        return buildResponse(responseBuilder, HttpServletResponse.SC_OK, new JsonBuilder(result).toString())
    }

    /**
     * Build an HTTP response.
     *
     * @param  responseBuilder the Rest API response builder
     * @param  httpStatus the status of the response
     * @param  body the response body
     * @return a RestAPIResponse
     */
    RestApiResponse buildResponse(RestApiResponseBuilder responseBuilder, int httpStatus, Serializable body) {
        return responseBuilder.with {
            withResponseStatus(httpStatus)
            withResponse(body)
            build()
        }
    }

    /**
     * Returns a paged result like Bonita BPM REST APIs.
     * Build a response with a content-range.
     *
     * @param  responseBuilder the Rest API response builder
     * @param  body the response body
     * @param  p the page index
     * @param  c the number of result per page
     * @param  total the total number of results
     * @return a RestAPIResponse
     */
    RestApiResponse buildPagedResponse(RestApiResponseBuilder responseBuilder, Serializable body, int p, int c, long total) {
        return responseBuilder.with {
            withContentRange(p,c,total)
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
