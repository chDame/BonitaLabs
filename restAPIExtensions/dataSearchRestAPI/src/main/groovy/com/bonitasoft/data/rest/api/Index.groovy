package com.bonitasoft.data.rest.api;

import java.lang.reflect.Field

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import org.bonitasoft.web.extension.ResourceProvider
import org.bonitasoft.web.extension.rest.RestApiResponse
import org.bonitasoft.web.extension.rest.RestApiResponseBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.mapping.PropertyReferenceException

import com.bonitasoft.searcher.EntityLuceneSearcher
import com.bonitasoft.searcher.exception.LuceneSearchException
import com.bonitasoft.searcher.json.BusinessDataObjectMapper
import com.bonitasoft.searcher.model.RestfulList
import com.bonitasoft.web.extension.rest.RestAPIContext
import com.bonitasoft.web.extension.rest.RestApiController

import groovy.json.JsonBuilder

class Index implements RestApiController {

    private static final Logger LOGGER = LoggerFactory.getLogger(Index.class);
	
		
    @Override
    RestApiResponse doHandle(HttpServletRequest request, RestApiResponseBuilder responseBuilder, RestAPIContext context) {

        def entity = request.getParameter "entity"
        def e = request.getParameter "e"
        if (e == null && entity==null) {
			def error = ["error": "the parameter entity/e is missing", "parameters":["entity":"Mandatory. The entity you're searching. Shortcut e",
				"query" :"Optional. Lucene query string. Shortcut q. E.G. q=persistenceId:1 AND licenseAccess.description:\"youp*\"",
				"count" :"Optional. If not defined, it will be 10. Shortcut c",
				"page" : "Optional. If not defined, it will be 0. Shortcut p",
				"sortedBy" : "Optional. If not defined, it will be persistenceId. Shortcut s",
				"order" : "Optionnal. ASC/DESC. If not defined, it will be ASC. Shortcut o"]]
            return buildResponse(responseBuilder, HttpServletResponse.SC_BAD_REQUEST,  new JsonBuilder(error).toString())
        }
		if (entity==null && e!=null) {
			entity = e;
		}

        def queryParam = request.getParameter "query"
		if (queryParam == null) {
			queryParam = request.getParameter "q"
		}
        def countParam = request.getParameter "count"
		if (countParam == null) {
			countParam = request.getParameter "c"
		}
        def pageParam = request.getParameter "page"
		if (pageParam == null) {
			pageParam = request.getParameter "p"
		}
        def sortedByParam = request.getParameter "sortedBy"
		if (sortedByParam == null) {
			sortedByParam = request.getParameter "s"
		}
        def orderParam = request.getParameter "order"
		if (orderParam == null) {
			orderParam = request.getParameter "o"
		}
        Long page = pageParam!=null ? Long.valueOf(pageParam) : null;
        Long count = countParam!=null ? Long.valueOf(countParam) : null;
		String order = "ASC";
        if (orderParam!=null && orderParam == "DESC") {
			order = orderParam;
		}

		EntityLuceneSearcher.init(context);
		try {
			RestfulList result = EntityLuceneSearcher.list(entity, queryParam, count, page, sortedByParam, order);
			EntityLuceneSearcher.addLinks(result, request);
			
			return buildResponse(responseBuilder, HttpServletResponse.SC_OK, BusinessDataObjectMapper.toJson(result))
		} catch (PropertyReferenceException | LuceneSearchException exception) {
			return buildResponse(responseBuilder, HttpServletResponse.SC_BAD_REQUEST,  "{\"error\" : \""+exception.getMessage()+"\"}")
		}
		

	}
	
	public static List<Field> getAllFields(List<Field> fields, Class<?> type) {
		fields.addAll(Arrays.asList(type.getDeclaredFields()));
	
		if (type.getSuperclass() != null) {
			getAllFields(fields, type.getSuperclass());
		}
	
		return fields;
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
