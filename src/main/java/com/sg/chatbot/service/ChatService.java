package com.sg.chatbot.service;

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

  public Flux<String> chatStream(String message) {
    Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();

    streamingAssistant.chat(message)
        .onNext(sink::tryEmitNext)
        .onComplete(c -> sink.tryEmitComplete())
        .onError(sink::tryEmitError)
        .start();

    return sink.asFlux();
  }
}