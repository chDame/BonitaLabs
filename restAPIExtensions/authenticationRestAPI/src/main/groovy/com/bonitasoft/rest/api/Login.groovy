package com.bonitasoft.rest.api;

import java.nio.charset.StandardCharsets

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpSession

import org.apache.commons.io.IOUtils
import org.bonitasoft.console.common.server.login.LoginFailedException
import org.bonitasoft.console.common.server.utils.PermissionsBuilder
import org.bonitasoft.console.common.server.utils.PermissionsBuilderAccessor
import org.bonitasoft.console.common.server.utils.SessionUtil
import org.bonitasoft.engine.platform.LoginException
import org.bonitasoft.engine.platform.UnknownUserException
import org.bonitasoft.engine.session.APISession
import org.bonitasoft.web.extension.ResourceProvider
import org.bonitasoft.web.extension.rest.RestApiResponse
import org.bonitasoft.web.extension.rest.RestApiResponseBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.bonitasoft.engine.api.LoginAPI
import com.bonitasoft.web.extension.rest.RestAPIContext
import com.bonitasoft.web.extension.rest.RestApiController

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper

class Login implements RestApiController {

    private static final Logger LOGGER = LoggerFactory.getLogger(Login.class)

	private static JsonSlurper jsonSlurper = new JsonSlurper();

    @Override
    RestApiResponse doHandle(HttpServletRequest request, RestApiResponseBuilder responseBuilder, RestAPIContext context) {

		def licenceRequest = jsonSlurper.parseText(IOUtils.toString(request.getInputStream(), StandardCharsets.UTF_8));
        if (licenceRequest.login == null) {
            return buildResponse(responseBuilder, HttpServletResponse.SC_BAD_REQUEST,"""{"error" : "the attribute login is missing"}""")
        }        
		
		if (licenceRequest.pwd == null) {
            return buildResponse(responseBuilder, HttpServletResponse.SC_BAD_REQUEST,"""{"error" : "the attribute pwd is missing"}""")
        }
		
		LoginAPI loginAPI = context.getApiClient().getLoginAPI();
		HttpSession httpSession = request.getSession();
		try {
			APISession userApiSession = loginAPI.login(licenceRequest.login, licenceRequest.pwd);
			httpSession.setAttribute(SessionUtil.API_SESSION_PARAM_KEY, userApiSession);
			PermissionsBuilder permissionsBuilder = PermissionsBuilderAccessor.createPermissionBuilder(userApiSession);
			def permissions = permissionsBuilder.getPermissions();
			httpSession.setAttribute(SessionUtil.PERMISSIONS_SESSION_PARAM_KEY, permissions);

			def result=["status":"ok", "userId" : userApiSession.getUserId(), "redirect" : "private"]
	
	        return buildResponse(responseBuilder, HttpServletResponse.SC_OK, new JsonBuilder(result).toString())
		} catch (LoginException | UnknownUserException | LoginFailedException e) {
			return buildResponse(responseBuilder, HttpServletResponse.SC_UNAUTHORIZED,"""{"error" : "Invalid credentials"}""")
		}
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
