package com.bonitasoft.searcher.model

class RestfulList {
	
	private List results;
	private Long count;
	private Long total;
	private Map<String, String> _links;
	private Query _query;
	public List getResults() {
		return results;
	}
	public void setResults(List results) {
		this.results = results;
	}
	public Long getCount() {
		return count;
	}
	public void setCount(Long count) {
		this.count = count;
	}
	public Long getTotal() {
		return total;
	}
	public void setTotal(Long total) {
		this.total = total;
	}
	public Map<String, String> get_links() {
		return _links;
	}
	public void set_links(Map<String, String> _links) {
		this._links = _links;
	}
	public Query get_query() {
		return _query;
	}
	public void set_query(Query _query) {
		this._query = _query;
	}
	
	
}