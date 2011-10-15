package com.jayway.forest.reflection.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This object is returned when a method returns a List
 */
public class PagedSortedListResponse<T> {
    private String name;
    private Long page;
    private Long pageSize;
    private Long totalElements;
    private Long totalPages;
    private List<T> list;
    private String next;
    private String previous;

    //private String self;
    private Map<String, String> orderByAsc;
    private Map<String, String> orderByDesc;


    public List<?> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }

    public Long getPage() {
        return page;
    }

    public void setPage(Long page) {
        this.page = page;
    }

    public Long getPageSize() {
        return pageSize;
    }

    public void setPageSize(Long pageSize) {
        this.pageSize = pageSize;
    }

    public Long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(Long totalElements) {
        this.totalElements = totalElements;
    }

    public Long getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(Long totalPages) {
        this.totalPages = totalPages;
    }

    public String getNext() {
        return next;
    }

    public void setNext(String next) {
        this.next = next;
    }

    public String getPrevious() {
        return previous;
    }

    public void setPrevious(String previous) {
        this.previous = previous;
    }

    public String getName() {
        return name;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public Map<String, String> getOrderByAsc() {
        return orderByAsc;
    }

    public void addOrderByAsc( String field, String href ) {
        if ( orderByAsc == null ) {
            orderByAsc = new HashMap<String, String>();
        }
        orderByAsc.put( field, href);
    }

    public void addOrderByDesc( String field, String href ) {
        if ( orderByDesc == null ) {
            orderByDesc = new HashMap<String, String>();
        }
        orderByDesc.put( field, href );
    }

    public Map<String, String> getOrderByDesc() {
        return orderByDesc;
    }

}
