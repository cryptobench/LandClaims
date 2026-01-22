package com.easyclaims.commands;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.easyclaims.EasyClaims;
import com.easyclaims.commands.subcommands.*;
import com.easyclaims.commands.subcommands.admin.AdminSubcommand;

/**
 * Main command for EasyClaims plugin.
 * Uses Hytale's subcommand system so all commands appear in /help.
 *
 * Subcommands:
 *   gui/map               - Open chunk visualizer GUI
 *   settings              - Manage trusted players GUI
 *   claim                 - Claim current chunk
 *   unclaim               - Unclaim current chunk
 *   unclaimall            - Unclaim all your chunks
 *   list/claims           - List all your claims
 *   trust <player> [level]- Trust a player
 *   untrust <player>      - Remove trust from a player
 *   trustlist             - List trusted players
 *   playtime/stats        - Show your playtime and claim slots
 *   admin                 - Admin commands (requires easyclaims.admin)
 */
public class EasyClaimsCommand extends AbstractCommandCollection {

    public EasyClaimsCommand(EasyClaims plugin) {
        super("easyclaims", "Land claiming and protection commands");
        requirePermission("easyclaims.use");

        // Register all subcommands - these will appear in /help
        addSubCommand(new GuiSubcommand(plugin));
        addSubCommand(new SettingsSubcommand(plugin));
        addSubCommand(new ClaimSubcommand(plugin));
        addSubCommand(new UnclaimSubcommand(plugin));
        addSubCommand(new UnclaimAllSubcommand(plugin));
        addSubCommand(new ListSubcommand(plugin));
        addSubCommand(new TrustSubcommand(plugin));
        addSubCommand(new UntrustSubcommand(plugin));
        addSubCommand(new TrustListSubcommand(plugin));
        addSubCommand(new PlaytimeSubcommand(plugin));
        addSubCommand(new AdminSubcommand(plugin));
    }
}
