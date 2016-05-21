package com.pahimar.ee3.knowledge;

import com.google.gson.JsonSyntaxException;
import com.pahimar.ee3.api.knowledge.AbilityRegistryProxy;
import com.pahimar.ee3.handler.ConfigurationHandler;
import com.pahimar.ee3.reference.Comparators;
import com.pahimar.ee3.reference.Files;
import com.pahimar.ee3.util.SerializationHelper;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.TreeMap;

public class PlayerKnowledgeRegistry {

    public static final PlayerKnowledgeRegistry INSTANCE = new PlayerKnowledgeRegistry();

    private final TreeMap<String, PlayerKnowledge> playerKnowledgeMap;
    private final PlayerKnowledge templatePlayerKnowledge;

    public static File templatePlayerKnowledgeFile;

    private PlayerKnowledgeRegistry() {

        playerKnowledgeMap = new TreeMap<>(Comparators.STRING_COMPARATOR);
        templatePlayerKnowledge = new PlayerKnowledge();
    }

    public PlayerKnowledge getTemplatePlayerKnowledge() {
        return templatePlayerKnowledge;
    }

    public boolean doesPlayerKnow(EntityPlayer player, ItemStack itemStack) {

        if (player != null) {
            return doesPlayerKnow(player.getDisplayName(), itemStack);
        }

        return false;
    }

    public boolean doesPlayerKnow(String playerName, ItemStack itemStack) {

        if (getPlayerKnowledge(playerName) != null) {
            return getPlayerKnowledge(playerName).isKnown(itemStack);
        }

        return false;
    }

    public boolean canPlayerLearn(EntityPlayer entityPlayer, ItemStack itemStack) {

        if (entityPlayer != null) {
            return canPlayerLearn(entityPlayer.getDisplayName(), itemStack);
        }

        return false;
    }

    public boolean canPlayerLearn(String playerName, ItemStack itemStack) {

        if (getPlayerKnowledge(playerName) != null) {
            return !getPlayerKnowledge(playerName).isKnown(itemStack) && AbilityRegistryProxy.isLearnable(itemStack);
        }

        return false;
    }

    public void teachPlayer(EntityPlayer entityPlayer, ItemStack itemStack) {

        if (entityPlayer != null) {
            teachPlayer(entityPlayer.getDisplayName(), itemStack);
        }
    }

    public void teachPlayer(String playerName, ItemStack itemStack) {

        if (itemStack != null && getPlayerKnowledge(playerName) != null) {
            getPlayerKnowledge(playerName).learn(itemStack);
            save(playerName);
        }
    }

    public void teachPlayer(EntityPlayer entityPlayer, Collection<ItemStack> itemStacks) {

        if (entityPlayer != null) {
            teachPlayer(entityPlayer.getDisplayName(), itemStacks);
        }
    }

    public void teachPlayer(String playerName, Collection<ItemStack> itemStacks) {

        if (itemStacks != null) {

            PlayerKnowledge playerKnowledge = getPlayerKnowledge(playerName);

            if (playerKnowledge != null) {
                itemStacks.forEach(playerKnowledge::learn);
                save(playerName);
            }
        }
    }

    public void makePlayerForget(EntityPlayer entityPlayer, ItemStack itemStack) {

        if (entityPlayer != null) {
            makePlayerForget(entityPlayer.getDisplayName(), itemStack);
        }
    }

    public void makePlayerForget(String playerName, ItemStack itemStack) {

        if (getPlayerKnowledge(playerName) != null) {
            getPlayerKnowledge(playerName).forget(itemStack);
            save(playerName);
        }
    }

    public void makePlayerForget(EntityPlayer entityPlayer, Collection<ItemStack> itemStacks) {

        if (entityPlayer != null) {
            makePlayerForget(entityPlayer.getDisplayName(), itemStacks);
        }
    }

