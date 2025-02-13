package cn.com.helei.bot.core.mvc.controller;

import cn.com.helei.bot.core.dto.Result;
import cn.com.helei.bot.core.entity.TwitterAccount;
import cn.com.helei.bot.core.mvc.service.ITwitterAccountService;
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
@RequestMapping("/core/twitterAccount")
public class TwitterAccountController {
    @Autowired
    private ITwitterAccountService twitterAccountService;

    @PostMapping("/batchAdd")
    public Result batchAdd(@RequestBody BotImportVO importVO) {
        return twitterAccountService.saveTwitters(importVO.getRawLines());
    }
}
