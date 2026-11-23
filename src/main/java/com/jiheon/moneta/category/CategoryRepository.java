package com.jiheon.moneta.category;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// DB에서 공통·개인 카테고리를 조회하고 저장·삭제한다.
public interface CategoryRepository extends JpaRepository<Category, Long>{
    @Query("select c from Category c where c.id = :id and (c.user is null or c.user.id = :userId)")
    Optional<Category> findAvailable(@Param("id") Long id, @Param("userId") Long userId);
    List<Category> findByUserIsNullOrUser_Id(Long userId);

    Optional<Category> findByIdAndUser_Id(Long id, Long userId);
}
