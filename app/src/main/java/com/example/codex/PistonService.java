package com.example.codex;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface PistonService {

    @Headers("Content-Type: application/json")
    @GET("runtimes")
    Call<PistonRuntimesResponse> getRuntimes();

    @Headers("Content-Type: application/json")
    @POST("execute")
    Call<PistonExecuteResponse> execute(@Body PistonExecuteRequest request);
}