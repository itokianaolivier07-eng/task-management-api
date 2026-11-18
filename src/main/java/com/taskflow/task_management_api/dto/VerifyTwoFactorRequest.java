package com.taskflow.task_management_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyTwoFactorRequest(
        @NotBlank String challengeToken,
        @NotBlank @Pattern(regexp = "\\d{6}", message = "6 chiffres attendus") String code) {}
