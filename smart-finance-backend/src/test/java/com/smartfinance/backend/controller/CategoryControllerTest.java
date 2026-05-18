package com.smartfinance.backend.controller;

import com.smartfinance.backend.config.SecurityConfig;
import com.smartfinance.backend.dto.category.CategoryRequest;
import com.smartfinance.backend.dto.category.CategoryResponse;
import com.smartfinance.backend.model.CategoryType;
import com.smartfinance.backend.repository.UserRepository;
import com.smartfinance.backend.service.CategoryService;
import com.smartfinance.backend.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
@Import(SecurityConfig.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private static final String AUTH_HEADER = "Bearer test-token";

    private final CategoryResponse salaryCategory = new CategoryResponse(
            1L, "Salario", CategoryType.INCOME, "wallet", "#22C55E", false, Instant.now(), Instant.now()
    );

    private final CategoryResponse foodCategory = new CategoryResponse(
            2L, "Alimentación", CategoryType.EXPENSE, "utensils", "#F59E0B", false, Instant.now(), Instant.now()
    );

    @BeforeEach
    void setUp() {
        Claims mockClaims = org.mockito.Mockito.mock(Claims.class);
        when(mockClaims.getSubject()).thenReturn("1");
        when(jwtService.parseAccessToken(any())).thenReturn(mockClaims);
        when(userRepository.existsById(1L)).thenReturn(true);
    }

    @Test
    void getCategoriesReturns200WithListOfCategories() throws Exception {
        when(categoryService.getCategories(null)).thenReturn(List.of(salaryCategory, foodCategory));

        mockMvc.perform(get("/api/categories").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Salario"))
                .andExpect(jsonPath("$[1].name").value("Alimentación"));
    }

    @Test
    void getCategoriesByTypeReturns200WithFilteredCategories() throws Exception {
        when(categoryService.getCategories(CategoryType.INCOME)).thenReturn(List.of(salaryCategory));

        mockMvc.perform(get("/api/categories?type=INCOME").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("INCOME"));
    }

    @Test
    void createCategoryReturns201WhenValid() throws Exception {
        CategoryRequest request = new CategoryRequest("Freelance", CategoryType.INCOME, "laptop", "#3B82F6");
        CategoryResponse response = new CategoryResponse(
                10L, "Freelance", CategoryType.INCOME, "laptop", "#3B82F6", false, Instant.now(), Instant.now()
        );
        when(categoryService.createCategory(any(CategoryRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.name").value("Freelance"));
    }

    @Test
    void createCategoryReturns400WhenNameIsBlank() throws Exception {
        CategoryRequest request = new CategoryRequest("", CategoryType.INCOME, "icon", "#3B82F6");

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateCategoryReturns200WhenUpdatingOwnCategory() throws Exception {
        CategoryRequest request = new CategoryRequest("Salario Actualizado", CategoryType.INCOME, "wallet", "#22C55E");
        CategoryResponse response = new CategoryResponse(
                1L, "Salario Actualizado", CategoryType.INCOME, "wallet", "#22C55E", false, Instant.now(), Instant.now()
        );
        when(categoryService.updateCategory(eq(1L), any(CategoryRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/categories/1")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Salario Actualizado"));
    }

    @Test
    void updateCategoryReturns403WhenUpdatingAnotherUsersCategory() throws Exception {
        CategoryRequest request = new CategoryRequest("Hack", CategoryType.INCOME, "icon", "#000000");
        when(categoryService.updateCategory(eq(99L), any(CategoryRequest.class)))
                .thenThrow(new AccessDeniedException("No tienes permisos sobre esta categoría"));

        mockMvc.perform(put("/api/categories/99")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteCategoryReturns204WhenDeletingOwnCategory() throws Exception {
        mockMvc.perform(delete("/api/categories/1")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteCategoryReturns403WhenDeletingSystemCategory() throws Exception {
        doThrow(new AccessDeniedException("No se pueden modificar categorías del sistema"))
                .when(categoryService).deleteCategory(1L);

        mockMvc.perform(delete("/api/categories/1")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void getCategoriesReturns401WithoutAuthToken() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isForbidden());
    }
}
