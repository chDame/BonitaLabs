package com.company.rest.api;

import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import org.bonitasoft.web.extension.ResourceProvider
import org.bonitasoft.web.extension.rest.RestApiResponse
import org.bonitasoft.web.extension.rest.RestApiResponseBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.bonitasoft.web.extension.rest.RestAPIContext
import com.bonitasoft.web.extension.rest.RestApiController

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper

class Index implements RestApiController {

    private static final Logger LOGGER = LoggerFactory.getLogger(Index.class)

    @Override
    RestApiResponse doHandle(HttpServletRequest request, RestApiResponseBuilder responseBuilder, RestAPIContext context) {
        // To retrieve query parameters use the request.getParameter(..) method.
        // Be careful, parameter values are always returned as String values

        // Retrieve p parameter
		
		http://localhost:8080/bonita/API/bpm/humanTask?c=50&d=context&f=state%3Dready&f=user_id%3D16&o=displayName+ASC&p=0
       
		def p = request.getParameter "p"
        if (p == null) {
            return buildResponse(responseBuilder, HttpServletResponse.SC_BAD_REQUEST,"""{"error" : "the parameter p is missing"}""")
        }
		
		def userId = request.getParameter "userId"
		if (userId == null) {
			return buildResponse(responseBuilder, HttpServletResponse.SC_BAD_REQUEST,"""{"error" : "the parameter userId is missing"}""")
        }


        // Retrieve c parameter
        def c = request.getParameter "c"
        if (c == null) {
            return buildResponse(responseBuilder, HttpServletResponse.SC_BAD_REQUEST,"""{"error" : "the parameter c is missing"}""")
        }
        def f = request.getParameter "f"
		
		def o = request.getParameter "o"
		if (o == null) {
			o="displayName ASC"
		}
		String baseUrl = request.getRequestURL().substring(0, request.getRequestURL().length() - request.getRequestURI().length()) + request.getContextPath();
		Cookie[] cookies = request.getCookies();
		String jsessionid = null;
		String bonitaToken = null;
		for(int i=0;i<cookies.length;i++) {
			Cookie cookie = cookies[i];
			if (cookie.name.toLowerCase() == "jsessionid") {
				jsessionid = cookie.value;
			}
			if (cookie.name == "X-Bonita-API-Token") {
				bonitaToken = cookie.value;
			}
		}

		URL url = new URL(baseUrl+"/API/bpm/humanTask?c="+c+"&p="+p+"&d=rootContainerId&o="+URLEncoder.encode(o, "UTF-8")+"&f=state%3Dready&f=user_id%3D"+userId);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestProperty("Cookie", "JSESSIONID="+jsessionid);
		con.setRequestProperty("X-Bonita-API-Token", bonitaToken)
		con.setRequestMethod("GET");
		
		
		BufferedReader streamReader = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
		StringBuilder responseStrBuilder = new StringBuilder();
 
		String inputStr;
		while ((inputStr = streamReader.readLine()) != null) {
			responseStrBuilder.append(inputStr);
		}
		//JSONObject jsonObject = new JSONObject();
		
		def jsonSlurper = new JsonSlurper()
		List mapFromNative = jsonSlurper.parseText(responseStrBuilder.toString())
		Long[] ids = new Long[mapFromNative.size()];
		int idx = 0;
		Map<Long, Map<String, String>> fastAccess = new HashMap<>();
		List<Map<String, String>> result = new ArrayList<>();
		for(def task : mapFromNative) {
			Map<String, String> taskResult = new HashMap<>();
			taskResult.put("id", task.id);
			Long caseId = Long.valueOf(task.parentCaseId);
			ids[idx++] = caseId;
			fastAccess.put(caseId, taskResult);
			taskResult.put("rootCaseId", task.rootCaseId);
			taskResult.put("parentCaseId", task.parentCaseId);
			taskResult.put("displayName", task.displayName);
			taskResult.put("name", task.name);
			taskResult.put("processId", task.processId);
			taskResult.put("priority", task.priority);
			taskResult.put("processName", task.rootContainerId.displayName);
			taskResult.put("processVersion", task.rootContainerId.version);
			taskResult.put("assigned_id", task.assigned_id)
			result.add(taskResult);
			
		}

        // Send the result as a JSON representation
        // You may use buildPagedResponse if your result is multiple
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
