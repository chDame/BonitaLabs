package com.bonitasoft.searcher.parser

import java.text.SimpleDateFormat;
import java.util.regex.Pattern;

import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.CriteriaQuery
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate
import javax.persistence.criteria.Root

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.search.WildcardQuery;
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.jpa.domain.Specification;

import com.bonitasoft.searcher.exception.LuceneSearchException

/**
 * Builds a JPA Specification from a Lucene query.
 * @author christophe.dame
 */
public class JpaSearchLuceneBuilder<T> {
	
	 private static QueryParser parser = null;

     private static SimpleDateFormat sqlFormat = new SimpleDateFormat("yyyy-MM-dd");

     public JpaSearchLuceneBuilder() {
     }
     
     private static QueryParser getParser() {
    	 if (parser==null) {
        	 parser = new QueryParser("id", new WhitespaceAnalyzer());
             parser.setDateResolution(DateTools.Resolution.DAY);
    	 }
    	 return parser;
     }
     
     /**
      * Builds a JPA Specification from a Lucene query. 
      */
     public Specification<T> getJpaSpecificationFromQuery(String luceneQuery) {
    	 if(StringUtils.isBlank(luceneQuery)) {
    		 return null;
    	 }
         try {
        	 return build(getParser().parse(luceneQuery));
        	 
		} catch (ParseException e) {
			throw new LuceneSearchException("Unable to parse the query string : "+luceneQuery);
		}
     }
     
     /**
      * build query (sub query) redirecting to the proper method depending on its type.
      */
     private Specification<T> build(Query query) {
     	if (query instanceof TermQuery) {
     		return build((TermQuery) query);
     	}
     	if (query instanceof BooleanQuery) {
     		return build((BooleanQuery) query);
     	}
     	if (query instanceof PhraseQuery) {
     		return build((PhraseQuery) query);
     	}
     	if (query instanceof TermRangeQuery) {
     		return buildQuery((TermRangeQuery) query);
     	}
     	if (query instanceof WildcardQuery) {
     		return buildQuery((WildcardQuery) query);
     	}
     	throw new LuceneSearchException("Search request not supported : "+query.toString());
     }
     
     /**
      * Builds a JPA Specification from a TermQuery.
      */
     private Specification<T> build(TermQuery termQuery) {
         Term term = termQuery.getTerm();
         if (Pattern.matches(".*[\\*|\\?].*", term.text())) {
         	return buildQuery(new WildcardQuery(term));
         }
         return buildQuery(termQuery);       
     } 

    /**
     * Builds a JPA Specification from a WildcardQuery.
     */
    private Specification<T> buildQuery(WildcardQuery wildcardQuery) {
    	
		return new Specification<T>() {
			public Predicate toPredicate(Root<T> root, CriteriaQuery query, CriteriaBuilder builder) {
			  return builder.like(getField(root, wildcardQuery.getTerm().field()), wildcardQuery.getTerm().text().replace('*', '%').replace('?', '_'));
			}
		}
	}

    /**
     * Builds a JPA Specification from a TermQuery.
     */
    private Specification<T> buildQuery(TermQuery termQuery) {
		
		return new Specification<T>() {
			public Predicate toPredicate(Root<T> root, CriteriaQuery query, CriteriaBuilder builder) {
				Path<Object> p = getField(root, termQuery.getTerm().field());
	    		if (termQuery.getTerm().text().equals("null")) {
	    			return builder.isNull(p);
	    		}
	    		if (p.getJavaType() == Boolean.class) {
	    			return builder.equal(p, Boolean.valueOf(termQuery.getTerm().text()));
	    		}
	    		return builder.equal(p, termQuery.getTerm().text());
			}
		}
    }
 
