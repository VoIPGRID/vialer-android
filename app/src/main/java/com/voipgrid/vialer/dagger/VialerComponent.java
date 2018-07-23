package com.voipgrid.vialer.dagger;

import com.voipgrid.vialer.VialerApplication;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {VialerModule.class})
public interface VialerComponent {
    void inject(VialerApplication app);
}
