package com.sg.chatbot.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.sg.chatbot.service.ChatService;

import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/chatbot")
@CrossOrigin(originPatterns = "*", allowedHeaders = "*", allowCredentials = "true")
public class ChatController {

  @Autowired
  private ChatService chatService;

  @PostMapping("/messages")
  public Flux<ServerSentEvent<String>> streamLastMessage(@RequestBody String message) {
    return chatService.chatStream(message).map(mes -> ServerSentEvent.<String>builder()
        .data(mes)
        .build());
  }
}
