/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.common.service.user;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.mojang.authlib.GameProfile;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.server.management.PlayerList;
import net.minecraft.server.management.PlayerProfileCache;
import net.minecraft.server.management.UserListBans;
import net.minecraft.server.management.UserListEntryBan;
import net.minecraft.server.management.UserListWhitelist;
import net.minecraft.server.management.UserListWhitelistEntry;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.SaveHandler;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.entity.player.SpongeUser;
import org.spongepowered.common.interfaces.entity.player.IMixinEntityPlayerMP;
import org.spongepowered.common.util.SpongeUsernameCache;
import org.spongepowered.common.world.WorldManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

class UserDiscoverer {

    private static final Cache<UUID, User> userCache = Caffeine.newBuilder()
            .expireAfterAccess(1, TimeUnit.DAYS)
            .build();

    static User create(GameProfile profile) {
        User user = (User) new SpongeUser(profile);
        userCache.put(profile.getId(), user);
        return user;
    }

    /**
     * Searches for user data from a variety of places, in order of preference.
     * A user that has data in sponge may not necessarily have been online
     * before. A user added to the ban/whitelist that has not been on the server
     * before should be discoverable.
     *
     * @param profile The user's profile
     * @return The user data, or null if not found
     */
    static User findByProfile(org.spongepowered.api.profile.GameProfile profile) {
        UUID uniqueId = profile.getUniqueId();
        User user = userCache.getIfPresent(uniqueId);
        if (user != null) {
            // update cached user with name
            if (user.getName() == null && profile.getName().isPresent()) {
                user = getFromStoredData(profile);
            }
            return user;
        }
        user = getOnlinePlayer(uniqueId);
        if (user != null) {
            return user;
        }
        user = getFromStoredData(profile);
        if (user != null) {
            return user;
        }
        user = getFromWhitelist(uniqueId);
        if (user != null) {
            return user;
        }
        user = getFromBanlist(uniqueId);
        return user;
    }

    static User findByUsername(String username) {
        // check mojang cache
        PlayerProfileCache cache = SpongeImpl.getServer().getPlayerProfileCache();
        HashSet<String> names = Sets.newHashSet(cache.getUsernames());
        if (names.contains(username.toLowerCase(Locale.ROOT))) {
            GameProfile profile = cache.getGameProfileForUsername(username);
            if (profile != null) {
                return findByProfile((org.spongepowered.api.profile.GameProfile) profile);
            }
        }

        // check username cache
        final UUID uuid = SpongeUsernameCache.getLastKnownUUID(username);
        if (uuid != null) {
            return create(new GameProfile(uuid, username));
        }

        return null;
    }

    static Collection<org.spongepowered.api.profile.GameProfile> getAllProfiles() {
        Preconditions.checkState(Sponge.isServerAvailable(), "Server is not available!");
        Set<org.spongepowered.api.profile.GameProfile> profiles = Sets.newHashSet();

        // Add all cached profiles
        profiles.addAll(userCache.asMap().values().stream().map(User::getProfile).collect(Collectors.toList()));

        // Add all known profiles from the data files
        SaveHandler saveHandler = (SaveHandler) WorldManager.getWorldByDimensionId(0).get().getSaveHandler();
        String[] uuids = saveHandler.getAvailablePlayerDat();
        for (String playerUuid : uuids) {

            // If the filename contains a period, we can fail fast. Vanilla code fixes the Strings that have ".dat" to strip that out
            // before passing that back in getAvailablePlayerDat. It doesn't remove non ".dat" filenames from the list.
            if (playerUuid.contains(".")) {
                continue;
            }

            // At this point, we have a filename who has no extension. This doesn't mean it is actually a UUID. We trap the exception and ignore
            // any filenames that fail the UUID check.
            UUID uuid;
            try {
                uuid = UUID.fromString(playerUuid);
            } catch (Exception ex) {
                continue;
            }

            final GameProfile profile = SpongeImpl.getServer().getPlayerProfileCache().getProfileByUUID(uuid);
            if (profile != null) {
                profiles.add((org.spongepowered.api.profile.GameProfile) profile);
            }
        }

        // Add all whitelisted users
        final UserListWhitelist whiteList = SpongeImpl.getServer().getPlayerList().getWhitelistedPlayers();
        profiles.addAll(whiteList.getValues().values().stream().map(entry -> (org.spongepowered.api.profile.GameProfile) entry.value)
                .collect(Collectors.toList()));

        // Add all banned users
        final UserListBans banList = SpongeImpl.getServer().getPlayerList().getBannedPlayers();
        profiles.addAll(banList.getValues().values().stream().filter(entry -> entry != null).map(entry -> (org.spongepowered.api.profile.GameProfile)
                entry.value).collect(Collectors.toList()));

        return profiles;
    }

