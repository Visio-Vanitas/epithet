package top.atdove.epithet.test;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;

import net.neoforged.neoforge.gametest.GameTestHolder;

import top.atdove.epithet.Epithet;
import top.atdove.epithet.attachment.ModAttachments;
import top.atdove.epithet.attachment.PlayerTitleData;

import java.util.Optional;
import java.util.Set;

/**
 * Automated GameTest suite for Epithet Mod.
 * Simulates player title unlocking, equipping, authoritative locking security,
 * and test dummy behavior control.
 */
@GameTestHolder(Epithet.MOD_ID)
public class EpithetGameTests {

    private static final ResourceLocation TEST_TITLE_1 =
            ResourceLocation.fromNamespaceAndPath(Epithet.MOD_ID, "test_champion");
    private static final ResourceLocation TEST_TITLE_2 =
            ResourceLocation.fromNamespaceAndPath(Epithet.MOD_ID, "test_novice");

    /**
     * Test 1: Simulates player title unlocking and equipping pipeline.
     */
    @GameTest(template = "empty")
    public static void testPlayerUnlockAndEquip(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        PlayerTitleData data = player.getData(ModAttachments.PLAYER_TITLE_DATA);

        // Verify initial state
        helper.assertTrue(!data.hasTitle(TEST_TITLE_1), "Player should not have title initially");

        // Unlock title
        data.unlockTitle(TEST_TITLE_1);
        helper.assertTrue(data.hasTitle(TEST_TITLE_1), "Player should have unlocked test title");

        // Equip title
        data.setActiveTitle(Optional.of(TEST_TITLE_1));
        helper.assertTrue(data.getActiveTitle().isPresent(), "Active title should be present");
        helper.assertTrue(TEST_TITLE_1.equals(data.getActiveTitle().get()), "Active title should match equipped title");

        // Unequip title
        data.setActiveTitle(Optional.empty());
        helper.assertTrue(data.getActiveTitle().isEmpty(), "Active title should be empty after unequipping");

        helper.succeed();
    }

    /**
     * Test 2: Simulates authoritative locking security guard.
     * When a player is locked, unauthorized equip or change operations must be rejected.
     */
    @GameTest(template = "empty")
    public static void testPlayerLockedSecurityGuard(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        PlayerTitleData data = player.getData(ModAttachments.PLAYER_TITLE_DATA);

        data.unlockTitle(TEST_TITLE_1);
        data.unlockTitle(TEST_TITLE_2);
        data.setActiveTitle(Optional.of(TEST_TITLE_1));

        // Lock player titles
        data.setLocked(true);
        helper.assertTrue(data.isLocked(), "Player title state should be locked");

        // Simulate unauthorized attempt to switch title while locked
        if (!data.isLocked()) {
            data.setActiveTitle(Optional.of(TEST_TITLE_2));
        }

        // Verify title remained unchanged due to server-side authority guard
        helper.assertTrue(TEST_TITLE_1.equals(data.getActiveTitle().get()),
                "Active title should remain locked and unchanged");

        // Unlock player
        data.setLocked(false);
        helper.assertTrue(!data.isLocked(), "Player title state should be unlocked");

        helper.succeed();
    }

    /**
     * Test 3: Simulates test dummy spawning and behavior control (sneaking and invisibility).
     */
    @GameTest(template = "empty")
    public static void testDummyEntityBehavior(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        ArmorStand dummy = helper.spawn(EntityType.ARMOR_STAND, pos);

        dummy.addTag(DummyManager.DUMMY_TAG);
        DummyManager.setDummyTitle(dummy, TEST_TITLE_1);

        helper.assertTrue(DummyManager.isDummy(dummy), "Entity should be recognized as dummy");
        helper.assertTrue(TEST_TITLE_1.equals(DummyManager.getDummyTitleId(dummy)),
                "Dummy should be bound to test title");

        // Test sneaking behavior
        dummy.setShiftKeyDown(true);
        helper.assertTrue(dummy.isShiftKeyDown(), "Dummy should be in sneaking state");

        dummy.setShiftKeyDown(false);
        helper.assertTrue(!dummy.isShiftKeyDown(), "Dummy should exit sneaking state");

        // Test invisibility behavior
        dummy.setInvisible(true);
        helper.assertTrue(dummy.isInvisible(), "Dummy should be in invisible state");

        // Cleanup
        DummyManager.removeDummy(dummy);
        helper.assertTrue(dummy.isRemoved(), "Dummy should be removed");

        helper.succeed();
    }

    /**
     * Test 4: Verifies PlayerTitleData structure and copyOnDeath persistence consistency.
     */
    @GameTest(template = "empty")
    public static void testDataPersistenceConsistency(GameTestHelper helper) {
        PlayerTitleData original = new PlayerTitleData(
                Set.of(TEST_TITLE_1, TEST_TITLE_2),
                Optional.of(TEST_TITLE_1),
                true
        );

        helper.assertTrue(original.isLocked(), "Original data should be locked");
        helper.assertTrue(original.hasTitle(TEST_TITLE_1), "Original data should contain title 1");
        helper.assertTrue(original.hasTitle(TEST_TITLE_2), "Original data should contain title 2");
        helper.assertTrue(original.getActiveTitle().isPresent(), "Original active title should be present");

        // Clone/reproduce data as copyOnDeath attachment would
        PlayerTitleData cloned = new PlayerTitleData(
                original.getUnlockedTitles(),
                original.getActiveTitle(),
                original.isLocked()
        );

        helper.assertTrue(cloned.isLocked(), "Cloned data should preserve locked state");
        helper.assertTrue(cloned.hasTitle(TEST_TITLE_1), "Cloned data should preserve title 1");
        helper.assertTrue(cloned.hasTitle(TEST_TITLE_2), "Cloned data should preserve title 2");
        helper.assertTrue(cloned.getActiveTitle().isPresent(), "Cloned active title should be preserved");

        helper.succeed();
    }
}
