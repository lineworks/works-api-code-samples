package com.example.echobot.worksapi;

import com.example.echobot.worksapi.exception.WorksApiJsonProcessException;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

public class WorksApiModel {
    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            property = "type",
            visible = true
        )
    @JsonSubTypes({
            @JsonSubTypes.Type(value = MessageEvent.class, name = "message")
        })
    public sealed interface Event permits MessageEvent {
        String type();
    }

    public record ErrorResponse(
            @JsonProperty("code") String code,
            @JsonProperty("description") String description
            ){};

    public record MessageEvent(
            @JsonProperty("type") String type,
            @JsonProperty("source") EventSource source,
            @JsonProperty("issuedTime") java.util.Date issuedTime,
            @JsonProperty("content") MessageContent content
            ) implements Event {}

    public record MessageRequest(
            @JsonProperty("content") MessageContent content
            ) {}

    public record EventSource(
            @JsonProperty("userId") String userId,
            @JsonProperty("channelId") String channelId,
            @JsonProperty("domainId") String domainId
            ) {}

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            property = "type",
            visible = true
        )
    @JsonSubTypes({
            @JsonSubTypes.Type(value = TextMessageContent.class, name = "text"),
            @JsonSubTypes.Type(value = StickerMessageContent.class, name = "sticker"),
            @JsonSubTypes.Type(value = LocationMessageContent.class, name = "location"),
            @JsonSubTypes.Type(value = ImageMessageContent.class, name = "image"),
            @JsonSubTypes.Type(value = FileMessageContent.class, name = "file")
        })
    public sealed interface MessageContent
            permits TextMessageContent, StickerMessageContent, LocationMessageContent, ImageMessageContent, FileMessageContent {
        String type();
    }

    public record TextMessageContent(
            @JsonProperty("type") String type,
            @JsonProperty("text") String text
            ) implements MessageContent{}

    public record StickerMessageContent(
            @JsonProperty("type") String type,
            @JsonProperty("packageId") String packageId,
            @JsonProperty("stickerId") String stickerId
            ) implements MessageContent{}

    public record LocationMessageContent(
            @JsonProperty("type") String type,
            @JsonProperty("address") String address,
            @JsonProperty("latitude") Float latitude,
            @JsonProperty("longitude") Float longitude
            ) implements MessageContent{}


    public record ImageMessageContent(
            @JsonProperty("type") String type,
            @JsonProperty("fileId") String fileId
            ) implements MessageContent{}

    public record FileMessageContent(
            @JsonProperty("type") String type,
            @JsonProperty("fileId") String fileId
            ) implements MessageContent{}

    public record AccessTokenRequest(
            @JsonProperty("assertion") String assertion,
            @JsonProperty("grant_type") String grantType,
            @JsonProperty("client_id") String clientId,
            @JsonProperty("client_secret") String clientSecret,
            @JsonProperty("scope") String scope
            ){}

    public record AccessTokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("refresh_token") String refreshToken,
            @JsonProperty("token_type") String tokenType,
            @JsonProperty("expires_in") String expiresIn,
            @JsonProperty("scope") String scope
            ){}

    public static <T> T JsonToObject(String value, Class<T> valueType) throws WorksApiJsonProcessException {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(value, valueType);
        } catch (JsonProcessingException e) {
            throw new WorksApiJsonProcessException(e.getMessage());
        }
    }

    public static <T> String ObjectToJson(T value)  throws WorksApiJsonProcessException {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new WorksApiJsonProcessException(e.getMessage());
        }
    }

}
