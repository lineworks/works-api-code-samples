package com.example.echobot;

import java.security.interfaces.RSAPrivateKey;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

import java.util.Date;
import java.util.Base64;

import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.JWT;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.FormBody;
import okhttp3.Response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;


public class WorksApi {
    final static String TOKEN_URL = "https://auth.worksmobile.com/oauth2/v2.0/token";
    final static String API_URL = "https://www.worksapis.com/v1.0";

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

    /**
     * Signature check
     */
    public static boolean validateRequest(String body, String signature, String botSecret) {
        boolean rst = false;
        try {
            SecretKeySpec key = new SecretKeySpec(botSecret.getBytes(), "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            byte[] source = body.getBytes();
            String targetSignature = Base64.getEncoder().encodeToString(mac.doFinal(source));

            rst = signature.equals(targetSignature);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        return rst;
    }

    /**
     * Convert body from String to Event object
     */
    public static Event convertToEvent(String body) {
        Event event = null;
        try {
            ObjectMapper mapper = new ObjectMapper();
            event = mapper.readValue(body, Event.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return event;
    }

    private static RSAPrivateKey getPrivateKeyFromString(String privateKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        String privateKeyContent = privateKey
                .replaceAll("\\n", "")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "");

        KeyFactory kf = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec keySpecPKCS8 = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyContent));
        return (RSAPrivateKey) kf.generatePrivate(keySpecPKCS8);
    }

    /**
     * Get JWT
     */
    private static String getJWT(String clientId, String serviceAccount, String privateKeyStr) {
        try {
            Date currentTime = new Date();
            Date expireTime = new Date();
            expireTime.setTime(currentTime.getTime() + 60 * 60);

            RSAPrivateKey privateKey = getPrivateKeyFromString(privateKeyStr);
            Algorithm algorithm = Algorithm.RSA256(null, privateKey);
            String token = JWT.create()
                    .withIssuer(clientId)
                    .withSubject(serviceAccount)
                    .withIssuedAt(currentTime)
                    .withExpiresAt(expireTime) // 60 minutes
                    .sign(algorithm);
            return token;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get Access Token
     */
    public static String getAccessToken(String clientId, String clientSecret, String serviceAccount, String privateKeyStr, String scope) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        OkHttpClient client = new OkHttpClient();

        // Get JWT
        String jwt = getJWT(clientId, serviceAccount, privateKeyStr);

        AccessTokenRequest reqObj = new AccessTokenRequest(jwt, "urn:ietf:params:oauth:grant-type:jwt-bearer", clientId, clientSecret, scope);

        RequestBody body = new FormBody.Builder()
            .add("assertion", reqObj.assertion())
            .add("grant_type", reqObj.grantType())
            .add("client_id", reqObj.clientId())
            .add("client_secret", reqObj.clientSecret())
            .add("scope", reqObj.scope())
            .build();

        Request request = new Request.Builder()
            .url(TOKEN_URL)
            .post(body)
            .build();

        try (Response response = client.newCall(request).execute()) {
            String resJson = response.body().string();
            if (!response.isSuccessful()) {
                throw new IOException(String.format("code: %s, message: %s", response.code(), resJson));
            }

            String accessToken = null;
            try {
                AccessTokenResponse resObj = null;
                resObj = mapper.readValue(resJson, AccessTokenResponse.class);
                accessToken = resObj.accessToken();
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            return accessToken;
        }
    }

    /**
     * Send message to user
     */
    public static void sendMessageToUser(MessageRequest messageRequest, String botId, String userId, String accessToken) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        OkHttpClient client = new OkHttpClient();

        String reqJson;
        try {
            reqJson = mapper.writeValueAsString(messageRequest);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new IOException(e);
        }

        String url = String.format("%s/bots/%s/users/%s/messages", API_URL, botId, userId);

        RequestBody body = RequestBody.create(reqJson, MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
            .url(url)
            .post(body)
            .header("Authorization", String.format("Bearer %s", accessToken))
            .build();

        // TODO: retry
        try (Response response = client.newCall(request).execute()) {
            String resJson = response.body().string();
            if (!response.isSuccessful()) {
                int statusCode = response.code();
                throw new IOException(String.format("code: %s, message: %s, %s, %s", statusCode, resJson, request, response));
            }
        }
    }
}
