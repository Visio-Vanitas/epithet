package top.atdove.epithet.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DataGen helper tool for generating all internationalization language files
 * (zh_cn, zh_tw, en_us, en_gb) programmatically rather than manually writing JSON.
 */
public class LanguageDataGenTool {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public static void main(String[] args) {
        String targetDir = "src/main/resources/assets/epithet/lang";
        if (args.length > 0) {
            targetDir = args[0];
        }

        File dir = new File(targetDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        writeLangFile(new File(dir, "zh_cn.json"), buildZhCn());
        writeLangFile(new File(dir, "zh_tw.json"), buildZhTw());
        writeLangFile(new File(dir, "en_us.json"), buildEnUs());
        writeLangFile(new File(dir, "en_gb.json"), buildEnGb());

        System.out.println("Language DataGen successfully generated 4 language files in: " + dir.getAbsolutePath());
    }

    private static void writeLangFile(File file, Map<String, String> translations) {
        try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(translations, writer);
        } catch (IOException e) {
            System.err.println("Failed to write language file " + file.getName() + ": " + e.getMessage());
        }
    }

    public static Map<String, String> buildZhCn() {
        Map<String, String> m = new LinkedHashMap<>();
        // GUI
        m.put("gui.epithet.pause_button_tooltip", "打开称号系统");
        m.put("gui.epithet.title_selection.title", "称号选择");
        m.put("gui.epithet.button.equip", "佩戴");
        m.put("gui.epithet.button.equip_tooltip", "佩戴此称号并在头顶展示");
        m.put("gui.epithet.button.unequip", "卸下");
        m.put("gui.epithet.button.unequip_tooltip", "卸下当前已佩戴的称号");
        m.put("gui.epithet.button.locked", "未解锁");
        m.put("gui.epithet.status.locked_tag", "[已锁定]");
        m.put("gui.epithet.title.not_unlocked_tooltip", "您尚未解锁该称号");
        m.put("gui.epithet.title.locked_tooltip", "称号已被管理员强制锁定，无法自行更换");
        m.put("gui.epithet.title.locked_banner", "您的称号已被管理员强制锁定");
        m.put("gui.epithet.button.player_selector", "玩家管理");
        m.put("gui.epithet.player_selector.title", "玩家称号管理器");
        m.put("gui.epithet.player_selector.search", "搜索玩家");
        m.put("gui.epithet.player_selector.search_hint", "输入玩家名筛选...");
        m.put("gui.epithet.button.lock", "锁定");
        m.put("gui.epithet.button.lock_tooltip", "锁定该玩家的称号更换权限");
        m.put("gui.epithet.button.unlock", "解锁");
        m.put("gui.epithet.button.unlock_tooltip", "解除锁定，允许自主更换称号");
        m.put("gui.epithet.no_title", "未佩戴称号");
        m.put("gui.epithet.empty_titles", "暂无已解锁称号，可通过达成成就或管理员赋予获取");
        m.put("gui.epithet.button.manage", "称号管理");
        m.put("gui.epithet.button.manage_tooltip", "管理该玩家的全部称号与佩戴状态");
        m.put("gui.epithet.manage.title", "玩家称号管理");
        m.put("gui.epithet.manage.tab.owned", "已拥有称号 (%d)");
        m.put("gui.epithet.manage.tab.available", "全服称号库 (%d)");
        m.put("gui.epithet.manage.status.locked", "状态: 已锁定");
        m.put("gui.epithet.manage.status.unlocked", "状态: 正常");
        m.put("gui.epithet.manage.status.equipped_badge", "[已佩戴]");
        m.put("gui.epithet.manage.button.set_equipped", "设为佩戴");
        m.put("gui.epithet.manage.button.set_equipped_tooltip", "强制将该玩家的佩戴称号设为此项");
        m.put("gui.epithet.manage.button.unequip", "卸下");
        m.put("gui.epithet.manage.button.unequip_tooltip", "卸下该玩家当前佩戴的称号");
        m.put("gui.epithet.manage.button.take", "收回");
        m.put("gui.epithet.manage.button.take_tooltip", "收回该称号，从玩家已解锁列表中移除");
        m.put("gui.epithet.manage.button.give", "赋予");
        m.put("gui.epithet.manage.button.give_tooltip", "向该玩家赋予此称号");
        m.put("gui.epithet.manage.button.add_title", "+ 赋予新称号");
        m.put("gui.epithet.manage.button.add_title_tooltip", "从全服称号库中选择新称号赋予给该玩家");
        m.put("gui.epithet.manage.button.back_to_owned", "返回已拥有列表");
        m.put("gui.epithet.manage.empty_owned", "该玩家尚未拥有任何称号");
        m.put("gui.epithet.manage.empty_available", "该玩家已拥有全服所有可用称号");
        m.put("gui.epithet.manage.search_hint", "筛选称号...");

        // Title Creation GUI
        m.put("gui.epithet.button.create_title", "创建称号");
        m.put("gui.epithet.create.title", "创建自定义称号");
        m.put("gui.epithet.create.id_tooltip", "称号唯一标识符（命名空间:名称）");
        m.put("gui.epithet.create.name_tooltip", "称号显示名称，支持文本、渐变色及动态流光标签");
        m.put("gui.epithet.create.color_tooltip", "称号主颜色（十六进制代码，如 #FFAA00）");
        m.put("gui.epithet.create.rarity_tooltip", "称号稀有度（common, uncommon, rare, epic, legendary）");
        m.put("gui.epithet.create.button.save_and_equip", "保存并佩戴");
        m.put("gui.epithet.create.button.save_only", "仅保存");
        m.put("gui.epithet.create.label.id", "称号标识符 (ID)");
        m.put("gui.epithet.create.label.name", "显示名称 (支持渐变色/动画)");
        m.put("gui.epithet.create.label.desc", "称号描述 (Description)");
        m.put("gui.epithet.create.label.color", "主色调 (Hex)");
        m.put("gui.epithet.create.label.rarity", "稀有度 (Rarity)");
        m.put("gui.epithet.create.preview_label", "实时预览");

        // Commands
        m.put("command.epithet.gui.opening", "正在打开称号界面...");
        m.put("command.epithet.list.empty", "你尚未拥有任何称号。");
        m.put("command.epithet.list.header", "=== 你的称号列表 (共 %s 个) ===");
        m.put("command.epithet.list.equipped", "[已佩戴]");
        m.put("command.epithet.list.locked", "当前称号已被锁定，无法更改。");
        m.put("command.epithet.admin.create.success", "成功创建称号: %s");
        m.put("command.epithet.admin.give.target", "你获得了新称号：%s");
        m.put("command.epithet.admin.give.sender", "已向 %s 位玩家赋予称号 %s");
        m.put("command.epithet.admin.take.target", "你的称号已被收回：%s");
        m.put("command.epithet.admin.take.sender", "已从 %s 位玩家收回称号 %s");
        m.put("command.epithet.admin.set.target", "你的佩戴称号已被设定为：%s");
        m.put("command.epithet.admin.set.sender", "已将 %s 位玩家的当前称号设为 %s");
        m.put("command.epithet.admin.lock.target", "你的称号已被管理员锁定，无法自主更改！");
        m.put("command.epithet.admin.lock.sender", "已锁定 %s 位玩家的称号状态");
        m.put("command.epithet.admin.unlock.target", "你的称号已被管理员解锁。");
        m.put("command.epithet.admin.unlock.sender", "已解锁 %s 位玩家的称号状态");

        // Dummy Test Commands
        m.put("command.epithet.dummy.spawn.success", "已在面前生成测试假人: %s");
        m.put("command.epithet.dummy.sneak.success", "已设置测试假人潜行状态为: %s");
        m.put("command.epithet.dummy.invisible.success", "已设置测试假人隐身状态为: %s");
        m.put("command.epithet.dummy.title.success", "已设置测试假人佩戴称号为: %s");
        m.put("command.epithet.dummy.look.success", "测试假人已看向当前玩家");
        m.put("command.epithet.dummy.remove.success", "已清除周围 %s 个测试假人");
        m.put("command.epithet.dummy.not_found", "未在附近找到测试假人！");

        // Rarity
        m.put("rarity.epithet.common", "普通");
        m.put("rarity.epithet.uncommon", "稀有");
        m.put("rarity.epithet.rare", "卓越");
        m.put("rarity.epithet.epic", "史诗");
        m.put("rarity.epithet.legendary", "传奇");

        return m;
    }

