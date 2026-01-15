package com.easyclaims.gui;

import com.easyclaims.data.PlayerClaims;
import com.easyclaims.data.TrustLevel;
import com.easyclaims.data.TrustedPlayer;
import com.easyclaims.managers.ClaimManager;
import com.easyclaims.managers.PlaytimeManager;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * GUI for viewing and managing claim settings and trusted players.
 */
public class ClaimSettingsGui extends InteractiveCustomUIPage<ClaimSettingsGui.SettingsData> {

    private final ClaimManager claimManager;
    private final PlaytimeManager playtimeManager;
    private String playerNameInput = "";
    private String statusMessage = "";
    private boolean statusIsError = true;
    private int requestingConfirmation = -1;
    private TrustLevel selectedTrustLevel = TrustLevel.BUILD;

    public ClaimSettingsGui(@Nonnull PlayerRef playerRef, ClaimManager claimManager, PlaytimeManager playtimeManager) {
        super(playerRef, CustomPageLifetime.CanDismiss, SettingsData.CODEC);
        this.claimManager = claimManager;
        this.playtimeManager = playtimeManager;
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull SettingsData data) {
        super.handleDataEvent(ref, store, data);

        var playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) {
            this.sendUpdate();
            return;
        }

        UUID playerId = playerRef.getUuid();

        // Handle player name input change
        if (data.playerName != null) {
            this.playerNameInput = data.playerName;
            this.statusMessage = "";
        }

        // Handle cancel/close button
        if (data.cancel != null) {
            this.close();
            return;
        }

        // Handle checkbox selection to set trust level
        if (data.selectTrust != null) {
            TrustLevel level = TrustLevel.fromString(data.selectTrust);
            if (level != null) {
                this.selectedTrustLevel = level;
                this.statusMessage = "";
            }
            // Force full UI refresh to show checkbox state
            UICommandBuilder commandBuilder = new UICommandBuilder();
            UIEventBuilder eventBuilder = new UIEventBuilder();
            this.build(ref, commandBuilder, eventBuilder, store);
            this.sendUpdate(commandBuilder, eventBuilder, true);
            return;
        }

        // Handle Add button
        if (data.addTrust != null) {
            if (!playerNameInput.isEmpty()) {
                addTrustedPlayer(playerId, playerNameInput, selectedTrustLevel);
            } else {
                statusMessage = "Enter a player name first!";
                statusIsError = true;
            }
            // Force full UI refresh
            UICommandBuilder commandBuilder = new UICommandBuilder();
            UIEventBuilder eventBuilder = new UIEventBuilder();
            this.build(ref, commandBuilder, eventBuilder, store);
            this.sendUpdate(commandBuilder, eventBuilder, true);
            return;
        }

        // Handle remove action
        if (data.removeButtonAction != null) {
            String[] parts = data.removeButtonAction.split(":");
            String action = parts[0];
            int index = Integer.parseInt(parts[1]);

            if (action.equals("Click")) {
                this.requestingConfirmation = index;
            }
            if (action.equals("Delete")) {
                PlayerClaims claims = claimManager.getPlayerClaims(playerId);
                List<UUID> trustedIds = new ArrayList<>(claims.getTrustedPlayersMap().keySet());
                if (index >= 0 && index < trustedIds.size()) {
                    UUID targetId = trustedIds.get(index);
                    String targetName = claims.getTrustedPlayersMap().get(targetId).getName();
                    claimManager.removeTrust(playerId, targetId);
                    statusMessage = "Removed " + targetName;
                    statusIsError = false;
                }
                this.requestingConfirmation = -1;
            }

            UICommandBuilder commandBuilder = new UICommandBuilder();
            UIEventBuilder eventBuilder = new UIEventBuilder();
            this.build(ref, commandBuilder, eventBuilder, store);
            this.sendUpdate(commandBuilder, eventBuilder, true);
            return;
        }

