package com.example.echobot;

import com.example.echobot.worksapi.WorksApi;
import com.example.echobot.worksapi.WorksApiModel;
import com.example.echobot.worksapi.exception.WorksApiUnauthorizedException;
import com.example.echobot.worksapi.exception.WorksApiBadRequestException;
import com.example.echobot.worksapi.exception.WorksApiOverRateLimitException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

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

import org.apache.logging.log4j.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;


@RestController
public class EchobotController {

    private Logger logger = LoggerFactory.getLogger(EchobotController.class);
    private WorksApiParameters worksApiParameters;

    @Autowired
    private SharedMemory sharedMemory;

    public EchobotController(WorksApiParameters worksApiParameters) {
        this.worksApiParameters = worksApiParameters;
    }

    private String getAccessToken() throws WorksApiBadRequestException {
        String accessToken = WorksApi.getAccessToken(
                worksApiParameters.clientId(),
                worksApiParameters.clientSecret(),
                worksApiParameters.serviceAccount(),
                worksApiParameters.privateKey(),
                "bot"
                );
        sharedMemory.put("access_token", accessToken);
        return accessToken;
    }

    @GetMapping("/")
    public String hello(@RequestParam(value = "name", defaultValue = "World") String name) {
      return String.format("Hello %s!", name);
    }

    @PostMapping(path="/callback", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> callback(
            @RequestBody String eventBody,
            @RequestHeader("X-WORKS-BotId") String botIdHeader,
            @RequestHeader("X-WORKS-Signature") String signatureHeader
            ) {
        logger.info(String.format("event: %s", eventBody));
        logger.info(String.format("botId: %s", botIdHeader));
        logger.info(String.format("sig: %s", signatureHeader));


        // Bot Id check
        if (!botIdHeader.equals(worksApiParameters.botId())) {
            logger.warn("BotId NG");
            return ResponseEntity.badRequest().body(null);
        } else {
            logger.info("BotId OK");
        }

        // Signature check
        if (!WorksApi.validateRequest(eventBody, signatureHeader, worksApiParameters.botSecret())) {
            logger.warn("Signature NG");
            return ResponseEntity.badRequest().body(null);
        } else {
            logger.info("Signature OK");
        }

        // Mapping
        WorksApiModel.Event event = WorksApi.convertToEvent(eventBody);
        if (event == null) {
            return ResponseEntity.badRequest().body(null);
        }

        // Get Access Token
        String accessToken = sharedMemory.get("access_token");
        if (accessToken == null) {
            logger.info("Refrech access token");
            try {
                accessToken = getAccessToken();
            } catch (WorksApiBadRequestException e) {
                logger.error(e.getMessage());
                return ResponseEntity.badRequest().body(null);
            }
        } else {
            logger.info("Use cache of access token");
        }


        // Handle event
        if (event instanceof WorksApiModel.MessageEvent messageEvent) {
            logger.info(String.format("%s", messageEvent.content()));

            WorksApiModel.MessageContent messageContent = messageEvent.content();
            WorksApiModel.EventSource eventSource = messageEvent.source();
            String userId = eventSource.userId();

            WorksApiModel.MessageRequest messageRequest = null;

            if (messageContent instanceof WorksApiModel.TextMessageContent textMessage) {
                logger.info("text");
                messageRequest = new WorksApiModel.MessageRequest(textMessage);
            } else if (messageContent instanceof WorksApiModel.StickerMessageContent stickerMessage) {
                logger.info("sticker");
                messageRequest = new WorksApiModel.MessageRequest(stickerMessage);
            } else if (messageContent instanceof WorksApiModel.LocationMessageContent locationMessage) {
                logger.info("location");
                messageRequest = new WorksApiModel.MessageRequest(new WorksApiModel.TextMessageContent("text", locationMessage.address()));
            } else if (messageContent instanceof WorksApiModel.ImageMessageContent imageMessage) {
                logger.info("image");
                messageRequest = new WorksApiModel.MessageRequest(imageMessage);
            } else if (messageContent instanceof WorksApiModel.FileMessageContent fileMessage) {
                logger.info("file");
                messageRequest = new WorksApiModel.MessageRequest(fileMessage);
            }

            if (messageRequest == null) {
                logger.warn("Invalid message type.");
                return ResponseEntity.badRequest().body(null);
            }

            // send message
            for (int attempt = 0; attempt < 2; attempt++) {
                try {
                    logger.info(String.format("Send message: %s", messageRequest));
                    WorksApi.sendMessageToUser(messageRequest, botIdHeader, userId, accessToken);
                    break;
                } catch (WorksApiUnauthorizedException e) {
                    // Access Token has been exipred
                    // Update
                    logger.info("Refrech access token");
                    try {
                        accessToken = getAccessToken();
                    } catch (WorksApiBadRequestException er) {
                        logger.error(er.getMessage());
                        return ResponseEntity.badRequest().body(null);
                    }
                } catch (WorksApiBadRequestException e) {
                    logger.error(e.getMessage());
                    return ResponseEntity.badRequest().body(null);
                } catch (WorksApiOverRateLimitException e) {
                    logger.error(e.getMessage());
                    return ResponseEntity.badRequest().body(null);
                }
                logger.info("Retry");
            }

            logger.info("done");
        } else {
            logger.error("Unknown message type");
            return ResponseEntity.badRequest().body(null);
        }

        return ResponseEntity.ok(null);
    }
}
