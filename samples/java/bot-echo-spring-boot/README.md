# Echo bot sample - Spring boot

## Library
https://github.com/auth0/java-jwt

## Prepare

```sh
docker compose up -d
docker compose exec workspace bash
```

## Build

```sh
./gradlew build
```

## Run

Run with API parameters

```sh
./gradlew bootRun \
    -Dworks.api.bot-id=BOT_ID \
    -Dworks.api.bot-secret=BOT_SECRET
    -Dworks.api.client-id=CLIENT_ID \
    -Dworks.api.client-secret=CLIENT_SECRET \
    -Dworks.api.service-account=SERVICE_ACCOUNT \
    -Dworks.api.private-key==PRIVATE_KEY
```

or, edit each parameter in `application.properties` file and run.
