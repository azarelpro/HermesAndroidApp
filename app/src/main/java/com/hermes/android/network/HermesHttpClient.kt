package com.hermes.android.network

import com.hermes.android.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import kotlinx.serialization.encodeToString

class HermesHttpClient(
    private val okHttpClient: OkHttpClient,
    private val json: Json
) {
    private val baseUrl: String = ""

    suspend fun login(baseUrl: String, username: String, password: String): Result<AuthLoginResponse> = withContext(Dispatchers.IO) {
        try {
            val loginUrl = "$baseUrl/auth/password-login"
            val requestBody = json.encodeToString(
                AuthLoginRequest(provider = "basic", username = username, password = password)
            )
            val request = Request.Builder()
                .url(loginUrl)
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val loginResponse = json.decodeFromString<AuthLoginResponse>(responseBody)
                    val cookies = extractCookies(response)
                    Result.success(loginResponse.copy(cookies = cookies))
                } else {
                    Result.failure(Exception("Login failed: ${response.code} - $responseBody"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getWsTicket(baseUrl: String, cookies: Map<String, String>): Result<WsTicketResponse> = withContext(Dispatchers.IO) {
        try {
            val ticketUrl = "$baseUrl/api/auth/ws-ticket"
            val cookieHeader = cookies.entries.map { "${it.key}=${it.value}" }.joinToString("; ")
            val requestBody = json.encodeToString(WsTicketRequest(cookies))
            val request = Request.Builder()
                .url(ticketUrl)
                .addHeader("Cookie", cookieHeader)
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    Result.success(json.decodeFromString<WsTicketResponse>(responseBody))
                } else {
                    Result.failure(Exception("Ticket request failed: ${response.code} - $responseBody"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractCookies(response: Response): Map<String, String> {
        val cookies = mutableMapOf<String, String>()
        val setCookieHeaders = response.headers("Set-Cookie")
        for (header in setCookieHeaders) {
            val parts = header.split(";")
            if (parts.isNotEmpty()) {
                val pair = parts[0]
                val separator = pair.indexOf('=')
                if (separator > 0) {
                    val name = pair.substring(0, separator).trim()
                    val value = pair.substring(separator + 1).trim().removeSurrounding("\"")
                    if (name.isNotEmpty()) cookies[name] = value
                }
            }
        }
        return cookies
    }
}