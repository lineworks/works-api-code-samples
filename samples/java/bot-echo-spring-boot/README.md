# Echo bot sample - Spring boot
Sample bot application based on Spring Boot.

## Using libraries
- https://spring.io/projects/spring-boot
- https://github.com/auth0/java-jwt
- https://github.com/square/okhttp

See [build.gradle](./build.gradle)

## Usage
### Workspace with Docker

```sh
docker compose up -d
docker compose exec workspace bash
```

## Run
Run using Gradle with API parameters

```sh
./gradlew bootRun \
    -Dworks.api.bot-id=BOT_ID \
    -Dworks.api.bot-secret=BOT_SECRET
    -Dworks.api.client-id=CLIENT_ID \
    -Dworks.api.client-secret=CLIENT_SECRET \
    -Dworks.api.service-account=SERVICE_ACCOUNT \
    -Dworks.api.private-key==PRIVATE_KEY
```

or, edit each parameter in [application.properties](./src/main/resources/application.properties) file and run.

## Question / Issue report
See root [README.md](../../../README.md)
