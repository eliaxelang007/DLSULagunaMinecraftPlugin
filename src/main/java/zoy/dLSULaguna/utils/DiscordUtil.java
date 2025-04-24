package zoy.dLSULaguna.utils;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.JDA;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class DiscordUtil {

    private static DiscordSRV discordSRV;

    // Initialize method to check if DiscordSRV is available
    public static void initialize() {
        // Check if DiscordSRV plugin is enabled
        Plugin plugin = Bukkit.getPluginManager().getPlugin("DiscordSRV");
        if (plugin != null && plugin.isEnabled()) {
            discordSRV = (DiscordSRV) plugin;
            Bukkit.getLogger().info("DiscordSRV has been successfully enabled!");
        } else {
            Bukkit.getLogger().warning("DiscordSRV is not available or not enabled.");
        }
    }

    // Get the instance of DiscordSRV, check if it is available
    private static DiscordSRV getDiscordSRV() {
        if (discordSRV == null) {
            Bukkit.getLogger().warning("DiscordSRV is not available.");
            return null;
        }
        return discordSRV;
    }

    // Send a message to a specified Discord channel
    public static CompletableFuture<Message> sendMessage(String channelId, String message) {
        CompletableFuture<Message> future = new CompletableFuture<>();
        DiscordSRV dsrv = getDiscordSRV();
        if (dsrv == null) {
            future.complete(null);
            return future;
        }

        try {
            JDA jda = dsrv.getJda();
            if (jda == null) {
                Bukkit.getLogger().warning("DiscordSRV's JDA instance is not ready. Cannot send message to channel " + channelId);
                future.complete(null);
                return future;
            }
            TextChannel channel = jda.getTextChannelById(channelId);

            if (channel != null) {
                channel.sendMessage(message).queue(
                        future::complete,
                        error -> {
                            Bukkit.getLogger().log(Level.WARNING, "Failed to queue Discord message to channel " + channelId + ": " + error.getMessage(), error);
                            future.complete(null);
                        }
                );
            } else {
                Bukkit.getLogger().warning("Could not find Discord channel with ID: " + channelId + ". Message not sent.");
                future.complete(null);
            }
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.SEVERE, "An unexpected error occurred while trying to send a message to Discord channel " + channelId, e);
            future.complete(null);
        }

        return future;
    }

    // Edit a message in the specified Discord channel
    public static void editMessage(String channelId, long messageId, String newContent) {
        DiscordSRV dsrv = getDiscordSRV();
        if (dsrv == null) return;

        try {
            JDA jda = dsrv.getJda();
            if (jda == null) {
                Bukkit.getLogger().warning("DiscordSRV's JDA instance is not ready. Cannot edit Discord message at this time.");
                return;
            }
            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel != null) {
                channel.retrieveMessageById(messageId).queue(message -> {
                    if (message != null) {
                        message.editMessage(newContent).queue(
                                success -> {},
                                error -> Bukkit.getLogger().log(Level.WARNING, "Failed to edit Discord message " + messageId + ": " + error.getMessage(), error)
                        );
                    } else {
                        Bukkit.getLogger().warning("Could not find Discord message with ID: " + messageId + " in channel: " + channelId);
                    }
                }, error -> Bukkit.getLogger().log(Level.WARNING, "Failed to retrieve message " + messageId + ": " + error.getMessage()));
            } else {
                Bukkit.getLogger().warning("Could not find Discord channel with ID: " + channelId);
            }
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "Error editing message " + messageId + ": " + e.getMessage(), e);
        }
    }

    // Edit a message embed in the specified Discord channel
    public static void editMessage(String channelId, long messageId, MessageEmbed embed) {
        DiscordSRV dsrv = getDiscordSRV();
        if (dsrv == null) return;

        try {
            JDA jda = dsrv.getJda();
            if (jda == null) {
                Bukkit.getLogger().warning("DiscordSRV's JDA instance is not ready. Cannot edit message embed.");
                return;
            }
            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel != null) {
                channel.retrieveMessageById(messageId).queue(message -> {
                    if (message != null) {
                        message.editMessageEmbeds(embed).queue(
                                success -> {},
                                error -> Bukkit.getLogger().log(Level.WARNING, "Failed to edit embed in message " + messageId + ": " + error.getMessage(), error)
                        );
                    }
                }, error -> Bukkit.getLogger().log(Level.WARNING, "Failed to retrieve message " + messageId + " for embed edit: " + error.getMessage()));
            }
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "Error editing message embed: " + e.getMessage(), e);
        }
    }

    // Edit a message with both content and embed
    public static void editMessage(String channelId, long messageId, String newContent, MessageEmbed embed) {
        DiscordSRV dsrv = getDiscordSRV();
        if (dsrv == null) return;

        try {
            JDA jda = dsrv.getJda();
            if (jda == null) {
                Bukkit.getLogger().warning("DiscordSRV's JDA instance is not ready. Cannot edit message content and embed.");
                return;
            }
            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel != null) {
                channel.retrieveMessageById(messageId).queue(message -> {
                    if (message != null) {
                        message.editMessage(newContent).setEmbeds(embed).queue(
                                success -> {},
                                error -> Bukkit.getLogger().log(Level.WARNING, "Failed to edit content and embed in message " + messageId + ": " + error.getMessage(), error)
                        );
                    }
                }, error -> Bukkit.getLogger().log(Level.WARNING, "Failed to retrieve message " + messageId + " for content and embed edit: " + error.getMessage()));
            }
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "Error editing message content and embed: " + e.getMessage(), e);
        }
    }
    public static void sendFile(String channelId, File file, String fileName) {
        TextChannel channel = DiscordSRV.getPlugin().getJda().getTextChannelById(channelId);
        if (channel == null) {
            Bukkit.getLogger().warning("[DiscordUtil] Could not find Discord channel with ID: " + channelId);
            return;
        }

        channel.sendMessage("📁 **players_stats.yml backup:**").addFile(file, fileName).queue(
                success -> Bukkit.getLogger().info("[DiscordUtil] Uploaded " + fileName + " to Discord."),
                failure -> Bukkit.getLogger().severe("[DiscordUtil] Failed to upload file: " + failure.getMessage())
        );
    }
}
