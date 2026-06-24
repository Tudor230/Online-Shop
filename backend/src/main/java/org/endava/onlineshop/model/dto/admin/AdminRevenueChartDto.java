package org.endava.onlineshop.model.dto.admin;

import java.time.LocalDate;
import java.math.BigDecimal;

public record AdminRevenueChartDto(
        LocalDate date,
        BigDecimal revenue
) {
}
