package com.example.tbmapp

object Routes {
    const val HOME = "home"
    const val DETAIL = "detail/{stationId}"

    fun detail(stationId: Int) = "detail/$stationId"
}