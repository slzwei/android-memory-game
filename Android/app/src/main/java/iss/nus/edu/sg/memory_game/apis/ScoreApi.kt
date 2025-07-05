package iss.nus.edu.sg.memory_game.apis

import retrofit2.Call
import iss.nus.edu.sg.memory_game.dao.ScoreRequest
import iss.nus.edu.sg.memory_game.dao.ScoreResult
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ScoreApi {
    @POST("api/score/add")
    fun addScore(@Body scoreRequest: ScoreRequest): Call<Void>

    @GET("api/score/topFive")
    fun getTopFive(): Call<List<ScoreResult>>
}