    public static Map<String, String> buildZhTw() {
        Map<String, String> m = new LinkedHashMap<>();
        // GUI
        m.put("gui.epithet.pause_button_tooltip", "開啟稱號系統");
        m.put("gui.epithet.title_selection.title", "稱號選擇");
        m.put("gui.epithet.button.equip", "佩戴");
        m.put("gui.epithet.button.equip_tooltip", "佩戴此稱號並在頭頂顯示");
        m.put("gui.epithet.button.unequip", "卸下");
        m.put("gui.epithet.button.unequip_tooltip", "卸下當前已佩戴的稱號");
        m.put("gui.epithet.button.locked", "未解鎖");
        m.put("gui.epithet.status.locked_tag", "[已鎖定]");
        m.put("gui.epithet.title.not_unlocked_tooltip", "您尚未解鎖該稱號");
        m.put("gui.epithet.title.locked_tooltip", "稱號已被管理員強制鎖定，無法自行更換");
        m.put("gui.epithet.title.locked_banner", "您的稱號已被管理員強制鎖定");
        m.put("gui.epithet.button.player_selector", "玩家管理");
        m.put("gui.epithet.player_selector.title", "玩家稱號管理器");
        m.put("gui.epithet.player_selector.search", "搜尋玩家");
        m.put("gui.epithet.player_selector.search_hint", "輸入玩家名稱篩選...");
        m.put("gui.epithet.button.lock", "鎖定");
        m.put("gui.epithet.button.lock_tooltip", "鎖定該玩家的稱號更換權限");
        m.put("gui.epithet.button.unlock", "解鎖");
        m.put("gui.epithet.button.unlock_tooltip", "解除鎖定，允許自主更換稱號");
        m.put("gui.epithet.no_title", "未佩戴稱號");
        m.put("gui.epithet.empty_titles", "暫無已解鎖稱號，可通過達成成就或管理員賦予獲取");
        m.put("gui.epithet.button.manage", "稱號管理");
        m.put("gui.epithet.button.manage_tooltip", "管理該玩家的全部稱號與佩戴狀態");
        m.put("gui.epithet.manage.title", "玩家稱號管理");
        m.put("gui.epithet.manage.tab.owned", "已擁有稱號 (%d)");
        m.put("gui.epithet.manage.tab.available", "全服稱號庫 (%d)");
        m.put("gui.epithet.manage.status.locked", "狀態: 已鎖定");
        m.put("gui.epithet.manage.status.unlocked", "狀態: 正常");
        m.put("gui.epithet.manage.status.equipped_badge", "[已佩戴]");
        m.put("gui.epithet.manage.button.set_equipped", "設為佩戴");
        m.put("gui.epithet.manage.button.set_equipped_tooltip", "強制將該玩家的佩戴稱號設為此項");
        m.put("gui.epithet.manage.button.unequip", "卸下");
        m.put("gui.epithet.manage.button.unequip_tooltip", "卸下該玩家當前佩戴的稱號");
        m.put("gui.epithet.manage.button.take", "收回");
        m.put("gui.epithet.manage.button.take_tooltip", "收回該稱號，從玩家已解鎖列表中移除");
        m.put("gui.epithet.manage.button.give", "賦予");
        m.put("gui.epithet.manage.button.give_tooltip", "向該玩家賦予此稱號");
        m.put("gui.epithet.manage.button.add_title", "+ 賦予新稱號");
        m.put("gui.epithet.manage.button.add_title_tooltip", "從全服稱號庫中選擇新稱號賦予給該玩家");
        m.put("gui.epithet.manage.button.back_to_owned", "返回已擁有列表");
        m.put("gui.epithet.manage.empty_owned", "該玩家尚未擁有任何稱號");
        m.put("gui.epithet.manage.empty_available", "該玩家已擁有全服所有可用稱號");
        m.put("gui.epithet.manage.search_hint", "篩選稱號...");

        // Title Creation GUI
        m.put("gui.epithet.button.create_title", "建立稱號");
        m.put("gui.epithet.create.title", "建立自訂稱號");
        m.put("gui.epithet.create.id_tooltip", "稱號唯一識別碼（命名空間:名稱）");
        m.put("gui.epithet.create.name_tooltip", "稱號顯示名稱，支援文字、漸變色及動態流光標籤");
        m.put("gui.epithet.create.color_tooltip", "稱號主顏色（十六進位代碼，如 #FFAA00）");
        m.put("gui.epithet.create.rarity_tooltip", "稱號稀有度（common, uncommon, rare, epic, legendary）");
        m.put("gui.epithet.create.button.save_and_equip", "儲存並佩戴");
        m.put("gui.epithet.create.button.save_only", "僅儲存");
        m.put("gui.epithet.create.label.id", "稱號識別碼 (ID)");
        m.put("gui.epithet.create.label.name", "顯示名稱 (支援漸變色/動畫)");
        m.put("gui.epithet.create.label.desc", "稱號描述 (Description)");
        m.put("gui.epithet.create.label.color", "主色調 (Hex)");
        m.put("gui.epithet.create.label.rarity", "稀有度 (Rarity)");
        m.put("gui.epithet.create.preview_label", "即時預覽");

        // Commands
        m.put("command.epithet.gui.opening", "正在開啟稱號介面...");
        m.put("command.epithet.list.empty", "你尚未擁有任何稱號。");
        m.put("command.epithet.list.header", "=== 你的稱號列表 (共 %s 個) ===");
        m.put("command.epithet.list.equipped", "[已佩戴]");
        m.put("command.epithet.list.locked", "當前稱號已被鎖定，無法更改。");
        m.put("command.epithet.admin.create.success", "成功創建稱號: %s");
        m.put("command.epithet.admin.give.target", "你獲得了新稱號：%s");
        m.put("command.epithet.admin.give.sender", "已向 %s 位玩家賦予稱號 %s");
        m.put("command.epithet.admin.take.target", "你的稱號已被收回：%s");
        m.put("command.epithet.admin.take.sender", "已從 %s 位玩家收回稱號 %s");
        m.put("command.epithet.admin.set.target", "你的佩戴稱號已被設定為：%s");
        m.put("command.epithet.admin.set.sender", "已將 %s 位玩家的當前稱號設為 %s");
        m.put("command.epithet.admin.lock.target", "你的稱號已被管理員鎖定，無法自主更改！");
        m.put("command.epithet.admin.lock.sender", "已鎖定 %s 位玩家的稱號狀態");
        m.put("command.epithet.admin.unlock.target", "你的稱號已被管理員解鎖。");
        m.put("command.epithet.admin.unlock.sender", "已解鎖 %s 位玩家的稱號狀態");

        // Dummy Test Commands
        m.put("command.epithet.dummy.spawn.success", "已在面前生成測試假人: %s");
        m.put("command.epithet.dummy.sneak.success", "已設置測試假人潛行狀態為: %s");
        m.put("command.epithet.dummy.invisible.success", "已設置測試假人隱身狀態為: %s");
        m.put("command.epithet.dummy.title.success", "已設置測試假人佩戴稱號為: %s");
        m.put("command.epithet.dummy.look.success", "測試假人已看向當前玩家");
        m.put("command.epithet.dummy.remove.success", "已清除周圍 %s 個測試假人");
        m.put("command.epithet.dummy.not_found", "未在附近找到測試假人！");

        // Rarity
        m.put("rarity.epithet.common", "普通");
        m.put("rarity.epithet.uncommon", "罕見");
        m.put("rarity.epithet.rare", "稀有");
        m.put("rarity.epithet.epic", "史詩");
        m.put("rarity.epithet.legendary", "傳奇");

        return m;
    }

