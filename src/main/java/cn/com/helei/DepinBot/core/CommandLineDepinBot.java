package cn.com.helei.DepinBot.core;

import cn.com.helei.DepinBot.core.commandMenu.CommandMenuNode;
import cn.com.helei.DepinBot.core.constants.DepinBotStatus;
import cn.com.helei.DepinBot.core.dto.AccountContext;
import cn.com.helei.DepinBot.core.env.BrowserEnvPool;
import cn.com.helei.DepinBot.core.exception.DepinBotStartException;
import cn.com.helei.DepinBot.core.exception.DepinBotStatusException;
import cn.com.helei.DepinBot.core.network.NetworkProxyPool;
import cn.com.helei.DepinBot.core.supporter.AccountContextManager;
import cn.com.helei.DepinBot.core.util.NamedThreadFactory;
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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * 命令行交互的depin机器人
 */
@Slf4j
@Getter
public abstract class CommandLineDepinBot<Req, Resp> {

    private final ExecutorService executorService;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    /**
     * 代理池
     */
    private final NetworkProxyPool proxyPool;

    /**
     * 浏览器环境池
     */
    private final BrowserEnvPool browserEnvPool;

    /**
     * 配置
     */
    private final BaseDepinBotConfig baseDepinBotConfig;

    /**
     * 账户管理器
     */
    private final AccountContextManager accountContextManager;

    /**
     * 状态
     */
    private DepinBotStatus status = DepinBotStatus.NEW;

    /**
     * 是否开始过链接所有账号
     */
    private final AtomicBoolean isStartAccountConnected = new AtomicBoolean(false);


    public CommandLineDepinBot(BaseDepinBotConfig baseDepinBotConfig) {
        this.baseDepinBotConfig = baseDepinBotConfig;
        this.executorService = Executors.newThreadPerTaskExecutor(new NamedThreadFactory(baseDepinBotConfig.getName() + "-executor"));

        this.proxyPool = NetworkProxyPool.loadYamlNetworkPool(baseDepinBotConfig.getNetworkPoolConfig());
        this.browserEnvPool = BrowserEnvPool.loadYamlBrowserEnvPool(baseDepinBotConfig.getBrowserEnvPoolConfig());

        this.accountContextManager = new AccountContextManager(this);
        init();
    }

    /**
     * 构建command菜单
     *
     * @return 主菜单节点
     */
    protected abstract CommandMenuNode buildMenuNode();

    /**
     * 使用accountContext构建AbstractDepinWSClient
     *
     * @param accountContext accountContext
     * @return AbstractDepinWSClient
     */
    public abstract AbstractDepinWSClient<Req, Resp> buildAccountWSClient(AccountContext accountContext);


    /**
     * 当账户链接时调用
     *
     * @param accountContext accountContext
     * @param success        是否成功
     */
    public abstract void whenAccountConnected(AccountContext accountContext, Boolean success);


    /**
     * 初始化方法
     */
    public void init() {
        updateState(DepinBotStatus.INIT);
        try {
            //Step 1 初始化账号
            accountContextManager.initAccounts();

            //更新状态
            updateState(DepinBotStatus.INIT_FINISH);
        } catch (Exception e) {
            log.error("初始化CommandLineDepinBot发生错误", e);
            updateState(DepinBotStatus.INIT_ERROR);
        }
    }


    /**
     * 启动bot
     *
     * @throws DepinBotStartException DepinBotStartException
     */
    public void start() throws DepinBotStartException {
        updateState(DepinBotStatus.STARTING);
        log.info("正在启动Depin Bot");
        try {
            CountDownLatch startLatch = new CountDownLatch(1);
            //启动命令行交互的线程
            asyncExecute(startLatch);

            log.info("Depin Bot启动完毕");

            updateState(DepinBotStatus.RUNNING);
            startLatch.await();

        } catch (Exception e) {
            updateState(DepinBotStatus.SHUTDOWN);
            throw new DepinBotStartException("启动CommandLineDepinBot发生错误", e);
        }
    }


