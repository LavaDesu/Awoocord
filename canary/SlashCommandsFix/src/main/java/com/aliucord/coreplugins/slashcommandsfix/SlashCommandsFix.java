/*
 * This file is part of Aliucord, an Android Discord client mod.
 * Copyright (c) 2021 Juby210 & Vendicated
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.coreplugins.slashcommandsfix;

import android.content.Context;

import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.entities.Plugin;

import de.robv.android.xposed.XposedBridge;

@AliucordPlugin
public final class SlashCommandsFix extends Plugin {
    public SlashCommandsFix() {
        super();
    }

    @Override
    public void start(Context context) throws Throwable {
        if (ConflictCheck.run(context)) return;

        XposedBridge.makeClassInheritable(com.discord.models.commands.Application.class);
        XposedBridge.makeClassInheritable(com.discord.models.commands.RemoteApplicationCommand.class);

        new Patches(this.logger).loadPatches(context);
    }

    @Override
    public void stop(Context context) {}
}
