package com.voipgrid.vialer.api;

import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.HTTP;
import retrofit2.http.POST;

/**
 * Registration API interface.
 *
 * API calls for registration on the VoipGrid middleware
 */
public interface MiddlewareApi {


    @FormUrlEncoded
    @POST("api/call-response/")
    Call<ResponseBody> reply(@Field("unique_key") String token, @Field("available") boolean isAvailable,
               @Field("message_start_time") String messageStartTime);

    @FormUrlEncoded
    @HTTP(method = "DELETE", path = "api/android-device/", hasBody = true)
    Call<ResponseBody> unregister(@Field("token") String token, @Field("sip_user_id") String sipId,
                                  @Field("app") String app);

    @FormUrlEncoded
    @POST("api/android-device/")
    Call<ResponseBody> register(@Field("name") String name, @Field("token") String token,
                    @Field("sip_user_id") String sipUserId, @Field("os_version") String osVersion,
                    @Field("client_version") String clientVersion, @Field("app") String app, @Field("remote_logging_id") String remoteLoggingId);

    @POST("api/log-metrics/")
    Call<Void> metrics(@Body Map<String, String> metrics);
}
