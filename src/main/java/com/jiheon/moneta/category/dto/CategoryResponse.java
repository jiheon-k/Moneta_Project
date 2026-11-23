package com.jiheon.moneta.category.dto;

public class CategoryResponse {
    private String name;
    private Long id;
    private boolean common;

    public CategoryResponse(String name, Long id, boolean common) {
        this.name = name;
        this.id=id;
        this.common = common;
    }

    public String getName() {
        return name;
    }
    public Long getId(){
        return id;
    }

    public boolean isCommon() {
        return common;
    }
}