    public static Map<String, String> buildEnUs() {
        Map<String, String> m = new LinkedHashMap<>();
        // GUI
        m.put("gui.epithet.pause_button_tooltip", "Open Title Selection");
        m.put("gui.epithet.title_selection.title", "Title Selection");
        m.put("gui.epithet.button.equip", "Equip");
        m.put("gui.epithet.button.equip_tooltip", "Equip this title to display on your nametag");
        m.put("gui.epithet.button.unequip", "Unequip");
        m.put("gui.epithet.button.unequip_tooltip", "Remove currently equipped title");
        m.put("gui.epithet.button.locked", "Locked");
        m.put("gui.epithet.status.locked_tag", "[Locked]");
        m.put("gui.epithet.title.not_unlocked_tooltip", "You have not unlocked this title yet");
        m.put("gui.epithet.title.locked_tooltip", "Your title is locked by an administrator and cannot be changed");
        m.put("gui.epithet.title.locked_banner", "Titles are currently locked by an administrator");
        m.put("gui.epithet.button.player_selector", "Players");
        m.put("gui.epithet.player_selector.title", "Player Title Selector");
        m.put("gui.epithet.player_selector.search", "Search Players");
        m.put("gui.epithet.player_selector.search_hint", "Filter players by name...");
        m.put("gui.epithet.button.lock", "Lock");
        m.put("gui.epithet.button.lock_tooltip", "Prevent player from changing titles");
        m.put("gui.epithet.button.unlock", "Unlock");
        m.put("gui.epithet.button.unlock_tooltip", "Allow player to freely change titles");
        m.put("gui.epithet.no_title", "No Title Equipped");
        m.put("gui.epithet.button.manage", "Manage");
        m.put("gui.epithet.button.manage_tooltip", "Manage this player's titles and equipped status");
        m.put("gui.epithet.manage.title", "Player Title Manager");
        m.put("gui.epithet.manage.tab.owned", "Owned Titles (%d)");
        m.put("gui.epithet.manage.tab.available", "Available Titles (%d)");
        m.put("gui.epithet.manage.status.locked", "Status: Locked");
        m.put("gui.epithet.manage.status.unlocked", "Status: Normal");
        m.put("gui.epithet.manage.status.equipped_badge", "[Equipped]");
        m.put("gui.epithet.manage.button.set_equipped", "Equip");
        m.put("gui.epithet.manage.button.set_equipped_tooltip", "Force equip this title for the player");
        m.put("gui.epithet.manage.button.unequip", "Unequip");
        m.put("gui.epithet.manage.button.unequip_tooltip", "Unequip this title from the player");
        m.put("gui.epithet.manage.button.take", "Revoke");
        m.put("gui.epithet.manage.button.take_tooltip", "Revoke this title from the player's collection");
        m.put("gui.epithet.manage.button.give", "Grant");
        m.put("gui.epithet.manage.button.give_tooltip", "Grant this title to the player");
        m.put("gui.epithet.manage.button.add_title", "+ Grant New Title");
        m.put("gui.epithet.manage.button.add_title_tooltip", "Select and grant a new title from the server collection");
        m.put("gui.epithet.manage.button.back_to_owned", "Back to Owned");
        m.put("gui.epithet.manage.empty_owned", "This player does not own any titles yet");
        m.put("gui.epithet.manage.empty_available", "This player already owns all available titles");
        m.put("gui.epithet.manage.search_hint", "Filter titles...");

        // Title Creation GUI
        m.put("gui.epithet.button.create_title", "Create Title");
        m.put("gui.epithet.create.title", "Create Custom Title");
        m.put("gui.epithet.create.id_tooltip", "Unique title identifier (namespace:path)");
        m.put("gui.epithet.create.name_tooltip", "Title display name, supports plain text, gradients, and animated gradient tags");
        m.put("gui.epithet.create.color_tooltip", "Primary title color (hex format, e.g. #FFAA00)");
        m.put("gui.epithet.create.rarity_tooltip", "Title rarity (common, uncommon, rare, epic, legendary)");
        m.put("gui.epithet.create.button.save_and_equip", "Save & Equip");
        m.put("gui.epithet.create.button.save_only", "Save Only");
        m.put("gui.epithet.create.label.id", "Title Identifier (ID)");
        m.put("gui.epithet.create.label.name", "Display Name (Gradients/Anim)");
        m.put("gui.epithet.create.label.desc", "Description");
        m.put("gui.epithet.create.label.color", "Main Color (Hex)");
        m.put("gui.epithet.create.label.rarity", "Rarity");
        m.put("gui.epithet.create.preview_label", "Live Preview");

        // Commands
        m.put("command.epithet.gui.opening", "Opening title selection...");
        m.put("command.epithet.list.empty", "You have no unlocked titles.");
        m.put("command.epithet.list.header", "=== Your Title Collection (%s titles) ===");
        m.put("command.epithet.list.equipped", "[Equipped]");
        m.put("command.epithet.list.locked", "Titles are currently locked and cannot be changed.");
        m.put("command.epithet.admin.create.success", "Successfully created title: %s");
        m.put("command.epithet.admin.give.target", "You have received a new title: %s");
        m.put("command.epithet.admin.give.sender", "Granted title %2$s to %1$s player(s)");
        m.put("command.epithet.admin.take.target", "Your title has been revoked: %s");
        m.put("command.epithet.admin.take.sender", "Revoked title %2$s from %1$s player(s)");
        m.put("command.epithet.admin.set.target", "Your active title has been set to: %s");
        m.put("command.epithet.admin.set.sender", "Set active title of %1$s player(s) to %2$s");
        m.put("command.epithet.admin.lock.target", "Your title has been locked by an administrator!");
        m.put("command.epithet.admin.lock.sender", "Locked titles for %s player(s)");
        m.put("command.epithet.admin.unlock.target", "Your title has been unlocked by an administrator.");
        m.put("command.epithet.admin.unlock.sender", "Unlocked titles for %s player(s)");

        // Dummy Test Commands
        m.put("command.epithet.dummy.spawn.success", "Spawned test dummy in front of you: %s");
        m.put("command.epithet.dummy.sneak.success", "Set test dummy sneaking state to: %s");
        m.put("command.epithet.dummy.invisible.success", "Set test dummy invisible state to: %s");
        m.put("command.epithet.dummy.title.success", "Set test dummy equipped title to: %s");
        m.put("command.epithet.dummy.look.success", "Test dummy is now looking at you");
        m.put("command.epithet.dummy.remove.success", "Removed %s nearby test dummy(ies)");
        m.put("command.epithet.dummy.not_found", "No test dummy found nearby!");

        // Rarity
        m.put("rarity.epithet.common", "Common");
        m.put("rarity.epithet.uncommon", "Uncommon");
        m.put("rarity.epithet.rare", "Rare");
        m.put("rarity.epithet.epic", "Epic");
        m.put("rarity.epithet.legendary", "Legendary");

        return m;
    }

