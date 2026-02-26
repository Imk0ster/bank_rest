package com.example.bankcards.util;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    private final UserRepository userRepository;
    private final CardRepository cardRepository;
    private final PasswordEncoder passwordEncoder;
    private final EncryptionUtil encryptionUtil;

    @Override
    public void run(String... args) {
        if (!userRepository.findByUsername("admin").isPresent()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setEmail("admin@bank.com");
            admin.setRole("ROLE_ADMIN");
            admin.setEnabled(true);
            admin.setCreatedAt(LocalDateTime.now());
            userRepository.save(admin);
            System.out.println("Admin created: admin / admin123");
        }

        if (!userRepository.findByUsername("user").isPresent()) {
            User user = new User();
            user.setUsername("user");
            user.setPassword(passwordEncoder.encode("user123"));
            user.setEmail("user@bank.com");
            user.setRole("ROLE_USER");
            user.setEnabled(true);
            user.setCreatedAt(LocalDateTime.now());
            userRepository.save(user);

            Card card1 = new Card();
            card1.setEncryptedNumber(encryptionUtil.encrypt("1234567890123456"));
            card1.setLastFourDigits("3456");
            card1.setCardHolderName("TEST USER");
            card1.setExpirationDate(LocalDate.now().plusYears(2));
            card1.setStatus("ACTIVE");
            card1.setBalance(new BigDecimal("1000.00"));
            card1.setOwner(user);
            card1.setCreatedAt(LocalDateTime.now());
            cardRepository.save(card1);

            Card card2 = new Card();
            card2.setEncryptedNumber(encryptionUtil.encrypt("9876543210987654"));
            card2.setLastFourDigits("7654");
            card2.setCardHolderName("TEST USER");
            card2.setExpirationDate(LocalDate.now().plusYears(3));
            card2.setStatus("ACTIVE");
            card2.setBalance(new BigDecimal("500.00"));
            card2.setOwner(user);
            card2.setCreatedAt(LocalDateTime.now());
            cardRepository.save(card2);

            System.out.println("User created: user / user123 with 2 cards");
        }
    }
}