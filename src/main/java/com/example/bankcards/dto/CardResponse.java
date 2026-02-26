package com.example.bankcards.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CardResponse {
    private Long id;
    private String maskedNumber;
    private String cardHolderName;
    private String expirationDate;
    private String status;
    private BigDecimal balance;
}