package com.easyclaims.commands.subcommands.admin;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.easyclaims.EasyClaims;

/**
 * Admin command collection for EasyClaims.
 * Contains all admin subcommands.
 */
public class AdminSubcommand extends AbstractCommandCollection {

    public AdminSubcommand(EasyClaims plugin) {
        super("admin", "Admin commands for claim management");
        requirePermission("easyclaims.admin");

        // Register admin subcommands
        addSubCommand(new AdminGuiSubcommand(plugin));
        addSubCommand(new AdminConfigSubcommand(plugin));
        addSubCommand(new AdminSetSubcommand(plugin));
        addSubCommand(new AdminReloadSubcommand(plugin));
        addSubCommand(new AdminUnclaimSubcommand(plugin));
        addSubCommand(new AdminFakeClaimSubcommand(plugin));
    }
}
