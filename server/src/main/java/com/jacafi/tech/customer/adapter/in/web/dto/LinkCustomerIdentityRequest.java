package com.jacafi.tech.customer.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LinkCustomerIdentityRequest(
        @NotBlank @Size(max = 64) String subjectId) {}
