package ru.practicum.ewm.category.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.category.dto.CategoryDto;
import ru.practicum.ewm.category.mapper.CategoryMapper;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.category.repository.CategoryRepository;
import ru.practicum.ewm.exception.NotFoundException;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional
    public CategoryDto createCategory(CategoryDto categoryDto) {
        log.info("Creating category: {}", categoryDto);

        Category category = categoryMapper.toEntity(categoryDto);
        Category saved = categoryRepository.save(category);

        log.info("Category created with id: {}", saved.getId());
        return categoryMapper.toDto(saved);
    }

    @Override
    @Transactional
    public CategoryDto updateCategory(Long catId, CategoryDto categoryDto) {
        log.info("Updating category id: {}", catId);

        Category existing = categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Category with id " + catId + " not found"));

        if (categoryDto.getName() != null && !categoryDto.getName().isBlank()) {
            existing.setName(categoryDto.getName());
        }

        Category updated = categoryRepository.save(existing);
        log.info("Category updated: {}", updated.getId());
        return categoryMapper.toDto(updated);
    }

    @Override
    @Transactional
    public void deleteCategory(Long catId) {
        log.info("Deleting category id: {}", catId);

        if (!categoryRepository.existsById(catId)) {
            throw new NotFoundException("Category with id " + catId + " not found");
        }

        // TODO: проверить, что нет событий, привязанных к категории
        categoryRepository.deleteById(catId);
        log.info("Category deleted: {}", catId);
    }

    @Override
    public List<CategoryDto> getCategories(int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size);

        return categoryRepository.findAll(pageable).stream()
                .map(categoryMapper::toDto)
                .toList();
    }

    @Override
    public CategoryDto getCategoryById(Long catId) {
        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Category with id " + catId + " not found"));

        return categoryMapper.toDto(category);
    }
}