package com.example.codex;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface JDoodleService {
    @POST("execute")
    Call<JDoodleResponse> execute(@Body JDoodleRequest req);
}

