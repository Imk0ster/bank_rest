package com.example.bankcards.dto;

import lombok.Data;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Data
public class CreateCardRequest {
    @NotBlank
    private String cardNumber;

    @NotBlank
    private String cardHolderName;

    @NotBlank
    @Pattern(regexp = "(0[1-9]|1[0-2])/[0-9]{2}")
    private String expiryDate; // MM/YY

    private BigDecimal initialBalance;
}