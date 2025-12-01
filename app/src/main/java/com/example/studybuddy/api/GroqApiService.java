package com.example.studybuddy.api;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface GroqApiService {
    String BASE_URL = "https://api.groq.com/";
    
    @POST("openai/v1/chat/completions")
    retrofit2.Call<GroqChatResponse> chatCompletion(
        @Body GroqChatRequest request
    );
    
    class Factory {
        public static GroqApiService create(String apiKey) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            
            OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    okhttp3.Request original = chain.request();
                    okhttp3.Request.Builder requestBuilder = original.newBuilder()
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "application/json");
                    okhttp3.Request request = requestBuilder.build();
                    return chain.proceed(request);
                })
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
            
            Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
            
            return retrofit.create(GroqApiService.class);
        }
    }
}

