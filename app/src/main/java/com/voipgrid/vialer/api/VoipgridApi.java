package com.voipgrid.vialer.api;

import com.voipgrid.vialer.api.models.ApiTokenRequest;
import com.voipgrid.vialer.api.models.ApiTokenResponse;
import com.voipgrid.vialer.api.models.AutoLoginToken;
import com.voipgrid.vialer.api.models.CallRecord;
import com.voipgrid.vialer.api.models.MobileNumber;
import com.voipgrid.vialer.api.models.PasswordChangeParams;
import com.voipgrid.vialer.api.models.PhoneAccount;
import com.voipgrid.vialer.api.models.SelectedUserDestinationParams;
import com.voipgrid.vialer.api.models.SystemUser;
import com.voipgrid.vialer.api.models.TwoStepCallStatus;
import com.voipgrid.vialer.api.models.UpdateVoIPAccountParameters;
import com.voipgrid.vialer.api.models.UserDestination;
import com.voipgrid.vialer.api.models.VoipGridResponse;
import com.voipgrid.vialer.models.ClickToDialParams;
import com.voipgrid.vialer.models.PasswordResetParams;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * API interface
 */
public interface VoipgridApi {

    @GET("api/autologin/token/")
    Call<AutoLoginToken> autoLoginToken();

    @POST("api/permission/apitoken/")
    Call<ApiTokenResponse> apiToken(@Body ApiTokenRequest apiTokenRequest);

    @POST("api/mobileapp/")
    Call<TwoStepCallStatus> twoStepCall(@Body ClickToDialParams params);

    @GET("api/mobileapp/{call_id}/")
    Call<TwoStepCallStatus> twoStepCall(@Path("call_id") String callId);

    @DELETE("api/mobileapp/{call_id}/")
    Call<Object> twoStepCallCancel(@Path("call_id") String callId);

    @GET("api/permission/systemuser/profile/")
    Call<SystemUser> systemUser();

    @GET("api/phoneaccount/basic/phoneaccount/{account}/")
    Call<PhoneAccount> phoneAccount(@Path("account") String accountId);

    @PUT("api/permission/mobile_number/")
    Call<MobileNumber> mobileNumber(@Body MobileNumber mobileNumber);

    @PUT("api/mobile/profile/")
    Call<UpdateVoIPAccountParameters> updateVoipAccount(@Body UpdateVoIPAccountParameters updateVoIPAccountParameters);

    @POST("api/permission/password_reset/")
    Call<Void> resetPassword(@Body PasswordResetParams params);
    @GET("api/cdr/record/")
    Call<VoipGridResponse<CallRecord>> getRecentCalls(@Query("limit") int limit,
            @Query("offset") int offset,
            @Query("call_date__gt") String from,
            @Query("call_date__lt") String to);

    @GET("api/cdr/record/personalized/")
    Call<VoipGridResponse<CallRecord>> getRecentCallsForLoggedInUser(@Query("limit") int limit,
            @Query("offset") int offset,
            @Query("call_date__gt") String from,
            @Query("call_date__lt") String to);

    @GET("api/userdestination/")
    Call<VoipGridResponse<UserDestination>> fetchDestinations();

    @PUT("api/selecteduserdestination/{id}/")
    Call<Object> setSelectedUserDestination(@Path("id") String id,
                                    @Body SelectedUserDestinationParams params);
    @PUT("api/v2/password/")
    Call<PasswordChangeParams> passwordChange (@Body PasswordChangeParams passwordChangeParams);

}
