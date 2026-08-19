package com.ameya.inventory.dto.category;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public class ItemCategoryDtos {

    public record Request(
            @NotBlank(message = "Name is required") String name,
            Long parentCategoryId,
            boolean active
    ) {
    }

    public record Response(
            Long id,
            String name,
            Long parentCategoryId,
            String parentCategoryName,
            boolean active,
            List<AttributeDefDtos.Response> attributes
    ) {
    }
}
