package top.atdove.epithet.command;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.decoration.ArmorStand;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.JsonOps;

import net.neoforged.neoforge.event.RegisterCommandsEvent;

import com.google.gson.JsonParser;
import top.atdove.epithet.Epithet;
import top.atdove.epithet.attachment.ModAttachments;
import top.atdove.epithet.attachment.PlayerTitleData;
import top.atdove.epithet.data.TitleSavedData;
import top.atdove.epithet.network.OpenTitleGuiPayload;
import top.atdove.epithet.network.TitleNetworkHandler;
import top.atdove.epithet.test.DummyManager;
import top.atdove.epithet.title.TitleDefinition;
import top.atdove.epithet.title.TitleRegistry;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class TitleCommands {

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
            Commands.literal("epithet")
                // /epithet gui
                .then(Commands.literal("gui")
                    .executes(TitleCommands::executeGui))
                // /epithet list
                .then(Commands.literal("list")
                    .executes(TitleCommands::executeList))
                // /epithet admin
                .then(Commands.literal("admin")
                    .requires(source -> source.hasPermission(2))
                    // admin create <id> <name_json_or_text> [color] [desc]
                    .then(Commands.literal("create")
                        .then(Commands.argument("id", ResourceLocationArgument.id())
                            .then(Commands.argument("name", StringArgumentType.string())
                                .executes(ctx -> executeAdminCreate(ctx, 0xFFFFFF, ""))
                                .then(Commands.argument("color", StringArgumentType.word())
                                    .executes(ctx -> executeAdminCreate(ctx, parseColor(StringArgumentType.getString(ctx, "color")), ""))
                                    .then(Commands.argument("desc", StringArgumentType.greedyString())
                                        .executes(ctx -> executeAdminCreate(ctx, parseColor(StringArgumentType.getString(ctx, "color")), StringArgumentType.getString(ctx, "desc")))
                                    )
                                )
                            )
                        )
                    )
                    // admin give <targets> <id>
                    .then(Commands.literal("give")
                        .then(Commands.argument("targets", EntityArgument.players())
                            .then(Commands.argument("id", ResourceLocationArgument.id())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(TitleRegistry.getInstance().getAllTitles().keySet(), builder))
                                .executes(TitleCommands::executeAdminGive)
                            )
                        )
                    )
                    // admin take <targets> <id>
                    .then(Commands.literal("take")
                        .then(Commands.argument("targets", EntityArgument.players())
                            .then(Commands.argument("id", ResourceLocationArgument.id())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(TitleRegistry.getInstance().getAllTitles().keySet(), builder))
                                .executes(TitleCommands::executeAdminTake)
                            )
                        )
                    )
                    // admin set <targets> <id>
                    .then(Commands.literal("set")
                        .then(Commands.argument("targets", EntityArgument.players())
                            .then(Commands.argument("id", ResourceLocationArgument.id())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(TitleRegistry.getInstance().getAllTitles().keySet(), builder))
                                .executes(TitleCommands::executeAdminSet)
                            )
                        )
                    )
                    // admin lock <targets>
                    .then(Commands.literal("lock")
                        .then(Commands.argument("targets", EntityArgument.players())
                            .executes(TitleCommands::executeAdminLock)
                        )
                    )
                    // admin unlock <targets>
                    .then(Commands.literal("unlock")
                        .then(Commands.argument("targets", EntityArgument.players())
                            .executes(TitleCommands::executeAdminUnlock)
                        )
                    )
                )
                // /epithet test dummy ...
                .then(Commands.literal("test")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.literal("dummy")
                        // spawn [name] [title]
                        .then(Commands.literal("spawn")
                            .executes(TitleCommands::executeDummySpawnDefault)
                            .then(Commands.argument("name", StringArgumentType.word())
                                .executes(TitleCommands::executeDummySpawnNamed)
                                .then(Commands.argument("title", ResourceLocationArgument.id())
                                    .suggests((c, b) -> SharedSuggestionProvider.suggestResource(TitleRegistry.getInstance().getAllTitles().keySet(), b))
                                    .executes(TitleCommands::executeDummySpawnFull)
                                )
                            )
                        )
                        // sneak <true|false>
                        .then(Commands.literal("sneak")
                            .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(TitleCommands::executeDummySneak)
                            )
                        )
                        // invisible <true|false>
                        .then(Commands.literal("invisible")
                            .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(TitleCommands::executeDummyInvisible)
                            )
                        )
                        // title <title_id>
                        .then(Commands.literal("title")
                            .then(Commands.argument("title", ResourceLocationArgument.id())
                                .suggests((c, b) -> SharedSuggestionProvider.suggestResource(TitleRegistry.getInstance().getAllTitles().keySet(), b))
                                .executes(TitleCommands::executeDummyTitle)
                            )
                        )
                        // look
                        .then(Commands.literal("look")
                            .executes(TitleCommands::executeDummyLook)
                        )
                        // remove
                        .then(Commands.literal("remove")
                            .executes(TitleCommands::executeDummyRemove)
                        )
                    )
                )
        );
    }

    private static int executeGui(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        TitleNetworkHandler.syncPlayerTitle(player, player, true);
        TitleNetworkHandler.sendToPlayer(player, new OpenTitleGuiPayload());
        ctx.getSource().sendSuccess(() -> Component.translatable("command.epithet.gui.opening").withStyle(ChatFormatting.GRAY), false);
        return 1;
    }

    private static int executeList(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        PlayerTitleData data = player.getData(ModAttachments.PLAYER_TITLE_DATA);
        Set<ResourceLocation> unlocked = data.getUnlockedTitles();

        if (unlocked.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.translatable("command.epithet.list.empty").withStyle(ChatFormatting.YELLOW), false);
            return 1;
        }

        ctx.getSource().sendSuccess(() -> Component.translatable("command.epithet.list.header", unlocked.size()).withStyle(ChatFormatting.GOLD), false);
        for (ResourceLocation id : unlocked) {
            Optional<TitleDefinition> titleOpt = TitleRegistry.getInstance().getTitle(id);
            MutableComponent line = Component.literal("- ");
            if (titleOpt.isPresent()) {
                TitleDefinition title = titleOpt.get();
                line.append(title.displayName());
            } else {
                line.append(Component.literal(id.toString()).withStyle(ChatFormatting.WHITE));
            }

            if (data.getActiveTitle().filter(id::equals).isPresent()) {
                line.append(Component.literal(" ")).append(Component.translatable("command.epithet.list.equipped").withStyle(ChatFormatting.GREEN));
            }
            line.append(Component.literal(" (" + id + ")").withStyle(ChatFormatting.DARK_GRAY));
            ctx.getSource().sendSuccess(() -> line, false);
        }

        if (data.isLocked()) {
            ctx.getSource().sendSuccess(() -> Component.translatable("command.epithet.list.locked").withStyle(ChatFormatting.RED), false);
        }

        return 1;
    }

    private static int executeAdminCreate(CommandContext<CommandSourceStack> ctx, int color, String desc) {
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "id");
        String nameStr = StringArgumentType.getString(ctx, "name");

        Component nameComponent;
        if (nameStr.startsWith("{")) {
            try {
                nameComponent = ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(nameStr))
                    .result().orElse(Component.literal(nameStr));
            } catch (Exception e) {
                nameComponent = Component.literal(nameStr);
            }
        } else {
            nameComponent = Component.literal(nameStr);
        }

        Component descComponent = desc.isEmpty() ? Component.empty() : Component.literal(desc);
        TitleDefinition newTitle = new TitleDefinition(
            id,
            nameComponent,
            descComponent,
            color,
            0,
            "common",
            ResourceLocation.fromNamespaceAndPath(Epithet.MOD_ID, "default"),
            false
        );

        TitleSavedData.get(ctx.getSource().getServer()).addTitle(newTitle);
        TitleNetworkHandler.broadcastRegistry(ctx.getSource().getServer());

        ctx.getSource().sendSuccess(() -> Component.translatable("command.epithet.admin.create.success", id.toString()).withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int executeAdminGive(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "id");
        Optional<TitleDefinition> titleOpt = TitleRegistry.getInstance().getTitle(id);

        for (ServerPlayer target : targets) {
            PlayerTitleData data = target.getData(ModAttachments.PLAYER_TITLE_DATA);
            data.unlockTitle(id);
            TitleNetworkHandler.syncPlayerTitle(target, target, true);

            Component titleComp = titleOpt.map(TitleDefinition::displayName).orElse(Component.literal(id.toString()));
            target.sendSystemMessage(Component.translatable("command.epithet.admin.give.target", titleComp).withStyle(ChatFormatting.GREEN));
        }

        ctx.getSource().sendSuccess(() -> Component.translatable("command.epithet.admin.give.sender", targets.size(), id.toString()).withStyle(ChatFormatting.GREEN), true);
        return targets.size();
    }

    private static int executeAdminTake(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "id");
        Optional<TitleDefinition> titleOpt = TitleRegistry.getInstance().getTitle(id);

        for (ServerPlayer target : targets) {
            PlayerTitleData data = target.getData(ModAttachments.PLAYER_TITLE_DATA);
            data.removeTitle(id);
            TitleNetworkHandler.syncToPlayerAndTrackers(target);

            Component titleComp = titleOpt.map(TitleDefinition::displayName).orElse(Component.literal(id.toString()));
            target.sendSystemMessage(Component.translatable("command.epithet.admin.take.target", titleComp).withStyle(ChatFormatting.YELLOW));
        }

        ctx.getSource().sendSuccess(() -> Component.translatable("command.epithet.admin.take.sender", targets.size(), id.toString()).withStyle(ChatFormatting.GREEN), true);
        return targets.size();
    }

    private static int executeAdminSet(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "id");
        Optional<TitleDefinition> titleOpt = TitleRegistry.getInstance().getTitle(id);

        for (ServerPlayer target : targets) {
            PlayerTitleData data = target.getData(ModAttachments.PLAYER_TITLE_DATA);
            data.unlockTitle(id);
            data.setActiveTitle(Optional.of(id));
            TitleNetworkHandler.syncToPlayerAndTrackers(target);

            Component titleComp = titleOpt.map(TitleDefinition::displayName).orElse(Component.literal(id.toString()));
            target.sendSystemMessage(Component.translatable("command.epithet.admin.set.target", titleComp).withStyle(ChatFormatting.GREEN));
        }

        ctx.getSource().sendSuccess(() -> Component.translatable("command.epithet.admin.set.sender", targets.size(), id.toString()).withStyle(ChatFormatting.GREEN), true);
        return targets.size();
    }

    private static int executeAdminLock(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        for (ServerPlayer target : targets) {
            PlayerTitleData data = target.getData(ModAttachments.PLAYER_TITLE_DATA);
            data.setLocked(true);
            TitleNetworkHandler.syncPlayerTitle(target, target, true);
            target.sendSystemMessage(Component.translatable("command.epithet.admin.lock.target").withStyle(ChatFormatting.RED));
        }

        ctx.getSource().sendSuccess(() -> Component.translatable("command.epithet.admin.lock.sender", targets.size()).withStyle(ChatFormatting.GREEN), true);
        return targets.size();
    }

    private static int executeAdminUnlock(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        for (ServerPlayer target : targets) {
            PlayerTitleData data = target.getData(ModAttachments.PLAYER_TITLE_DATA);
            data.setLocked(false);
            TitleNetworkHandler.syncPlayerTitle(target, target, true);
            target.sendSystemMessage(Component.translatable("command.epithet.admin.unlock.target").withStyle(ChatFormatting.GREEN));
        }

        ctx.getSource().sendSuccess(() -> Component.translatable("command.epithet.admin.unlock.sender", targets.size()).withStyle(ChatFormatting.GREEN), true);
        return targets.size();
    }

    private static int parseColor(String str) {
        try {
            if (str.startsWith("#")) {
                return (int) Long.parseLong(str.substring(1), 16);
            } else if (str.startsWith("0x") || str.startsWith("0X")) {
                return (int) Long.parseLong(str.substring(2), 16);
            } else {
                return (int) Long.parseLong(str);
            }
        } catch (NumberFormatException e) {
            return 0xFFFFFF;
        }
    }

    private static int executeDummySpawnDefault(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return spawnDummy(ctx, DummyManager.DEFAULT_NAME, null);
    }

    private static int executeDummySpawnNamed(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String name = StringArgumentType.getString(ctx, "name");
        return spawnDummy(ctx, name, null);
    }

    private static int executeDummySpawnFull(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String name = StringArgumentType.getString(ctx, "name");
        ResourceLocation title = ResourceLocationArgument.getId(ctx, "title");
        return spawnDummy(ctx, name, title);
    }

    private static int spawnDummy(CommandContext<CommandSourceStack> ctx, String name, ResourceLocation titleId) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        var look = player.getLookAngle();
        double x = player.getX() + look.x * 2.0;
        double y = player.getY();
        double z = player.getZ() + look.z * 2.0;

        ArmorStand dummy = new ArmorStand(player.serverLevel(), x, y, z);
        dummy.setYRot(player.getYRot() + 180.0f);
        dummy.setCustomName(Component.literal(name));
        dummy.setCustomNameVisible(true);
        dummy.setShowArms(true);
        dummy.addTag(DummyManager.DUMMY_TAG);

        if (titleId != null) {
            DummyManager.setDummyTitle(dummy, titleId);
        }

        player.serverLevel().addFreshEntity(dummy);
        DummyManager.syncDummyToTrackers(dummy);

        ctx.getSource().sendSuccess(() -> Component.translatable("command.epithet.dummy.spawn.success", name), false);
        return 1;
    }

    private static int executeDummySneak(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        boolean enabled = BoolArgumentType.getBool(ctx, "enabled");
        ArmorStand dummy = DummyManager.findNearestDummy(player, 10.0);
        if (dummy == null) {
            ctx.getSource().sendFailure(Component.translatable("command.epithet.dummy.not_found"));
            return 0;
        }
        dummy.setShiftKeyDown(enabled);
        ctx.getSource().sendSuccess(() -> Component.translatable("command.epithet.dummy.sneak.success", String.valueOf(enabled)), false);
        return 1;
    }

    private static int executeDummyInvisible(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        boolean enabled = BoolArgumentType.getBool(ctx, "enabled");
        ArmorStand dummy = DummyManager.findNearestDummy(player, 10.0);
        if (dummy == null) {
            ctx.getSource().sendFailure(Component.translatable("command.epithet.dummy.not_found"));
            return 0;
        }
        dummy.setInvisible(enabled);
        ctx.getSource().sendSuccess(() -> Component.translatable("command.epithet.dummy.invisible.success", String.valueOf(enabled)), false);
        return 1;
    }

    private static int executeDummyTitle(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ResourceLocation titleId = ResourceLocationArgument.getId(ctx, "title");
        ArmorStand dummy = DummyManager.findNearestDummy(player, 10.0);
        if (dummy == null) {
            ctx.getSource().sendFailure(Component.translatable("command.epithet.dummy.not_found"));
            return 0;
        }
        DummyManager.setDummyTitle(dummy, titleId);
        DummyManager.syncDummyToTrackers(dummy);
        ctx.getSource().sendSuccess(() -> Component.translatable("command.epithet.dummy.title.success", titleId.toString()), false);
        return 1;
    }

    private static int executeDummyLook(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ArmorStand dummy = DummyManager.findNearestDummy(player, 10.0);
        if (dummy == null) {
            ctx.getSource().sendFailure(Component.translatable("command.epithet.dummy.not_found"));
            return 0;
        }
        dummy.lookAt(EntityAnchorArgument.Anchor.EYES, player.getEyePosition());
        ctx.getSource().sendSuccess(() -> Component.translatable("command.epithet.dummy.look.success"), false);
        return 1;
    }

    private static int executeDummyRemove(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        List<ArmorStand> list = DummyManager.findNearbyDummies(player, 16.0);
        if (list.isEmpty()) {
            ctx.getSource().sendFailure(Component.translatable("command.epithet.dummy.not_found"));
            return 0;
        }
        for (ArmorStand dummy : list) {
            DummyManager.removeDummy(dummy);
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("command.epithet.dummy.remove.success", list.size()), false);
        return list.size();
    }
}