        this.sendUpdate();
    }

    private void addTrustedPlayer(UUID ownerId, String targetName, TrustLevel level) {
        UUID targetId = null;
        String resolvedName = targetName;

        // First try online player
        PlayerRef targetPlayer = Universe.get().getPlayerByUsername(targetName, NameMatching.EXACT_IGNORE_CASE);
        if (targetPlayer != null) {
            targetId = targetPlayer.getUuid();
            resolvedName = targetPlayer.getUsername();
        } else {
            // Try UUID
            try {
                targetId = UUID.fromString(targetName);
            } catch (IllegalArgumentException e) {
                statusMessage = "Player not found: " + targetName;
                statusIsError = true;
                return;
            }
        }

        // Can't trust yourself
        if (targetId.equals(ownerId)) {
            statusMessage = "You can't trust yourself!";
            statusIsError = true;
            return;
        }

        // Check if already trusted
        PlayerClaims claims = claimManager.getPlayerClaims(ownerId);
        TrustLevel existingLevel = claims.getTrustLevel(targetId);
        if (existingLevel != TrustLevel.NONE) {
            claimManager.addTrust(ownerId, targetId, resolvedName, level);
            statusMessage = "Updated " + resolvedName + " to " + level.getDescription();
            statusIsError = false;
        } else {
            claimManager.addTrust(ownerId, targetId, resolvedName, level);
            statusMessage = "Trusted " + resolvedName + " with " + level.getDescription();
            statusIsError = false;
        }

        playerNameInput = "";
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder uiCommandBuilder,
                      @Nonnull UIEventBuilder uiEventBuilder, @Nonnull Store<EntityStore> store) {
        uiCommandBuilder.append("Pages/cryptobench_EasyClaims_Settings.ui");

        var playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) return;

        UUID playerId = playerRef.getUuid();
        PlayerClaims claims = claimManager.getPlayerClaims(playerId);

        // Set player name input value and bind change event
        uiCommandBuilder.set("#PlayerNameField.Value", playerNameInput);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#PlayerNameField",
                EventData.of("@PlayerName", "#PlayerNameField.Value"), false);

        // Bind Add button
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#AddButton",
                EventData.of("AddTrust", "true"), false);

        // Set permission checkboxes based on selected trust level
        uiCommandBuilder.set("#UseBlocksSetting #CheckBox.Value", selectedTrustLevel == TrustLevel.USE);
        uiCommandBuilder.set("#ContainerSetting #CheckBox.Value", selectedTrustLevel == TrustLevel.CONTAINER);
        uiCommandBuilder.set("#WorkstationSetting #CheckBox.Value", selectedTrustLevel == TrustLevel.WORKSTATION);
        uiCommandBuilder.set("#BuildSetting #CheckBox.Value", selectedTrustLevel == TrustLevel.BUILD);

        // Bind checkboxes to select trust level
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#UseBlocksSetting #CheckBox",
                EventData.of("SelectTrust", "use"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#ContainerSetting #CheckBox",
                EventData.of("SelectTrust", "container"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#WorkstationSetting #CheckBox",
                EventData.of("SelectTrust", "workstation"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#BuildSetting #CheckBox",
                EventData.of("SelectTrust", "build"), false);

        // Set status message - show selected trust level if no other message
        if (!statusMessage.isEmpty()) {
            uiCommandBuilder.set("#StatusMessage.Text", statusMessage);
            String color = statusIsError ? "#ff5555" : "#55ff55";
            uiCommandBuilder.set("#StatusMessage.Style.TextColor", color);
        } else {
            uiCommandBuilder.set("#StatusMessage.Text", "Selected: " + selectedTrustLevel.getDescription());
            uiCommandBuilder.set("#StatusMessage.Style.TextColor", "#aaaaaa");
        }

        // Build trusted players list
        Map<UUID, TrustedPlayer> trustedMap = claims.getTrustedPlayersMap();
        List<UUID> trustedIds = new ArrayList<>(trustedMap.keySet());

        for (int i = 0; i < trustedIds.size(); i++) {
            UUID trustedId = trustedIds.get(i);
            TrustedPlayer tp = trustedMap.get(trustedId);

            uiCommandBuilder.append("#TrustedEntries", "Pages/cryptobench_EasyClaims_TrustedEntry.ui");
            uiCommandBuilder.set("#TrustedEntries[" + i + "] #PlayerName.Text", tp.getName());

            // Set trust level with color based on level
            TrustLevel level = tp.getLevel();
            uiCommandBuilder.set("#TrustedEntries[" + i + "] #TrustLevel.Text", level.getDescription());

            // Color code by trust level
            String bgColor, outlineColor;
            switch (level) {
                case USE:
                    bgColor = "#4a9c4a83";
                    outlineColor = "#4a9c4ade";
                    break;
                case CONTAINER:
                    bgColor = "#4a4a9c83";
                    outlineColor = "#4a4a9cde";
                    break;
                case WORKSTATION:
                    bgColor = "#9c9c4a83";
                    outlineColor = "#9c9c4ade";
                    break;
                case DAMAGE:
                    bgColor = "#9c6a4a83";
                    outlineColor = "#9c6a4ade";
                    break;
                case BUILD:
                    bgColor = "#9c4a4a83";
                    outlineColor = "#9c4a4ade";
                    break;
                default:
                    bgColor = "#1a8dec83";
                    outlineColor = "#1a8decde";
            }
            uiCommandBuilder.set("#TrustedEntries[" + i + "] #TrustLevel.Background.Color", bgColor);
            uiCommandBuilder.set("#TrustedEntries[" + i + "] #TrustLevel.OutlineColor", outlineColor);

            // Handle remove button
            if (this.requestingConfirmation == i) {
                uiCommandBuilder.set("#TrustedEntries[" + i + "] #RemoveButton.Text", "Are you sure?");
                uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                        "#TrustedEntries[" + i + "] #RemoveButton",
                        EventData.of("RemoveButtonAction", "Delete:" + i), false);
                uiEventBuilder.addEventBinding(CustomUIEventBindingType.MouseExited,
                        "#TrustedEntries[" + i + "] #RemoveButton",
                        EventData.of("RemoveButtonAction", "Click:-1"), false);
            } else {
                uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                        "#TrustedEntries[" + i + "] #RemoveButton",
                        EventData.of("RemoveButtonAction", "Click:" + i), false);
            }
        }

        // Close button
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton",
                EventData.of("Cancel", "true"), false);
    }

    /**
     * Data class for handling GUI events.
     */
    public static class SettingsData {
        static final String KEY_PLAYER_NAME = "@PlayerName";
        static final String KEY_CANCEL = "Cancel";
        static final String KEY_REMOVE_BUTTON_ACTION = "RemoveButtonAction";
        static final String KEY_ADD_TRUST = "AddTrust";
        static final String KEY_SELECT_TRUST = "SelectTrust";

        public static final BuilderCodec<SettingsData> CODEC = BuilderCodec.<SettingsData>builder(SettingsData.class, SettingsData::new)
                .addField(new KeyedCodec<>(KEY_PLAYER_NAME, Codec.STRING),
                        (data, s) -> data.playerName = s,
                        data -> data.playerName)
                .addField(new KeyedCodec<>(KEY_CANCEL, Codec.STRING),
                        (data, s) -> data.cancel = s,
                        data -> data.cancel)
                .addField(new KeyedCodec<>(KEY_REMOVE_BUTTON_ACTION, Codec.STRING),
                        (data, s) -> data.removeButtonAction = s,
                        data -> data.removeButtonAction)
                .addField(new KeyedCodec<>(KEY_ADD_TRUST, Codec.STRING),
                        (data, s) -> data.addTrust = s,
                        data -> data.addTrust)
                .addField(new KeyedCodec<>(KEY_SELECT_TRUST, Codec.STRING),
                        (data, s) -> data.selectTrust = s,
                        data -> data.selectTrust)
                .build();

        private String playerName;
        private String cancel;
        private String removeButtonAction;
        private String addTrust;
        private String selectTrust;
    }
}
