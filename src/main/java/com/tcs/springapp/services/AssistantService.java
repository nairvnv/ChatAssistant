package com.tcs.springapp.services;


import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.InsertOneResult;
import com.tcs.springapp.model.ChatMessage;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import static java.lang.String.format;

@Service
public class AssistantService {

    static MongoClient client = MongoClients.create("mongodb://localhost:27017");
    static MongoDatabase database = client.getDatabase("ChatApplication_TCS");
    static MongoCollection<Document> conversations = database.getCollection("Conversations");

    public static void insertAndSendMessage(SimpMessageSendingOperations messagingTemplate, @DestinationVariable String roomId, @Payload ChatMessage chatMessage){
        try {
            InsertOneResult result = conversations.insertOne(new Document()
                    .append("conversation_id", Integer.parseInt(roomId))
                    .append("sender", chatMessage.getSender())
                    .append("message", chatMessage.getContent())
                    .append("timestamp",System.currentTimeMillis()));
            System.out.println("Success! Inserted document id: " + result.getInsertedId());
        } catch (
                MongoException me) {
            System.err.println("Unable to insert message in collection due to an error: " + me);
        }

        messagingTemplate.convertAndSend(format("/channel/%s", roomId), chatMessage);

    }

    public static void addNewUser(SimpMessageSendingOperations messagingTemplate,@DestinationVariable String roomId, @Payload ChatMessage chatMessage,SimpMessageHeaderAccessor headerAccessor){
        String currentRoomId = (String) headerAccessor.getSessionAttributes().put("room_id", roomId);
        if (currentRoomId != null) {
            ChatMessage leaveMessage = new ChatMessage();
            leaveMessage.setType(ChatMessage.MessageType.LEAVE);
            leaveMessage.setSender(chatMessage.getSender());
            messagingTemplate.convertAndSend(format("/channel/%s", currentRoomId), leaveMessage);
        }
        headerAccessor.getSessionAttributes().put("username", chatMessage.getSender());
        messagingTemplate.convertAndSend(format("/channel/%s", roomId), chatMessage);

    }

}
