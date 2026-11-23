package com.jiheon.moneta.category.dto;

import jakarta.validation.constraints.*;

public class CategoryRequest {
    @NotBlank
    @Size(max = 100)
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
