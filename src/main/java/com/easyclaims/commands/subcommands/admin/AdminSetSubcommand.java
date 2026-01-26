package com.easyclaims.commands.subcommands.admin;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.easyclaims.EasyClaims;
import com.easyclaims.commands.subcommands.admin.set.*;

/**
 * Admin set command collection for changing server claim settings.
 * Uses subcommands pattern to match Hytale's native command structure.
 *
 * Usage:
 *   /easyclaims admin set             - Show available settings
 *   /easyclaims admin set starting <value>  - Set starting claims
 *   /easyclaims admin set perhour <value>   - Set claims per hour
 *   /easyclaims admin set max <value>       - Set maximum claims
 *   /easyclaims admin set buffer <value>    - Set buffer zone size
 */
public class AdminSetSubcommand extends AbstractCommandCollection {

    public AdminSetSubcommand(EasyClaims plugin) {
        super("set", "Change server claim settings");
        requirePermission("easyclaims.admin");

        // Register setting subcommands
        addSubCommand(new SetStartingSubcommand(plugin));
        addSubCommand(new SetPerHourSubcommand(plugin));
        addSubCommand(new SetMaxSubcommand(plugin));
        addSubCommand(new SetBufferSubcommand(plugin));
        addSubCommand(new SetAllowPvpToggleSubcommand(plugin));
        addSubCommand(new SetShowClaimsOnMapSubcommand(plugin));
        addSubCommand(new SetMapTextScaleSubcommand(plugin));
        addSubCommand(new SetMapTextModeSubcommand(plugin));
        addSubCommand(new SetMapTextGroupingSubcommand(plugin));
    }
}
