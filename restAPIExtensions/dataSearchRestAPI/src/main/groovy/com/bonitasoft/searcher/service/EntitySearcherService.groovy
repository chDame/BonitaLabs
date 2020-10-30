package com.bonitasoft.searcher.service

import javax.persistence.EntityManager

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort.Direction
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.support.SimpleJpaRepository

import com.bonitasoft.searcher.json.BusinessDataObjectMapper
import com.bonitasoft.searcher.parser.JpaSearchLuceneBuilder

class EntitySearcherService<T> {

	private SimpleJpaRepository jpaRepository;
	
	private JpaSearchLuceneBuilder searchBuilder;
	
	public EntitySearcherService(Class<T> type, EntityManager em) {
		searchBuilder = new JpaSearchLuceneBuilder<T>();
		jpaRepository = new SimpleJpaRepository<>(type, em);
	}

	public List<T> list(String query, int count, int page, String orderBy, String order) {
		Pageable pageable = getPageable(count, page, orderBy, order);
		return jpaRepository.findAll(getSpecifications(query), pageable).getContent();
	}

	public List<T> findAll(int count, int page, String orderBy, String order) {
		Pageable pageable = getPageable(count, page, orderBy, order);
		return jpaRepository.findAll(pageable).getContent();
	}
	
	public Long count(String query) {
		return jpaRepository.count(getSpecifications(query));
	}
	
	public Long count() {
		return jpaRepository.count();
	}
	
	private Specification<T> getSpecifications(String luceneQuery) {
		return searchBuilder.getJpaSpecificationFromQuery(luceneQuery);
	}
	
	private Pageable getPageable(int count, int page, String orderBy, String order) {
		return PageRequest.of(page, count, Direction.valueOf(order), orderBy);
	}
	
}