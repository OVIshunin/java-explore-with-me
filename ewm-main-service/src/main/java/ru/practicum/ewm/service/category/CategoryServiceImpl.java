package ru.practicum.ewm.service.category;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.category.CategoryDto;
import ru.practicum.ewm.dto.category.NewCategoryDto;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.CategoryMapper;
import ru.practicum.ewm.model.Category;
import ru.practicum.ewm.repository.CategoryRepository;
import ru.practicum.ewm.repository.EventRepository;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional
    public CategoryDto createCategory(NewCategoryDto dto) {
        log.info("Creating category: {}", dto);

        if (categoryRepository.existsByName(dto.getName())) {
            throw new ConflictException("Category with name '" + dto.getName() + "' already exists");
        }

        Category category = categoryMapper.toEntity(dto);
        Category savedCategory = categoryRepository.save(category);

        log.info("Category created with id: {}", savedCategory.getId());
        return categoryMapper.toDto(savedCategory);
    }

    @Override
    @Transactional
    public CategoryDto updateCategory(Long catId, CategoryDto dto) {
        log.info("Updating category with id: {}, new data: {}", catId, dto);

        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Category with id " + catId + " not found"));

        // Проверяем, что новое имя не занято другой категорией
        if (!category.getName().equals(dto.getName()) &&
                categoryRepository.existsByName(dto.getName())) {
            throw new ConflictException("Category with name '" + dto.getName() + "' already exists");
        }

        category.setName(dto.getName());
        Category updatedCategory = categoryRepository.save(category);

        log.info("Category with id {} updated", catId);
        return categoryMapper.toDto(updatedCategory);
    }

    @Override
    @Transactional
    public void deleteCategory(Long catId) {
        log.info("Deleting category with id: {}", catId);

        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Category with id " + catId + " not found"));

        // Проверяем, есть ли события с этой категорией
        if (eventRepository.existsByCategoryId(catId)) {
            throw new ConflictException("Cannot delete category with existing events");
        }

        categoryRepository.deleteById(catId);
        log.info("Category with id {} deleted", catId);
    }

    @Override
    public List<CategoryDto> getCategories(Integer from, Integer size) {
        log.info("Getting categories with from: {}, size: {}", from, size);

        Pageable pageable = PageRequest.of(from / size, size);
        return categoryRepository.findAll(pageable).getContent().stream()
                .map(categoryMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public CategoryDto getCategoryById(Long catId) {
        log.info("Getting category by id: {}", catId);

        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Category with id " + catId + " not found"));

        return categoryMapper.toDto(category);
    }
}