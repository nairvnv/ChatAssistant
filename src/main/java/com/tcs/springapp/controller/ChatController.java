package com.tcs.springapp.controller;


import com.mongodb.BasicDBObject;
import com.mongodb.MongoException;
import com.mongodb.client.*;

import com.mongodb.client.result.*;
import com.tcs.springapp.services.*;
import org.bson.Document;

import java.io.IOException;
import java.util.*;


import static java.lang.String.format;

import com.tcs.springapp.model.ChatMessage;
import com.tcs.springapp.model.ChatMessage.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.tcs.springapp.services.AssistantService;

@Controller
public class ChatController {

	@Autowired
	private SimpMessageSendingOperations messagingTemplate;

	MongoClient client = MongoClients.create("mongodb://localhost:27017");
	MongoDatabase database = client.getDatabase("ChatApplication_TCS");
	MongoCollection<Document> conversations = database.getCollection("Conversations");


	@RequestMapping(value="/login",method = RequestMethod.POST)
	@ResponseBody
	public Document login(@RequestBody int uid){
		return(LoginService.login(uid));
	}



	@MessageMapping("/chat/{roomId}/sendMessage")
	public void sendMessage(@DestinationVariable String roomId, @Payload ChatMessage chatMessage) throws IOException {
		AssistantService.insertAndSendMessage(messagingTemplate,roomId,chatMessage);
		if(!chatMessage.getSender().equals("IHG Assistant") && !chatMessage.getSender().equals("Admin")){
			AssistantService.insertAndSendMessage(messagingTemplate,roomId,SmartService.smartResponse(chatMessage));
		}
	}


	@MessageMapping("/chat/{roomId}/addUser")
	public void addUser(@DestinationVariable String roomId, @Payload ChatMessage chatMessage,
						SimpMessageHeaderAccessor headerAccessor) {
		AssistantService.addNewUser(messagingTemplate,roomId,chatMessage,headerAccessor);
	}


	@RequestMapping(value="/getBookingDetails",method = RequestMethod.POST)
	@ResponseBody
	public Document getBookingDetails(@RequestBody int bookingId){
		return(BookingService.getStatus(bookingId));
	}



	@RequestMapping(value="/cancelBooking",method = RequestMethod.POST)
	@ResponseBody
	public String cancelBooking(@RequestBody int bookingId){
		return(BookingService.cancel(bookingId));
	}
}
