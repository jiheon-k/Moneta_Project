package com.jiheon.moneta.category;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import com.jiheon.moneta.auth.security.LoginUser;
import com.jiheon.moneta.category.dto.CategoryRequest;
import com.jiheon.moneta.category.dto.CategoryResponse;
import jakarta.validation.Valid;

// 카테고리 목록·생성·수정·삭제 요청을 받아 Service로 전달한다.
@RestController
@RequestMapping("/categories")
public class CategoryController {
    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    // 카테고리 목록 조회
    @GetMapping
    public List<CategoryResponse> getCategoryList(@AuthenticationPrincipal LoginUser loginId) {
        return categoryService.getCategoryList(loginId.getId());
    }

    // 사용자 지정 카테고리 추가
    @PostMapping
    public void addNewCategory(@AuthenticationPrincipal LoginUser loginId,
            @Valid @RequestBody CategoryRequest request) {
        categoryService.addNewCateogory(loginId.getId(), request);
    }

    // 사용자가 추가했던 카테고리 이름 수정
    @PatchMapping("/{id}")
    public void changeCategory(@AuthenticationPrincipal LoginUser loginId,
            @PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        categoryService.changeCategory(loginId.getId(), id, request);
    }

    // 사용자가 추가했던 카테고리 삭제
    @DeleteMapping("/{id}")
    public void deleteCategory(@AuthenticationPrincipal LoginUser loginId,
            @PathVariable Long id) {
        categoryService.deleteCategory(loginId.getId(), id);
    }
}
