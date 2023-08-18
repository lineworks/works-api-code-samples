# Echo bot sample - Spring boot
Sample bot application based on Spring Boot.

## Using libraries
- https://spring.io/projects/spring-boot
- https://github.com/auth0/java-jwt
- https://github.com/square/okhttp

See [build.gradle](./build.gradle)

## Usage
### Requirements

- JDK >= 17

### Prepare parameters
Create App and generate parameters on LINE WORKS Developer Console.
https://dev.worksmobile.com/jp/reference/authorization-sa?lang=ja


### Workspace with Docker

```sh
docker compose up -d
docker compose exec workspace bash
```

### Set env

```sh
export LW_API_BOT_ID=1111111
export LW_API_BOT_SECRET=xxxxxxxxxx
export LW_API_CLIENT_ID=xxxxxxxx
export LW_API_CLIENT_SECRET=xxxxxx
export LW_API_SERVICE_ACCOUNT=xxxxx@xxxxxxx
export LW_API_PRIVATEKEY="-----BEGIN PRIVATE KEY-----
xxxxxxxxxxxxxxxxxxxxxxxx
-----END PRIVATE KEY-----"
```

## Run
Run using Gradle with API parameters

```sh
./gradlew bootRun \
    -Dworks.api.bot-id="$LW_API_BOT_ID" \
    -Dworks.api.bot-secret="$LW_API_BOT_SECRET"
    -Dworks.api.client-id="$LW_API_CLIENT_ID" \
    -Dworks.api.client-secret="$LW_API_CLIENT_SECRET" \
    -Dworks.api.service-account="$LW_API_SERVICE_ACCOUNT" \
    -Dworks.api.private-key="$LW_API_PRIVATE_KEY"
```

or, edit each parameter in [application.properties](./src/main/resources/application.properties) file and run.

## Question / Issue report
See root [README.md](../../../README.md)
