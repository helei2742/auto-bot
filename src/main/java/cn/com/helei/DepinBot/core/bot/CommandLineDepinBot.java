package cn.com.helei.DepinBot.core.bot;

import cn.com.helei.DepinBot.core.BaseDepinBotConfig;
import cn.com.helei.DepinBot.core.commandMenu.CommandMenuNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Stack;


/**
 * 命令行交互的depin机器人
 */
@Slf4j
@Getter
public abstract class CommandLineDepinBot<Req, Resp> extends AccountAutoManageDepinBot<Req, Resp> {


    public CommandLineDepinBot(BaseDepinBotConfig baseDepinBotConfig) {
        super(baseDepinBotConfig);
    }

    /**
     * 构建command菜单
     *
     * @return 主菜单节点
     */
    protected abstract CommandMenuNode buildMenuNode();


    /**
     * 运行机器人
     *
     * @throws IOException IOException
     */
    protected void doExecute() throws IOException {
        //Step 1 获取输入
        CommandMenuNode mainMenuNode = getMenuNode();
        Terminal terminal = TerminalBuilder.builder().system(true).build();
        LineReader reader = LineReaderBuilder.builder().terminal(terminal).parser(new DefaultParser()).build();

        Stack<CommandMenuNode> menuNodeStack = new Stack<>();
        CommandMenuNode currentMenuNode = mainMenuNode;

        //Step 2 不断监听控制台输入
        while (true) {
            //Step 2.1 获取输入
            String choice = reader.readLine("\n<\n" + getInvokeActionAndMenuNodePrintStr(currentMenuNode) + "请选择>");
            try {
                //Step 2.2 退出
                if ("exit".equals(choice)) {
                    exitHandler();
                    break;
                }

                //Step 2.3 选择操作
                int option = Integer.parseInt(choice.trim());
                if (option == 0) {
                    //返回上一级菜单
                    if (!menuNodeStack.isEmpty()) {
                        currentMenuNode = menuNodeStack.pop();
                    }
                } else if (option > 0 && option <= currentMenuNode.getSubNodeList().size()) {
                    //进入选择的菜单
                    menuNodeStack.push(currentMenuNode);
                    currentMenuNode = currentMenuNode.getSubNodeList().get(option - 1);
                } else {
                    System.out.println("输入无效，请重新输入");
                }

                //终点节点，不进入，直接返回
                if (currentMenuNode.isEnd()) {
                    System.out.println(getInvokeActionAndMenuNodePrintStr(currentMenuNode));
                    currentMenuNode = menuNodeStack.pop();
                }
            } catch (Exception e) {
                System.out.println("输入无效，请重新输入");
            }
        }
    }


    /**
     * 获取菜单， 会放入额外的固定菜单
     *
     * @return CommandMenuNode
     */
    private CommandMenuNode getMenuNode() {
        CommandMenuNode menuNode = buildMenuNode();

        //获取到子类菜单后，给子类菜单添加新的菜单选项
        return new DefaultCommandMenuBuilder(this)
                .addDefaultMenuNode(menuNode);
    }

    /**
     * 退出回调
     */
    protected void exitHandler() {
    }



    /**
     * 执行Action回调，获取当前菜单打印的字符串
     *
     * @param currentMenuNode currentMenuNode
     * @return String
     */
    public String getInvokeActionAndMenuNodePrintStr(CommandMenuNode currentMenuNode) {
        StringBuilder sb = new StringBuilder();
        sb.append(currentMenuNode.getDescribe()).append("\n");

        if (currentMenuNode.getAction() != null) {
            sb.append(currentMenuNode.getAction().get()).append("\n");
        }

        sb.append("选项:\n");
        List<CommandMenuNode> menuNodeList = currentMenuNode.getSubNodeList();
        for (int i = 0; i < menuNodeList.size(); i++) {
            sb.append(i + 1).append(". ").append(menuNodeList.get(i).getTittle()).append("\n");
        }

        sb.append("0. 返回上一级菜单\n");

        return sb.toString();
    }

}
