package com.bonitasoft.searcher

import java.lang.reflect.Field

import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory
import javax.persistence.metamodel.ManagedType
import javax.servlet.http.HttpServletRequest

import org.apache.commons.lang3.StringUtils
import org.bonitasoft.engine.business.data.BusinessDataRepository
import org.bonitasoft.engine.service.TenantServiceAccessor
import org.bonitasoft.engine.service.impl.ServiceAccessorFactory

import com.bonitasoft.searcher.exception.LuceneSearchException
import com.bonitasoft.searcher.model.Query
import com.bonitasoft.searcher.model.RestfulList
import com.bonitasoft.searcher.service.EntitySearcherService
import com.bonitasoft.web.extension.rest.RestAPIContext

import groovy.json.JsonBuilder

class EntityLuceneSearcher {
	
	
	private static Map<String, EntitySearcherService> searcherMap = new HashMap<>();
	
	private static boolean init = false;
	
	public static synchronized void init(RestAPIContext context) {
		if (!init) {
			ServiceAccessorFactory serviceAccessorFactory = ServiceAccessorFactory.getInstance();
			TenantServiceAccessor tsa = serviceAccessorFactory.createTenantServiceAccessor(context.getApiSession().tenantId);
	
			BusinessDataRepository bdr = tsa.getBusinessDataRepository();
			EntityManagerFactory emf = null;
			for(Field f : bdr.getClass().getDeclaredFields()) {
				if (EntityManagerFactory.class.isAssignableFrom(f.getType())) {
					f.setAccessible(true);
					emf = f.get(bdr);
				}
			}
			EntityManager em =  emf.createEntityManager();
			
			for (ManagedType<?> entity :  em.getMetamodel().getManagedTypes()) {
				EntitySearcherService es = new EntitySearcherService(entity.getJavaType(), em);
				searcherMap.put(entity.getJavaType().getSimpleName(), es);
				searcherMap.put(entity.getJavaType().getName(), es);
			}
			init = true;
		}
	}
	public static RestfulList list(String entity, String query) {
		return list(entity, query, null);
	}
	public static RestfulList list(String entity, String query, Integer count) {
		return list(entity, query, count, null);
	}
	public static RestfulList list(String entity, String query, Integer count, Integer page) {
		return list(entity, query, count, page, null);
	}
	public static RestfulList list(String entity, String query, Integer count, Integer page, String orderBy) {
		return list(entity, query, count, page, orderBy, null);
	}
	public static RestfulList list(String entity, String query, Integer count, Integer page, String orderBy, String order) {
		if (!searcherMap.containsKey(entity)) {
			throw new LuceneSearchException("Unmanaged entity "+entity+". Lists of availableEntities : "+new JsonBuilder(searcherMap.keySet()).toString())
		}
		if (page==null || page<0) {
			page=0;
		}
		if (count==null || count<0) {
			count = 10;
		}
		if (StringUtils.isBlank(orderBy)) {
			orderBy = "persistenceId";
		}
		if (StringUtils.isBlank(order)) {
			order = "ASC";
		}
		List result = null;
		Long total = null
		if (StringUtils.isBlank(query)) {
			total = searcherMap.get(entity).count();
			result = searcherMap.get(entity).findAll(count, page, orderBy, order);
		} else {
			total = searcherMap.get(entity).count(query);
			result = searcherMap.get(entity).list(query, count, page, orderBy, order);
		}
		RestfulList list = new RestfulList();
		list.setCount(result.size());
		list.setTotal(total);
		list.setResults(result);
		Query resultQuery = new Query();
		resultQuery.setCount(count);
		resultQuery.setPage(page);
		resultQuery.setEntity(entity);
		resultQuery.setQuery(query);
		resultQuery.setOrderBy(orderBy);
		resultQuery.setOrder(order);
		list.set_query(resultQuery);
		
		return list;
	}
	
	public static addLinks(RestfulList result, HttpServletRequest request) {
		String baseUrl = request.getRequestURL();//.substring(0, request.getRequestURL().length() - request.getRequestURI().length()) + request.getContextPath();
		Map<String, String> links = new HashMap<>();
		links.put("base", baseUrl);
		String query = "?entity="+result.get_query().entity+"&query="+result.get_query().query;
		String order = "&orderby="+result.get_query().orderBy+"&order="+result.get_query().order;
		String self = "&page="+result.get_query().page+"&count="+result.get_query().count;
		links.put("self", baseUrl+query+self+order);
		if (result.get_query().page>0) {
			String prev = "&page="+(result.get_query().page-1)+"&count="+result.get_query().count;
			links.put("prev", baseUrl+query+prev+order);
			
		}
		if ((result.get_query().page+1)*result.get_query().count < result.getTotal()) {
			String next = "&page="+(result.get_query().page+1)+"&count="+result.get_query().count;
			links.put("next", baseUrl+query+next+order);
		}
		result.set_links(links);
	}
	
}
