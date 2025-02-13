package cn.com.helei.bot.core.mvc.controller;

import cn.com.helei.bot.core.dto.Result;
import cn.com.helei.bot.core.mvc.service.IBotAccountContextService;
import cn.com.helei.bot.core.mvc.vo.BotImportVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author com.helei
 * @since 2025-02-05
 */
@RestController
@RequestMapping("/accountContext")
public class BotAccountContextController {

    @Autowired
    private IBotAccountContextService botAccountContextService;

    @PostMapping("/batchAdd")
    public Result batchAdd(@RequestBody BotImportVO importVO) {
        return botAccountContextService.saveBotAccountContext(importVO.getBotId(), importVO.getBotKey(), importVO.getRawLines());
    }
}
