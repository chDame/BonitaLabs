package com.bonitasoft.searcher.exception

class LuceneSearchException  extends RuntimeException {

    private static final long serialVersionUID = -7593616210087047797L;

    public LuceneSearchException() {
        super();
    }

    public LuceneSearchException(Exception e) {
        super(e);
    }

    public LuceneSearchException(String message) {
        super(message);
    }

    public LuceneSearchException(String message, Exception e) {
        super(message, e);
    }
}
