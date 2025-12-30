package com.example.energy20.data

import com.google.gson.annotations.SerializedName

data class GitHubRelease(
    @SerializedName("tag_name")
    val tagName: String,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("body")
    val body: String?,
    
    @SerializedName("published_at")
    val publishedAt: String,
    
    @SerializedName("assets")
    val assets: List<GitHubAsset>,
    
    @SerializedName("prerelease")
    val prerelease: Boolean
)

data class GitHubAsset(
    @SerializedName("name")
    val name: String,
    
    @SerializedName("browser_download_url")
    val downloadUrl: String,
    
    @SerializedName("size")
    val size: Long,
    
    @SerializedName("content_type")
    val contentType: String
)
