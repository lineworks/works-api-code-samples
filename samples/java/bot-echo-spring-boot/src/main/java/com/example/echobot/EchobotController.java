package com.example.echobot;

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

    public EchobotController(WorksApiParameters worksApiParameters) {
        this.worksApiParameters = worksApiParameters;
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
        WorksApi.Event event = WorksApi.convertToEvent(eventBody);
        if (event == null) {
            return ResponseEntity.badRequest().body(null);
        }

        // Get Access Token
        // TODO:InMemory
        String accessToken = null;
        try {
            accessToken = WorksApi.getAccessToken(
                    worksApiParameters.clientId(),
                    worksApiParameters.clientSecret(),
                    worksApiParameters.serviceAccount(),
                    worksApiParameters.privateKey(),
                    "bot"
                    );
        } catch (IOException e) {
            logger.error(e.getMessage());
            return ResponseEntity.badRequest().body(null);
        }

        if (accessToken == null) {
            logger.warn("Access Token is null.");
            return ResponseEntity.badRequest().body(null);
        }
        logger.info(accessToken);

        // Handle event
        if (event instanceof WorksApi.MessageEvent messageEvent) {
            logger.info(String.format("%s", messageEvent.content()));

            WorksApi.MessageContent messageContent = messageEvent.content();
            WorksApi.EventSource eventSource = messageEvent.source();
            String userId = eventSource.userId();

            WorksApi.MessageRequest messageRequest = null;

            if (messageContent instanceof WorksApi.TextMessageContent textMessage) {
                logger.info("text");
                messageRequest = new WorksApi.MessageRequest(textMessage);
            } else if (messageContent instanceof WorksApi.StickerMessageContent stickerMessage) {
                logger.info("sticker");
                messageRequest = new WorksApi.MessageRequest(stickerMessage);
            } else if (messageContent instanceof WorksApi.LocationMessageContent locationMessage) {
                logger.info("location");
                messageRequest = new WorksApi.MessageRequest(new WorksApi.TextMessageContent("text", locationMessage.toString()));
            } else if (messageContent instanceof WorksApi.ImageMessageContent imageMessage) {
                logger.info("image");
                messageRequest = new WorksApi.MessageRequest(imageMessage);
            } else if (messageContent instanceof WorksApi.FileMessageContent fileMessage) {
                logger.info("file");
                messageRequest = new WorksApi.MessageRequest(fileMessage);
            }

            if (messageRequest == null) {
                logger.warn("Invalid message type.");
                return ResponseEntity.badRequest().body(null);
            }

            // send message
            try {
                logger.info(String.format("Send message: %s", messageRequest));
                WorksApi.sendMessageToUser(messageRequest, botIdHeader, userId, accessToken);
            } catch (IOException e) {
                // TODO:retry
                logger.error(e.getMessage());
                return ResponseEntity.badRequest().body(null);
            }

            logger.info("done");
        } else {
            logger.error("Unknown message type");
            return ResponseEntity.badRequest().body(null);
        }

        return ResponseEntity.ok(null);
    }
}

