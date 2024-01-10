package com.sg.chatbot.service;

import org.springframework.http.codec.ServerSentEvent;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiTokenizer;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Service
public class ChatService {

  private String openaiApiKey = "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";
  private Assistant assistant;
  private StreamingAssistant streamingAssistant;

  interface Assistant {
    String chat(String message);
  }

  interface StreamingAssistant {
    TokenStream chat(String message);
  }

  public ChatService(){
    if (openaiApiKey == null) {
      System.err
          .println("ERROR: OPENAI_API_KEY environment variable is not set. Please set it to your OpenAI API key.");
    }

    var memory = TokenWindowChatMemory.withMaxTokens(2000, new OpenAiTokenizer("gpt-3.5-turbo"));

    assistant = AiServices.builder(Assistant.class)
        .chatLanguageModel(OpenAiChatModel.withApiKey(openaiApiKey))
        .chatMemory(memory)
        .build();

    streamingAssistant = AiServices.builder(StreamingAssistant.class)
        .streamingChatLanguageModel(OpenAiStreamingChatModel.withApiKey(openaiApiKey))
        .chatMemory(memory)
        .build();
  }

  public String chat(String message) {
    System.out.println(message);
    return assistant.chat(message);
  }

  public Flux<ServerSentEvent<String>> chatStream(String message) {
    Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();

    streamingAssistant.chat(message)
        .onNext(sink::tryEmitNext)
        .onComplete(c -> sink.tryEmitComplete())
        .onError(sink::tryEmitError)
        .start();

    return sink.asFlux().map(mes -> ServerSentEvent.<String>builder()
        .event("chat")      
        .data(mes)
        .build());
  }
}