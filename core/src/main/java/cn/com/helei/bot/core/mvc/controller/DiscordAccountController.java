package cn.com.helei.bot.core.mvc.controller;

import cn.com.helei.bot.core.dto.Result;
import cn.com.helei.bot.core.entity.BrowserEnv;
import cn.com.helei.bot.core.entity.DiscordAccount;
import cn.com.helei.bot.core.mvc.service.IBrowserEnvService;
import cn.com.helei.bot.core.mvc.service.IDiscordAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author com.helei
 * @since 2025-02-05
 */
@RestController
@RequestMapping("/discordAccount")
public class DiscordAccountController {
    @Autowired
    private IDiscordAccountService discordAccountService;

    @PostMapping("/batchAdd")
    public Result batchAdd(@RequestBody List<DiscordAccount> discordAccounts) {
        if (discordAccounts == null) {
            return Result.fail("参数不能为空");
        }
        boolean b = discordAccountService.saveBatch(discordAccounts);
        return b ? Result.ok() : Result.fail("批量添加discord账号失败");
    }
}