    public static Map<String, String> buildEnGb() {
        Map<String, String> m = new LinkedHashMap<>();
        // GUI
        m.put("gui.epithet.pause_button_tooltip", "Open Title Selection");
        m.put("gui.epithet.title_selection.title", "Title Selection");
        m.put("gui.epithet.button.equip", "Equip");
        m.put("gui.epithet.button.equip_tooltip", "Equip this title to display on your nametag");
        m.put("gui.epithet.button.unequip", "Unequip");
        m.put("gui.epithet.button.unequip_tooltip", "Remove currently equipped title");
        m.put("gui.epithet.button.locked", "Locked");
        m.put("gui.epithet.status.locked_tag", "[Locked]");
        m.put("gui.epithet.title.not_unlocked_tooltip", "You have not unlocked this title yet");
        m.put("gui.epithet.title.locked_tooltip", "Your title is locked by an administrator and cannot be changed");
        m.put("gui.epithet.title.locked_banner", "Titles are currently locked by an administrator");
        m.put("gui.epithet.button.player_selector", "Players");
        m.put("gui.epithet.player_selector.title", "Player Title Selector");
        m.put("gui.epithet.player_selector.search", "Search Players");
        m.put("gui.epithet.player_selector.search_hint", "Filter players by name...");
        m.put("gui.epithet.button.lock", "Lock");
        m.put("gui.epithet.button.lock_tooltip", "Prevent player from changing titles");
        m.put("gui.epithet.button.unlock", "Unlock");
        m.put("gui.epithet.button.unlock_tooltip", "Allow player to freely change titles");
        m.put("gui.epithet.no_title", "No Title Equipped");
        m.put("gui.epithet.button.manage", "Manage");
        m.put("gui.epithet.button.manage_tooltip", "Manage this player's titles and equipped status");
        m.put("gui.epithet.manage.title", "Player Title Manager");
        m.put("gui.epithet.manage.tab.owned", "Owned Titles (%d)");
        m.put("gui.epithet.manage.tab.available", "Available Titles (%d)");
        m.put("gui.epithet.manage.status.locked", "Status: Locked");
        m.put("gui.epithet.manage.status.unlocked", "Status: Normal");
        m.put("gui.epithet.manage.status.equipped_badge", "[Equipped]");
        m.put("gui.epithet.manage.button.set_equipped", "Equip");
        m.put("gui.epithet.manage.button.set_equipped_tooltip", "Force equip this title for the player");
        m.put("gui.epithet.manage.button.unequip", "Unequip");
        m.put("gui.epithet.manage.button.unequip_tooltip", "Unequip this title from the player");
        m.put("gui.epithet.manage.button.take", "Revoke");
        m.put("gui.epithet.manage.button.take_tooltip", "Revoke this title from the player's collection");
        m.put("gui.epithet.manage.button.give", "Grant");
        m.put("gui.epithet.manage.button.give_tooltip", "Grant this title to the player");
        m.put("gui.epithet.manage.button.add_title", "+ Grant New Title");
        m.put("gui.epithet.manage.button.add_title_tooltip", "Select and grant a new title from the server collection");
        m.put("gui.epithet.manage.button.back_to_owned", "Back to Owned");
        m.put("gui.epithet.manage.empty_owned", "This player does not own any titles yet");
        m.put("gui.epithet.manage.empty_available", "This player already owns all available titles");
        m.put("gui.epithet.manage.search_hint", "Filter titles...");

        // Title Creation GUI
        m.put("gui.epithet.button.create_title", "Create Title");
        m.put("gui.epithet.create.title", "Create Custom Title");
        m.put("gui.epithet.create.id_tooltip", "Unique title identifier (namespace:path)");
        m.put("gui.epithet.create.name_tooltip", "Title display name, supports plain text, gradients, and animated gradient tags");
        m.put("gui.epithet.create.color_tooltip", "Primary title colour (hex format, e.g. #FFAA00)");
        m.put("gui.epithet.create.rarity_tooltip", "Title rarity (common, uncommon, rare, epic, legendary)");
        m.put("gui.epithet.create.button.save_and_equip", "Save & Equip");
        m.put("gui.epithet.create.button.save_only", "Save Only");
        m.put("gui.epithet.create.label.id", "Title Identifier (ID)");
        m.put("gui.epithet.create.label.name", "Display Name (Gradients/Anim)");
        m.put("gui.epithet.create.label.desc", "Description");
        m.put("gui.epithet.create.label.color", "Main Colour (Hex)");
        m.put("gui.epithet.create.label.rarity", "Rarity");
        m.put("gui.epithet.create.preview_label", "Live Preview");

        // Commands
        m.put("command.epithet.gui.opening", "Opening title selection...");
        m.put("command.epithet.list.empty", "You have no unlocked titles.");
        m.put("command.epithet.list.header", "=== Your Title Collection (%s titles) ===");
        m.put("command.epithet.list.equipped", "[Equipped]");
        m.put("command.epithet.list.locked", "Titles are currently locked and cannot be changed.");
        m.put("command.epithet.admin.create.success", "Successfully created title: %s");
        m.put("command.epithet.admin.give.target", "You have received a new title: %s");
        m.put("command.epithet.admin.give.sender", "Conferred title %2$s upon %1$s player(s)");
        m.put("command.epithet.admin.take.target", "Your title has been revoked: %s");
        m.put("command.epithet.admin.take.sender", "Revoked title %2$s from %1$s player(s)");
        m.put("command.epithet.admin.set.target", "Your active title has been set to: %s");
        m.put("command.epithet.admin.set.sender", "Set active title of %1$s player(s) to %2$s");
        m.put("command.epithet.admin.lock.target", "Your title has been locked by an administrator!");
        m.put("command.epithet.admin.lock.sender", "Locked titles for %s player(s)");
        m.put("command.epithet.admin.unlock.target", "Your title has been unlocked by an administrator.");
        m.put("command.epithet.admin.unlock.sender", "Unlocked titles for %s player(s)");

        // Dummy Test Commands
        m.put("command.epithet.dummy.spawn.success", "Spawned test dummy in front of you: %s");
        m.put("command.epithet.dummy.sneak.success", "Set test dummy sneaking state to: %s");
        m.put("command.epithet.dummy.invisible.success", "Set test dummy invisible state to: %s");
        m.put("command.epithet.dummy.title.success", "Set test dummy equipped title to: %s");
        m.put("command.epithet.dummy.look.success", "Test dummy is now looking at you");
        m.put("command.epithet.dummy.remove.success", "Removed %s nearby test dummy(ies)");
        m.put("command.epithet.dummy.not_found", "No test dummy found nearby!");

        // Rarity
        m.put("rarity.epithet.common", "Common");
        m.put("rarity.epithet.uncommon", "Uncommon");
        m.put("rarity.epithet.rare", "Rare");
        m.put("rarity.epithet.epic", "Epic");
        m.put("rarity.epithet.legendary", "Legendary");

        return m;
    }
}
