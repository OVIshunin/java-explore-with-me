package ru.practicum.ewm.category.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import ru.practicum.ewm.category.dto.CategoryDto;
import ru.practicum.ewm.category.mapper.CategoryMapper;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.category.repository.CategoryRepository;
import ru.practicum.ewm.exception.NotFoundException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category category;
    private CategoryDto categoryDto;

    @BeforeEach
    void setUp() {
        category = Category.builder()
                .id(1L)
                .name("Концерты")
                .build();

        categoryDto = CategoryDto.builder()
                .id(1L)
                .name("Концерты")
                .build();
    }

    // ==================== createCategory ====================

    @Test
    void createCategory_shouldSaveAndReturnCategory() {
        when(categoryMapper.toEntity(categoryDto)).thenReturn(category);
        when(categoryRepository.save(any(Category.class))).thenReturn(category);
        when(categoryMapper.toDto(category)).thenReturn(categoryDto);

        CategoryDto result = categoryService.createCategory(categoryDto);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Концерты");
        verify(categoryRepository).save(any(Category.class));
    }

    // ==================== updateCategory ====================

    @Test
    void updateCategory_shouldUpdateAndReturnCategory() {
        CategoryDto updateDto = CategoryDto.builder()
                .name("Выставки")
                .build();

        Category updatedCategory = Category.builder()
                .id(1L)
                .name("Выставки")
                .build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.save(any(Category.class))).thenReturn(updatedCategory);
        when(categoryMapper.toDto(updatedCategory)).thenReturn(CategoryDto.builder()
                .id(1L)
                .name("Выставки")
                .build());

        CategoryDto result = categoryService.updateCategory(1L, updateDto);

        assertThat(result.getName()).isEqualTo("Выставки");
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void updateCategory_shouldThrowNotFoundException_whenCategoryNotFound() {
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.updateCategory(999L, categoryDto))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Category with id 999 not found");
    }

    // ==================== deleteCategory ====================

    @Test
    void deleteCategory_shouldDeleteCategory_whenCategoryExists() {
        when(categoryRepository.existsById(1L)).thenReturn(true);

        categoryService.deleteCategory(1L);

        verify(categoryRepository).deleteById(1L);
    }

    @Test
    void deleteCategory_shouldThrowNotFoundException_whenCategoryNotFound() {
        when(categoryRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> categoryService.deleteCategory(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Category with id 999 not found");
    }

    // ==================== getCategories ====================

    @Test
    void getCategories_shouldReturnListOfCategories() {
        when(categoryRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(category)));
        when(categoryMapper.toDto(category)).thenReturn(categoryDto);

        List<CategoryDto> result = categoryService.getCategories(0, 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Концерты");
    }

    @Test
    void getCategories_shouldReturnEmptyList_whenNoCategories() {
        when(categoryRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        List<CategoryDto> result = categoryService.getCategories(0, 10);

        assertThat(result).isEmpty();
    }

    // ==================== getCategoryById ====================

    @Test
    void getCategoryById_shouldReturnCategory() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryMapper.toDto(category)).thenReturn(categoryDto);

        CategoryDto result = categoryService.getCategoryById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Концерты");
    }

    @Test
    void getCategoryById_shouldThrowNotFoundException_whenCategoryNotFound() {
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getCategoryById(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Category with id 999 not found");
    }
}
