package com.aksenov.vladimir.gallery.entities

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.io.Serializable

@JsonIgnoreProperties(ignoreUnknown = true)
data class Image(var id: String,
                 var page: Int? = null,
                 var description: String? = null,
                 var instagram_username: String? = null,
                 var user: SplashUser? = null,
                 var portfolio_url: String? = null,
                 var color: String? = null,
                 var urls: Urls? = null) : Serializable

@JsonIgnoreProperties(ignoreUnknown = true)
data class SplashUser(val name: String? = null, val username: String? = null, val bio: String? = null,
                      val total_photos: Int?, val profile_image: ProfileImage? = null) : Serializable

@JsonIgnoreProperties(ignoreUnknown = true)
data class ProfileImage(val large: String? = null) : Serializable

@JsonIgnoreProperties(ignoreUnknown = true)
data class Urls(
        var raw: String? = null,
        var localCoverPath: String? = null,
        var full: String? = null,
        var regular: String? = null,
        var small: String? = null,
        var thumb: String? = null) : Serializable