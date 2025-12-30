package com.example.energy20.network

import com.example.energy20.data.GitHubRelease
import okhttp3.OkHttpClient
import okhttp3.Request
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GitHubApiService(private val client: OkHttpClient) {
    
    private val gson = Gson()
    
    companion object {
        private const val GITHUB_API_BASE = "https://api.github.com"
        private const val REPO_OWNER = "dannica2k"
        private const val REPO_NAME = "Energy-2.0-App"
        // GitHub Personal Access Token for private repo access
        private const val GITHUB_TOKEN = "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"
    }
    
    suspend fun getLatestRelease(): Result<GitHubRelease> = withContext(Dispatchers.IO) {
        try {
            val url = "$GITHUB_API_BASE/repos/$REPO_OWNER/$REPO_NAME/releases/latest"
            
            val request = Request.Builder()
                .url(url)
                .addHeader("Accept", "application/vnd.github.v3+json")
                .addHeader("Authorization", "Bearer $GITHUB_TOKEN")
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("Failed to fetch release: ${response.code}")
                )
            }
            
            val body = response.body?.string() 
                ?: return@withContext Result.failure(Exception("Empty response body"))
            
            val release = gson.fromJson(body, GitHubRelease::class.java)
            Result.success(release)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getAllReleases(): Result<List<GitHubRelease>> = withContext(Dispatchers.IO) {
        try {
            val url = "$GITHUB_API_BASE/repos/$REPO_OWNER/$REPO_NAME/releases"
            
            val request = Request.Builder()
                .url(url)
                .addHeader("Accept", "application/vnd.github.v3+json")
                .addHeader("Authorization", "Bearer $GITHUB_TOKEN")
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("Failed to fetch releases: ${response.code}")
                )
            }
            
            val body = response.body?.string() 
                ?: return@withContext Result.failure(Exception("Empty response body"))
            
            val type = object : TypeToken<List<GitHubRelease>>() {}.type
            val releases = gson.fromJson<List<GitHubRelease>>(body, type)
            Result.success(releases)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
