package cn.com.helei.bot.core.mvc.controller;

import cn.com.helei.bot.core.dto.Result;
import cn.com.helei.bot.core.entity.BrowserEnv;
import cn.com.helei.bot.core.entity.DiscordAccount;
import cn.com.helei.bot.core.mvc.service.IBrowserEnvService;
import cn.com.helei.bot.core.mvc.service.IDiscordAccountService;
import cn.com.helei.bot.core.mvc.vo.BotImportVO;
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
    public Result batchAdd(@RequestBody BotImportVO importVO) {
        return discordAccountService.saveDiscordAccounts(importVO.getRawLines());
    }
}
