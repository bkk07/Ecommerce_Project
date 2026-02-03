package com.ecommerce.ratingservice.mapper;

import com.ecommerce.ratingservice.dto.EligibilityResponse;
import com.ecommerce.ratingservice.dto.RatingResponse;
import com.ecommerce.ratingservice.entity.Rating;
import com.ecommerce.ratingservice.entity.RatingEligibility;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * MapStruct mapper for converting between entities and DTOs.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RatingMapper {

    /**
     * Convert Rating entity to RatingResponse DTO
     */
    RatingResponse toRatingResponse(Rating rating);

    /**
     * Convert list of Rating entities to list of RatingResponse DTOs
     */
    List<RatingResponse> toRatingResponseList(List<Rating> ratings);

    /**
     * Convert RatingEligibility entity to EligibilityResponse DTO
     */
    EligibilityResponse toEligibilityResponse(RatingEligibility eligibility);

    /**
     * Convert list of RatingEligibility entities to list of EligibilityResponse DTOs
     */
    List<EligibilityResponse> toEligibilityResponseList(List<RatingEligibility> eligibilities);
}
