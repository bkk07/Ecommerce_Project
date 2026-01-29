package com.ecommerce.searchservice.dto;

import com.ecommerce.feigndtos.ProductResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SearchResponse {
    private Page<ProductResponse> products;
    private Map<String, Long> categoryFacets;
    private Map<String, Long> brandFacets;
}
