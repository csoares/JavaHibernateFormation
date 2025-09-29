package com.formation.hibernate.converter;

import com.formation.hibernate.dto.ProductDto;
import com.formation.hibernate.dto.ProductSummaryDto;
import com.formation.hibernate.dto.CategoryDto;
import com.formation.hibernate.entity.Product;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ProductConverter {

    public ProductConverter() {
    }

    public ProductDto toDto(Product product) {
        if (product == null) return null;

        ProductDto dto = new ProductDto(
            product.getId(),
            product.getName(),
            product.getDescription(),
            product.getPrice(),
            product.getStockQuantity()
        );

        if (product.getCategory() != null) {
            dto.setCategory(new CategoryDto(
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getCategory().getDescription()
            ));
        }

        return dto;
    }

    public ProductSummaryDto toSummaryDto(Product product) {
        if (product == null) return null;

        String categoryName = product.getCategory() != null ?
            product.getCategory().getName() : null;

        return new ProductSummaryDto(
            product.getId(),
            product.getName(),
            product.getPrice(),
            categoryName
        );
    }

    public List<ProductDto> toDtoList(List<Product> products) {
        if (products == null) return null;
        return products.stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    public List<ProductSummaryDto> toSummaryDtoList(List<Product> products) {
        if (products == null) return null;
        return products.stream()
            .map(this::toSummaryDto)
            .collect(Collectors.toList());
    }

    public Product toEntity(ProductDto dto) {
        if (dto == null) return null;

        Product product = new Product();
        product.setId(dto.getId());
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setStockQuantity(dto.getStockQuantity());

        // Note: Category será definida separadamente para evitar dependência circular

        return product;
    }
}