    static boolean delete(UUID uniqueId) {
        if (getOnlinePlayer(uniqueId) != null) {
            // Don't delete online player's data
            return false;
        }
        boolean success = deleteStoredPlayerData(uniqueId);
        success = success && deleteWhitelistEntry(uniqueId);
        success = success && deleteBanlistEntry(uniqueId);
        return success;
    }

    private static User getOnlinePlayer(UUID uniqueId) {
        Preconditions.checkState(Sponge.isServerAvailable(), "Server is not available!");
        final PlayerList playerList = SpongeImpl.getServer().getPlayerList();

        // Although the player itself could be returned here (as Player extends
        // User), a plugin is more likely to cache the User object and we don't
        // want the player entity to be cached.
        final IMixinEntityPlayerMP player = (IMixinEntityPlayerMP) playerList.getPlayerByUUID(uniqueId);
        if (player != null) {
            final User user = player.getUserObject();
            userCache.put(uniqueId, user);
            return user;
        }

        return null;
    }

    private static User getFromStoredData(org.spongepowered.api.profile.GameProfile profile) {
        // Always cache user to avoid constant lookups in storage when file does not exist
        final User user = create((GameProfile) profile);
        // Note: Uses the overworld's player data
        final File dataFile = getPlayerDataFile(profile.getUniqueId());
        if (dataFile == null) {
            return null;
        }

        try {
            ((SpongeUser) user).readFromNbt(CompressedStreamTools.readCompressed(new FileInputStream(dataFile)));
        } catch (IOException e) {
            SpongeImpl.getLogger().warn("Corrupt user file {}", dataFile, e);
        }

        return user;
    }

    private static User getFromWhitelist(UUID uniqueId) {
        GameProfile profile = null;
        UserListWhitelist whiteList = SpongeImpl.getServer().getPlayerList().getWhitelistedPlayers();
        UserListWhitelistEntry whiteListData = whiteList.getEntry(new GameProfile(uniqueId, ""));
        if (whiteListData != null) {
            profile = whiteListData.value;
        }
        if (profile != null) {
            return create(profile);
        }
        return null;
    }

    private static User getFromBanlist(UUID uniqueId) {
        GameProfile profile = null;
        UserListBans banList = SpongeImpl.getServer().getPlayerList().getBannedPlayers();
        UserListEntryBan<GameProfile> banData = banList.getEntry(new GameProfile(uniqueId, ""));
        if (banData != null) {
            profile = banData.value;
        }
        if (profile != null) {
            return create(profile);
        }
        return null;
    }

    private static File getPlayerDataFile(UUID uniqueId) {
        // This may be called triggered by mods using FakePlayer during
        // initial world gen (before the overworld is registered). Because of
        // this, we need to check if the overworld is actually registered yet
        Optional<WorldServer> worldServer = WorldManager.getWorldByDimensionId(0);
        if (!worldServer.isPresent()) {
            return null;
        }

        // Note: Uses the overworld's player data
        SaveHandler saveHandler = (SaveHandler) worldServer.get().getSaveHandler();
        File file = new File(saveHandler.playersDirectory, uniqueId.toString() + ".dat");
        if (file.exists()) {
            return file;
        }
        return null;
    }

    private static boolean deleteStoredPlayerData(UUID uniqueId) {
        File dataFile = getPlayerDataFile(uniqueId);
        if (dataFile != null) {
            try {
                return dataFile.delete();
            } catch (SecurityException e) {
                SpongeImpl.getLogger().warn("Unable to delete file {} due to a security error", dataFile, e);
                return false;
            }
        }
        return true;
    }

    private static boolean deleteWhitelistEntry(UUID uniqueId) {
        UserListWhitelist whiteList = SpongeImpl.getServer().getPlayerList().getWhitelistedPlayers();
        whiteList.removeEntry(new GameProfile(uniqueId, ""));
        return true;
    }

    private static boolean deleteBanlistEntry(UUID uniqueId) {
        UserListBans banList = SpongeImpl.getServer().getPlayerList().getBannedPlayers();
        banList.removeEntry(new GameProfile(uniqueId, ""));
        return true;
    }

}
