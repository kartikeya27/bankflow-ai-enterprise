package dev.portfolio.bankflow.model;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CaseInput(
    @NotBlank @Size(max = 40) @Pattern(regexp = "[A-Za-z0-9_-]+") String reference,
    @NotNull @DecimalMin("0.01") @DecimalMax("100000000.00") BigDecimal amount,
    @NotBlank @Pattern(regexp = "USD") String currency,
    boolean newBeneficiary,
    boolean sanctionsHit,
    boolean accountMismatch,
    @Size(max = 500) String narrative
) {}
