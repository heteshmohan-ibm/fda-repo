package com.example.ebookstore.exception;

import lombok.Value;

/**
 * Matches the OpenAPI ApiError schema exactly:
 * { "code": "...", "message": "..." }
 */
@Value
public class ApiError {
    String code;
    String message;
}
