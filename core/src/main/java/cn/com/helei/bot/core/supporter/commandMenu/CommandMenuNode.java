package cn.com.helei.bot.core.supporter.commandMenu;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Data
public class CommandMenuNode {

    private final boolean isEnd;

    /**
     * 当前节点被选择时显示的文本
     */
    private String tittle;

    /**
     * 当前节点进入后的describe
     */
    private String describe;

    /**
     * 当前节点调用的函数, 返回内容会显示在describe 和 子节点选项之间
     */
    private Supplier<String> action;

    /**
     * 处理输入
     */
    private Consumer<String> resolveInput;

    /**
     * 当前节点的子节点
     */
    private final List<CommandMenuNode> subNodeList = new ArrayList<>();

    public CommandMenuNode(String tittle, String describe, Supplier<String> action) {
        this(false, tittle, describe, action);
    }

    public CommandMenuNode(boolean isEnd, String tittle, String describe, Supplier<String> action) {
        this.isEnd = isEnd;
        this.tittle = tittle;
        this.describe = describe;
        this.action = action;
    }

    public CommandMenuNode addSubMenu(CommandMenuNode subMenu) {
        this.subNodeList.add(subMenu);
        return this;
    }
}
