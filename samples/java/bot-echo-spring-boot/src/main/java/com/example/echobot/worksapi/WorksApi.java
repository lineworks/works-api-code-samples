package com.example.echobot.worksapi;

import com.example.echobot.worksapi.exception.WorksApiUnauthorizedException;
import com.example.echobot.worksapi.exception.WorksApiBadRequestException;
import com.example.echobot.worksapi.exception.WorksApiOverRateLimitException;
import com.example.echobot.worksapi.exception.WorksApiJsonProcessException;

import java.security.interfaces.RSAPrivateKey;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

import java.util.Date;
import java.util.concurrent.TimeUnit;
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


public class WorksApi {
    final static String TOKEN_URL = "https://auth.worksmobile.com/oauth2/v2.0/token";
    final static String API_URL = "https://www.worksapis.com/v1.0";
    final static int RETRY_COUNT_MAX = 5;

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
    public static WorksApiModel.Event convertToEvent(String body) {
        WorksApiModel.Event event = null;
        try {
            event = WorksApiModel.JsonToObject(body, WorksApiModel.Event.class);
        } catch (WorksApiJsonProcessException e) {
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
    public static String getAccessToken(String clientId, String clientSecret, String serviceAccount, String privateKeyStr, String scope) throws WorksApiBadRequestException {
        OkHttpClient client = new OkHttpClient();

        // Get JWT
        String jwt = getJWT(clientId, serviceAccount, privateKeyStr);

        WorksApiModel.AccessTokenRequest reqObj = new WorksApiModel.AccessTokenRequest(jwt, "urn:ietf:params:oauth:grant-type:jwt-bearer", clientId, clientSecret, scope);

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

        try {
            Response response = client.newCall(request).execute();
            String resJson = response.body().string();
            if (!response.isSuccessful())
                throw new WorksApiBadRequestException(String.format("code: %s, message: %s", response.code(), resJson));

            String accessToken = null;
            WorksApiModel.AccessTokenResponse resObj = null;
            try {
                resObj = WorksApiModel.JsonToObject(resJson, WorksApiModel.AccessTokenResponse.class);
            } catch (WorksApiJsonProcessException e) {
                e.printStackTrace();
            }
            if (resObj == null)
                throw new WorksApiBadRequestException(String.format("code: %s, message: %s", response.code(), resJson));

            accessToken = resObj.accessToken();

            return accessToken;
        } catch (IOException e) {
            throw new WorksApiBadRequestException(e.getMessage());
        }
    }

    /**
     * Send message to user
     */
    public static void sendMessageToUser(WorksApiModel.MessageRequest messageRequest, String botId, String userId, String accessToken)
            throws WorksApiBadRequestException, WorksApiUnauthorizedException, WorksApiOverRateLimitException{
        OkHttpClient client = new OkHttpClient();

        String reqJson;
        try {
            reqJson = WorksApiModel.ObjectToJson(messageRequest);
        } catch (WorksApiJsonProcessException e) {
            e.printStackTrace();
            throw new WorksApiUnauthorizedException(e.getMessage());
        }

        String url = String.format("%s/bots/%s/users/%s/messages", API_URL, botId, userId);

        RequestBody body = RequestBody.create(reqJson, MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
            .url(url)
            .post(body)
            .header("Authorization", String.format("Bearer %s", accessToken))
            .build();

        boolean isSuccess = false;
        int statusCode = 0;
        Response response = null;
        String resJson = null;

        // request with retry
        int attempt = 0;
        while (attempt < RETRY_COUNT_MAX) {
            try {
                // request
                response = client.newCall(request).execute();

                resJson = response.body().string();
                isSuccess = response.isSuccessful();
                statusCode = response.code();
                if (isSuccess) {
                    break;
                } else if (statusCode == 403) {
                    try {
                        WorksApiModel.ErrorResponse err = WorksApiModel.JsonToObject(resJson, WorksApiModel.ErrorResponse.class);
                        if (err.code() == "UNAUTHORIZED")
                            throw new WorksApiUnauthorizedException(err.description());
                    } catch (WorksApiJsonProcessException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                // request error
            }

            attempt++;

            try {
                long backoffTime = (long) Math.pow(2, attempt);
                TimeUnit.SECONDS.sleep(backoffTime);
            } catch (InterruptedException e) {
                // sleep error
            }
        }

        if (!isSuccess) {
            String msg = String.format("code: %s, message: %s, %s, %s", statusCode, resJson, request, response);
            if (statusCode == 429) throw new WorksApiOverRateLimitException(msg);
            else throw new WorksApiBadRequestException(msg);
        }
    }
}
