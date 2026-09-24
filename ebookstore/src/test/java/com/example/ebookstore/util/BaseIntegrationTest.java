package com.example.ebookstore.util;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base class for all integration tests.
 * @Transactional ensures each test rolls back, leaving a clean H2 DB.
 * @ActiveProfiles("test") picks up application-test.properties if present,
 * otherwise falls back to the default H2 in-memory config.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public abstract class BaseIntegrationTest {
}
