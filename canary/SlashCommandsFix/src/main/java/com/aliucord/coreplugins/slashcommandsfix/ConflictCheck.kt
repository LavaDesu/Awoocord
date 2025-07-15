package com.aliucord.coreplugins.slashcommandsfix

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import com.aliucord.*
import com.aliucord.fragments.ConfirmDialog
import java.io.File
import kotlin.system.exitProcess

object ConflictCheck {
    @SuppressLint("SetTextI18n")
    @JvmStatic
    fun run(context: Context): Boolean {
        val hasFix = PluginManager.plugins.containsKey("SlashCommandsFix")
        val hasForcedFix = PluginManager.plugins.containsKey("ForceSlashCommandsFixNOW")
        val fromStorage = Main.settings.getBool("AC_from_storage", false)

        if (hasFix) {
            Logger("SlashCommandsFixBeta").warn("conflict detected")
            if (hasForcedFix || fromStorage) {
                Utils.threadPool.execute {
                    Thread.sleep(5000) // wait for app to load guh
                    Utils.mainThread.post {
                        val dialog = ConfirmDialog()
                        dialog
                            .setTitle("SlashCommandsFix Conflict")
                            .setDescription("You have another variant of SlashCommandsFix installed. Do you want to disable it?")
                            .setIsDangerous(true)
                            .setOnOkListener {
                                File(context.codeCacheDir, "Aliucord.zip").delete()
                                if (fromStorage)
                                    Main.settings.setBool("AC_from_storage", false)
                                if (hasForcedFix)
                                    PluginManager.disablePlugin("ForceSlashCommandsFixNOW")
                                val ctx = it.context
                                val intent = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)
                                Utils.appActivity.startActivity(Intent.makeRestartActivityTask(intent!!.component))
                                exitProcess(0)
                            }
                            .apply { isCancelable = false }
                            .show(Utils.appActivity.supportFragmentManager, "SlashCommandsFix conflict")
                    }
                }
            } else {
                Logger("SlashCommandsFixBeta").warn("removing myself... bye!")
                File("${Constants.PLUGINS_PATH}/SlashCommandsFixBeta.zip").delete()
            }
        }

        return hasFix
    }
}
