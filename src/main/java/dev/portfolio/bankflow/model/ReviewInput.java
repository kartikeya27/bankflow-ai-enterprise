package dev.portfolio.bankflow.model;

import jakarta.validation.constraints.*;
public record ReviewInput(
    @NotBlank @Pattern(regexp = "APPROVE|REJECT") String decision,
    @NotBlank @Size(min = 4, max = 400) String note
) {}