    /**
     * Builds a JPA Specification from a TermRangeQuery.
     */
    private Specification<T> buildQuery(TermRangeQuery rangeQuery) {
        String lowerTerm = null;
        if (rangeQuery.getLowerTerm()!=null) {
        	lowerTerm = formatTerm(rangeQuery.getLowerTerm().utf8ToString());
        }
        String upperTerm = null;
        if (rangeQuery.getUpperTerm()!=null) {
            upperTerm = formatTerm(rangeQuery.getUpperTerm().utf8ToString());
        }
        
        if(lowerTerm==null && upperTerm==null) {
        	return null;
        }
        final String lowerTermQuery = lowerTerm;
        final String upperTermQuery = upperTerm;
        if (lowerTerm!=null && upperTerm!=null) {
        	return new Specification<T>() {
				public Predicate toPredicate(Root<T> root, CriteriaQuery query, CriteriaBuilder builder) {
				  return builder.and(builder.greaterThanOrEqualTo(getField(root, rangeQuery.getField()), lowerTermQuery), builder.lessThanOrEqualTo(root.get(rangeQuery.getField()), upperTermQuery));;
				}
			}
		}
        if (lowerTerm!=null) {
        	return new Specification<T>() {
				public Predicate toPredicate(Root<T> root, CriteriaQuery query, CriteriaBuilder builder) {
				  return builder.greaterThanOrEqualTo(getField(root, rangeQuery.getField()), lowerTermQuery);
				}
			}
		}
        if (upperTerm!=null) {
        	return new Specification<T>() {
				public Predicate toPredicate(Root<T> root, CriteriaQuery query, CriteriaBuilder builder) {
				  return builder.lessThanOrEqualTo(getField(root, rangeQuery.getField()), upperTermQuery);
				}
			}
        }
        
        return null;
    }

    
    /**
     * Builds a JPA Specification from a BooleanQuery.
     */    
    private Specification<T> build(BooleanQuery booleanQuery) {
	
		Specification<T> spec = new Specification<T>() {
			public Predicate toPredicate(Root<T> root, CriteriaQuery query, CriteriaBuilder builder) {
			  return builder.and();
			}
		}
			
        List<BooleanClause> clauses= booleanQuery.clauses();
		
        boolean required = clauses.get(1).isRequired();

        List<BooleanClause> clausesToProcess = new ArrayList<>();
        clausesToProcess.add(clauses.get(0));
        clausesToProcess.add(clauses.get(1));
        if (required) {
        	spec = build(clauses.get(0).getQuery()).and(build(clauses.get(1).getQuery()));
        } else {
        	spec = build(clauses.get(0).getQuery()).or(build(clauses.get(1).getQuery()));
        }
        for(int i=2;i<clauses.size();i++) {
        	
        	BooleanClause clause = booleanQuery.clauses().get(i);
        	if (clause.isRequired() == required) {
        		spec = spec.and(build(clauses.get(i).getQuery()));
        	} else {
        		spec = spec.or(build(clauses.get(i).getQuery()));
        	}
        }

        return spec;
    }    
	      

    /**
     * Builds a JPA Specification from a PhraseQuery.
     */    
    private Specification<T> build(PhraseQuery phraseQuery) {
        int x = 0;
        String field = null;
        String text = "";
 
        // Join all tokens into a single token
        for(Term term : phraseQuery.getTerms()) { 
                if (x == 0) {
                    field = term.field();
                }
                if (x++ > 0) {
                    text += " ";
                }
 
                text += term.text();
            }
 
        if (Pattern.matches(".*[\\*|\\?].*", text)) {
        	return buildQuery(new WildcardQuery(new Term(field, text)));
        }
        return build(new TermQuery(new Term(field, text)));
        
    }    

    /**
     * Formats a term as either a text or date.
     */
    private static String formatTerm(String termval) {
        String term = termval;
 
        Date date = parseDate(termval);
        if (date!=null) {
            term = formatDate(date);
        }
 
        return term;
    }
    
    /**
     * Attempts to parse a Date from a query value.
     */
    private static Date parseDate(String text) {
        String[] formats = ["yyyy-MM-dd", "yyyyMMddHHmm", "yyyyMMdd", "dd-MMM-yyyy", "dd-MMM-yyyy HHmm"];
 
        for (String format : formats) {
            try {
            	if (format.length()==text.length()) {
            		return new SimpleDateFormat(format).parse(text);
            	}
            } catch (Exception ex) {
                // Ignore
            }
        }
 
        return null;
    }
 
    /**
     * Format a date to correct format.
     */
    private static String formatDate(Date date) {
        return "'"+sqlFormat.format(date)+"'";
    }
 
	/**
	 * Manage terms as foo.bar.doe
	 */
	private Path getField(Root<T> root, String field) {
		if (!field.contains(".")) {
			return root.get(field)
		}
		String[] array = field.split("\\.");
		Path result = root;
		for(int i=0;i<array.length;i++) {
			result = result.get(array[i]);
		}
		return result;
	}

}