package com.example.shortformvideofeed.data.remote

import retrofit2.http.GET
import retrofit2.http.Url

interface FeedApi {
    @GET
    suspend fun fetchFeed(@Url url: String): List<FeedDto>
}
