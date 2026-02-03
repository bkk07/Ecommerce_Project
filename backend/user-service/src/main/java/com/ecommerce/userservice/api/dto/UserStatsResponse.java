package com.ecommerce.userservice.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatsResponse {
    
    private long totalUsers;
    private long adminCount;
    private long userCount;
    private long emailVerified;
    private long phoneVerified;
    private NewUsersStats newUsers;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NewUsersStats {
        private long today;
        private long thisWeek;
        private long thisMonth;
        private long thisYear;
    }
}
