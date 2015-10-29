package com.voipgrid.vialer.api;


import retrofit.Callback;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.POST;

/**
 * Registration API interface.
 *
 * API calls for registration on the VoipGrid middleware
 */
public interface Registration {

    @FormUrlEncoded
    @POST("/")
    void reply(@Field("unique_key") String token, @Field("available") boolean isAvailable, Callback<Object> callback);

    @FormUrlEncoded
    @POST("/api/unregister-gcm-device/")
    void unregister(@Field("token") String token, @Field("sip_user_id") String sipId, Callback<Object> callback);

    @FormUrlEncoded
    @POST("/api/unregister-gcm-device/")
    Object unregister(@Field("token") String token, @Field("sip_user_id") String sipId);

    @FormUrlEncoded
    @POST("/api/register-gcm-device/")
    Object register(@Field("name") String name, @Field("token") String token,
                    @Field("sip_user_id") String sipUserId, @Field("os_version") String osVersion,
                    @Field("client_version") String clientVersion, @Field("app") String app);

}
