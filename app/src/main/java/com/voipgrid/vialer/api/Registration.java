package com.voipgrid.vialer.api;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * Registration API interface.
 *
 * API calls for registration on the VoipGrid middleware
 */
public interface Registration {

    @FormUrlEncoded
    @POST("api/call-response/")
    Call<ResponseBody> reply(@Field("unique_key") String token, @Field("available") boolean isAvailable,
               @Field("message_start_time") String messageStartTime);

    @DELETE("api/gcm-device/")
    Call<ResponseBody> unregister(@Query("token") String token, @Query("sip_user_id") String sipId,
                      @Query("app") String app);

    @FormUrlEncoded
    @POST("api/gcm-device/")
    Call<ResponseBody> register(@Field("name") String name, @Field("token") String token,
                    @Field("sip_user_id") String sipUserId, @Field("os_version") String osVersion,
                    @Field("client_version") String clientVersion, @Field("app") String app);
}
