package com.ecommerce.userservice.service;

import com.ecommerce.userservice.api.dto.UserStatsResponse;
import com.ecommerce.userservice.domain.model.enums.Role;
import com.ecommerce.userservice.infrastructure.repository.JpaUserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminUserService {

    private final JpaUserRepo userRepository;

    /**
     * Get comprehensive user statistics for admin dashboard.
     * Results are cached for 60 seconds to improve performance.
     */
    @Cacheable(value = "userStats", unless = "#result == null")
    public UserStatsResponse getUserStats() {
        log.info("Fetching user statistics for admin dashboard");

        // Get total counts
        long totalUsers = userRepository.count();
        long adminCount = userRepository.countByRole(Role.ADMIN);
        long userCount = userRepository.countByRole(Role.USER);
        long emailVerified = userRepository.countByIsEmailVerifiedTrue();
        long phoneVerified = userRepository.countByIsPhoneVerifiedTrue();

        // Calculate time boundaries
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime startOfThisWeek = LocalDate.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .atStartOfDay();
        LocalDateTime startOfThisMonth = LocalDate.now()
                .with(TemporalAdjusters.firstDayOfMonth())
                .atStartOfDay();
        LocalDateTime startOfThisYear = LocalDate.now()
                .with(TemporalAdjusters.firstDayOfYear())
                .atStartOfDay();

        // Get time-based registration counts
        long usersToday = userRepository.countUsersRegisteredSince(startOfToday);
        long usersThisWeek = userRepository.countUsersRegisteredSince(startOfThisWeek);
        long usersThisMonth = userRepository.countUsersRegisteredSince(startOfThisMonth);
        long usersThisYear = userRepository.countUsersRegisteredSince(startOfThisYear);

        // Build response
        UserStatsResponse.NewUsersStats newUsersStats = UserStatsResponse.NewUsersStats.builder()
                .today(usersToday)
                .thisWeek(usersThisWeek)
                .thisMonth(usersThisMonth)
                .thisYear(usersThisYear)
                .build();

        return UserStatsResponse.builder()
                .totalUsers(totalUsers)
                .adminCount(adminCount)
                .userCount(userCount)
                .emailVerified(emailVerified)
                .phoneVerified(phoneVerified)
                .newUsers(newUsersStats)
                .build();
    }
}