    /**
     * 添加定时任务
     *
     * @param runnable runnable
     * @param delay    delay
     * @param timeUnit timeUnit
     */
    public void addTimer(Runnable runnable, long delay, TimeUnit timeUnit) {
        scheduler.scheduleAtFixedRate(runnable, 0, delay, timeUnit);
    }

    /**
     * 异步启动
     */
    private void asyncExecute(CountDownLatch startLatch) {
        Thread commandInputThread = new Thread(() -> {
            try {
                doExecute();
            } catch (IOException e) {
                log.error("控制台输入发生错误", e);
            } finally {
                startLatch.countDown();
            }
        }, "command-input");
        commandInputThread.setDaemon(true);
        commandInputThread.start();
    }

    /**
     * 运行机器人
     *
     * @throws IOException IOException
     */
    private void doExecute() throws IOException {
        //Step 1 获取输入
        CommandMenuNode mainMenuNode = getMenuNode();
        Terminal terminal = TerminalBuilder.builder().system(true).build();
        LineReader reader = LineReaderBuilder.builder().terminal(terminal).parser(new DefaultParser()).build();

        Stack<CommandMenuNode> menuNodeStack = new Stack<>();
        CommandMenuNode currentMenuNode = mainMenuNode;

        //Step 2 不断监听控制台输入
        while (true) {
            //Step 2.1 获取输入
            String choice = reader.readLine("\n<\n" + getMenuNodePrintStr(currentMenuNode) + "请选择>");
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
     * 开始所有账户的连接
     *
     * @return String 打印的消息
     */
    public String startAccountDepinClient() {
        if (isStartAccountConnected.compareAndSet(false, true)) {
            accountContextManager
                    .allAccountConnectExecute()
                    .exceptionally(throwable -> {
                        log.error("开始所有账户连接时发生异常", throwable);
                        return null;
                    });
            return "已开始账号链接任务";
        }

        return "已提交过建立连接任务";
    }


    /**
     * 更新DepinBotStatus
     *
     * @param newStatus 新状态
     */
    private synchronized void updateState(DepinBotStatus newStatus) throws DepinBotStatusException {
        boolean b = true;
        if (newStatus.equals(DepinBotStatus.SHUTDOWN)) {
            status = DepinBotStatus.SHUTDOWN;
            b = false;
        } else {
            b = switch (status) {
                //当前为NEW，新状态才能为NEW,SHUTDOWN
                case NEW -> DepinBotStatus.INIT.equals(newStatus);
                //当前为INIT，新状态只能为INIT_FINISH、INIT_ERROR,SHUTDOWN
                case INIT -> newStatus.equals(DepinBotStatus.INIT_FINISH)
                        || newStatus.equals(DepinBotStatus.INIT_ERROR);
                //当前为INIT_ERROR,新状态只能为ACCOUNT_LOADING, SHUTDOWN
                case INIT_ERROR -> newStatus.equals(DepinBotStatus.INIT);
                //当前状态为INIT_FINISH，新状态只能为ACCOUNT_LIST_CONNECT, SHUTDOWN
                case INIT_FINISH -> newStatus.equals(DepinBotStatus.STARTING);
                //当前状态为STARING，新状态只能为RUNNING,SHUTDOWN
                case STARTING -> newStatus.equals(DepinBotStatus.RUNNING);
                //RUNNING，新状态只能为 SHUTDOWN
                case RUNNING -> false;
                case SHUTDOWN -> false;
            };
        }


        if (b) {
            log.info("CommandLineDepinBot Status change [{}] => [{}]", status, newStatus);
            this.status = newStatus;
        } else {
            throw new DepinBotStatusException(String.format("Depin Bot Status不能从[%s]->[%s]", status, newStatus));
        }
    }

    /**
     * 获取当前菜单打印的字符串
     *
     * @param currentMenuNode currentMenuNode
     * @return String
     */
    public String getMenuNodePrintStr(CommandMenuNode currentMenuNode) {
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
