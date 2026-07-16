package ru.practicum.ewm.service.category;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import ru.practicum.ewm.dto.category.CategoryDto;
import ru.practicum.ewm.dto.category.NewCategoryDto;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.CategoryMapper;
import ru.practicum.ewm.model.Category;
import ru.practicum.ewm.repository.CategoryRepository;
import ru.practicum.ewm.repository.EventRepository;

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
    private EventRepository eventRepository;
    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category category;
    private CategoryDto categoryDto;
    private NewCategoryDto newCategoryDto;

    @BeforeEach
    void setUp() {
        category = Category.builder()
                .id(1L)
                .name("Test Category")
                .build();

        categoryDto = CategoryDto.builder()
                .id(1L)
                .name("Test Category")
                .build();

        newCategoryDto = NewCategoryDto.builder()
                .name("Test Category")
                .build();
    }

    @Test
    void createCategory_success() {
        when(categoryRepository.existsByName("Test Category")).thenReturn(false);
        when(categoryMapper.toEntity(newCategoryDto)).thenReturn(category);
        when(categoryRepository.save(any(Category.class))).thenReturn(category);
        when(categoryMapper.toDto(category)).thenReturn(categoryDto);

        CategoryDto result = categoryService.createCategory(newCategoryDto);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Category");
    }

    @Test
    void createCategory_duplicateName_throwsConflictException() {
        when(categoryRepository.existsByName("Test Category")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.createCategory(newCategoryDto))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Category with name 'Test Category' already exists");
    }

    @Test
    void updateCategory_success() {
        CategoryDto updateDto = CategoryDto.builder()
                .name("Updated Category")
                .build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByName("Updated Category")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(category);
        when(categoryMapper.toDto(category)).thenReturn(updateDto);

        CategoryDto result = categoryService.updateCategory(1L, updateDto);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Updated Category");
    }

    @Test
    void updateCategory_categoryNotFound_throwsNotFoundException() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.updateCategory(1L, categoryDto))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Category with id 1 not found");
    }

    @Test
    void updateCategory_duplicateName_throwsConflictException() {
        CategoryDto updateDto = CategoryDto.builder()
                .name("Existing Category")
                .build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByName("Existing Category")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.updateCategory(1L, updateDto))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Category with name 'Existing Category' already exists");
    }

    @Test
    void deleteCategory_success() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(eventRepository.existsByCategoryId(1L)).thenReturn(false);

        categoryService.deleteCategory(1L);

        verify(categoryRepository).deleteById(1L);
    }

    @Test
    void deleteCategory_categoryNotFound_throwsNotFoundException() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.deleteCategory(1L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void deleteCategory_withEvents_throwsConflictException() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(eventRepository.existsByCategoryId(1L)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.deleteCategory(1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Cannot delete category with existing events");
    }

    @Test
    void getCategories_success() {
        Page<Category> categoryPage = new PageImpl<>(List.of(category));

        when(categoryRepository.findAll(any(Pageable.class))).thenReturn(categoryPage);
        when(categoryMapper.toDto(category)).thenReturn(categoryDto);

        List<CategoryDto> result = categoryService.getCategories(0, 10);

        assertThat(result).hasSize(1);
    }

    @Test
    void getCategoryById_success() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryMapper.toDto(category)).thenReturn(categoryDto);

        CategoryDto result = categoryService.getCategoryById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getCategoryById_notFound_throwsNotFoundException() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getCategoryById(1L))
                .isInstanceOf(NotFoundException.class);
    }
}