package com.voipgrid.vialer.api;

/**
 * Created by stefan on 17-11-15.
 */
public class PreviousRequestNotFinishedException extends Exception
{
    public PreviousRequestNotFinishedException(String message)
    {
        super(message);
    }
}