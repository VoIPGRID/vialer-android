package com.voipgrid.vialer.api;


import retrofit.Callback;
import retrofit.http.DELETE;
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
    void reply(@Field("unique_key") String token, @Field("available") boolean isAvailable,
               @Field("message_start_time") String messageStartTime, Callback<Object> callback);

    @FormUrlEncoded
    @DELETE("/api/gcm-device/")
    void unregister(@Field("token") String token, @Field("sip_user_id") String sipId,
                    Callback<Object> callback);

    @FormUrlEncoded
    @DELETE("/api/gcm-device/")
    Object unregister(@Field("token") String token, @Field("sip_user_id") String sipId,
                      @Field("app") String app);

    @FormUrlEncoded
    @POST("/api/gcm-device/")
    Object register(@Field("name") String name, @Field("token") String token,
                    @Field("sip_user_id") String sipUserId, @Field("os_version") String osVersion,
                    @Field("client_version") String clientVersion, @Field("app") String app);

}
