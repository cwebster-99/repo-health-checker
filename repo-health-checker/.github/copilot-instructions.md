# Project Conventions for repo-health-checker

## Logging
- Use SLF4J logger (`LoggerFactory.getLogger()`), never `System.out.println` for logging.

## Documentation
- All public methods must have Javadoc with `@param` and `@return`.

## Data Transfer Objects
- Use Java records for data transfer objects, not POJOs.

## Null Handling
- Prefer `Optional<T>` over returning `null`.

## Testing
- Use AssertJ `assertThat()` in tests, not JUnit `assertEquals`.

## Code Style
- Follow Google Java Style Guide.

## Error Handling
- Exception messages should be descriptive and include the failing input value.

## Constants
- Use constants for magic numbers, never inline literal values in business logic.

## Deprecation
- When removing deprecated code, also remove all associated tests, imports, and references.

