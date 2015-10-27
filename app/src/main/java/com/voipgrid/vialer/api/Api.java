package com.voipgrid.vialer.api;

import com.voipgrid.vialer.api.models.AutoLoginToken;
import com.voipgrid.vialer.api.models.CallRecord;
import com.voipgrid.vialer.api.models.TwoStepCallStatus;
import com.voipgrid.vialer.api.models.MobileNumber;
import com.voipgrid.vialer.api.models.PhoneAccount;
import com.voipgrid.vialer.api.models.SelectedUserDestinationParams;
import com.voipgrid.vialer.api.models.SystemUser;
import com.voipgrid.vialer.api.models.UserDestination;
import com.voipgrid.vialer.api.models.VoipGridResponse;
import com.voipgrid.vialer.models.ClickToDialParams;
import com.voipgrid.vialer.models.PasswordResetParams;

import retrofit.Callback;
import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;
import retrofit.http.Query;

/**
 * API interface
 */
public interface Api {

    @GET("/api/autologin/token/")
    void autoLoginToken(Callback<AutoLoginToken> callback);

    @POST("/api/mobileapp/")
    TwoStepCallStatus twoStepCall(@Body ClickToDialParams params);

    @GET("/api/mobileapp/{call_id}")
    TwoStepCallStatus twoStepCall(@Path("call_id") String callId);

    @GET("/api/permission/systemuser/profile/")
    void systemUser(Callback<SystemUser> callback);

    @GET("/api/permission/systemuser/profile/")
    SystemUser systemUser();

    @GET("/api/phoneaccount/basic/phoneaccount/{account}/")
    void phoneAccount(@Path("account") String accountId, Callback<PhoneAccount> callback);

    @GET("/api/phoneaccount/basic/phoneaccount/{account}/")
    PhoneAccount phoneAccount(@Path("account") String accountId);

    //@PUT("/api/permission/systemuser/profile/")
    @PUT("/api/permission/mobile_number/")
    void mobileNumber(@Body MobileNumber mobileNumber, Callback<MobileNumber> callback);

    @POST("/api/permission/password_reset/")
    void resetPassword(@Body PasswordResetParams params, Callback<Response> callback);

    @GET("/api/cdr/record/")
    void getRecentCalls(@Query("limit") int limit, @Query("offset") int offset, @Query("call_date__gt") String date, Callback<VoipGridResponse<CallRecord>> callback);

    @GET("/api/userdestination")
    void getUserDestination(Callback<VoipGridResponse<UserDestination>> callback);

    @PUT("/api/selecteduserdestination/{id}/")
    void setSelectedUserDestination(@Path("id") String id, @Body SelectedUserDestinationParams params, Callback<Response> callback);
}