    public void makePlayerForget(String playerName, Collection<ItemStack> itemStacks) {

        if (itemStacks != null) {

            PlayerKnowledge playerKnowledge = getPlayerKnowledge(playerName);

            if (playerKnowledge != null) {
                itemStacks.forEach(playerKnowledge::forget);
                save(playerName);
            }
        }
    }

    public void makePlayerForgetAll(EntityPlayer entityPlayer) {

        if (entityPlayer != null) {
            makePlayerForgetAll(entityPlayer.getDisplayName());
        }
    }

    public void makePlayerForgetAll(String playerName) {

        if (playerName != null && !playerName.isEmpty()) {
            playerKnowledgeMap.put(playerName, new PlayerKnowledge());
            save(playerName);
        }
    }

    /**
     * TODO Finish JavaDoc
     *
     * @param entityPlayer
     * @return
     */
    public PlayerKnowledge getPlayerKnowledge(EntityPlayer entityPlayer) {

        if (entityPlayer != null) {
            return getPlayerKnowledge(entityPlayer.getDisplayName());
        }

        return null;
    }

    /**
     * TODO Finish JavaDoc
     *
     * @param playerName
     * @return
     */
    public PlayerKnowledge getPlayerKnowledge(String playerName) {

        // TODO Logging
        if (playerName != null && !playerName.isEmpty()) {
            if (!playerKnowledgeMap.containsKey(playerName)) {
                playerKnowledgeMap.put(playerName, load(getPlayerKnowledgeFile(playerName), false));
            }

            return playerKnowledgeMap.get(playerName);
        }

        return null;
    }

    /**
     * TODO Finish JavaDoc
     */
    public void saveAll() {

        if (FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER) {

            // Save the template to file
            SerializationHelper.writeJsonFile(templatePlayerKnowledgeFile, SerializationHelper.GSON.toJson(templatePlayerKnowledge));

            // Save every currently loaded player knowledge to file
            for (String playerName : playerKnowledgeMap.keySet()) {
                save(playerName);
            }
        }
    }

    /**
     * TODO Finish JavaDoc
     *
     * @param playerName
     */
    private void save(String playerName) {

        if (FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER) {
            if (playerName != null && !playerName.isEmpty()) {
                File playerKnowledgeFile = getPlayerKnowledgeFile(playerName);
                if (playerKnowledgeFile != null) {
                    SerializationHelper.writeJsonFile(playerKnowledgeFile, SerializationHelper.GSON.toJson(playerKnowledgeMap.get(playerName)));
                }
            }
        }
    }

    /**
     * TODO Finish JavaDoc
     */
    public void load() {

        // Load template knowledge
        if (FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER) {

            templatePlayerKnowledge.forgetAll();
            templatePlayerKnowledge.learn(load(templatePlayerKnowledgeFile, true).getKnownItemStacks());

            // Reset the player knowledge map
            playerKnowledgeMap.clear();
        }
    }

    /**
     * TODO Finish JavaDoc
     *
     * @param file
     * @return
     */
    private PlayerKnowledge load(File file, boolean isTemplate) {

        if (file != null) {
            try {
                String jsonString = SerializationHelper.readJsonFile(file);
                PlayerKnowledge playerKnowledge = SerializationHelper.GSON.fromJson(jsonString, PlayerKnowledge.class);

                if (playerKnowledge != null) {
                   return playerKnowledge;
                }
            }
            catch (JsonSyntaxException | FileNotFoundException e) {
            }
        }

        if (ConfigurationHandler.Settings.playerKnowledgeTemplateEnabled && !isTemplate) {
            return new PlayerKnowledge(templatePlayerKnowledge);
        }
        else {
            return new PlayerKnowledge();
        }
    }

    /**
     * TODO Finish JavaDoc
     *
     * @param playerName
     * @return
     */
    private static File getPlayerKnowledgeFile(String playerName) {

        if (playerName != null && !playerName.isEmpty()) {
            return new File(Files.playerDataDirectory, "knowledge" + File.separator + "transmutation" + File.separator + playerName + ".json");
        }

        return null;
    }
}
