package me.dingtou.options.web;

import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.model.OwnerSecurity;
import me.dingtou.options.model.OwnerStrategy;
import me.dingtou.options.service.AdminService;
import me.dingtou.options.web.model.WebResult;
import me.dingtou.options.web.util.SessionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理控制器
 *
 * @author qiyan
 */
@Slf4j
@RestController
@RequestMapping("/admin")
public class WebAdminController {

    @Autowired
    private AdminService adminService;

    /**
     * 获取所有用户期权标的
     *
     * @return 用户期权标的列表
     */
    @RequestMapping(value = "/security/list", method = RequestMethod.GET)
    public WebResult<List<OwnerSecurity>> listSecurities() {
        String owner = SessionUtils.getCurrentOwner();
        List<OwnerSecurity> securities = adminService.listSecurities(owner);
        return WebResult.success(securities);
    }

    /**
     * 保存用户期权标的
     *
     * @param security 用户期权标的
     * @return 保存后的用户期权标的
     */
    @RequestMapping(value = "/security/save", method = RequestMethod.POST)
    public WebResult<OwnerSecurity> saveSecurity(@RequestBody OwnerSecurity security) {
        String owner = SessionUtils.getCurrentOwner();
        security.setOwner(owner);
        OwnerSecurity savedSecurity = adminService.saveSecurity(security);
        return WebResult.success(savedSecurity);
    }

    /**
     * 更新用户期权标的状态
     *
     * @param id     标的ID
     * @param status 状态
     * @return 是否更新成功
     */
    @RequestMapping(value = "/security/status", method = RequestMethod.POST)
    public WebResult<Boolean> updateSecurityStatus(@RequestParam("id") Long id, @RequestParam("status") Integer status) {
        boolean result = adminService.updateSecurityStatus(id, status);
        return WebResult.success(result);
    }

    /**
     * 获取所有用户期权策略
     *
     * @return 用户期权策略列表
     */
    @RequestMapping(value = "/strategy/list", method = RequestMethod.GET)
    public WebResult<List<OwnerStrategy>> listStrategies() {
        String owner = SessionUtils.getCurrentOwner();
        List<OwnerStrategy> strategies = adminService.listStrategies(owner);
        return WebResult.success(strategies);
    }

    /**
     * 保存用户期权策略
     *
     * @param strategy 用户期权策略
     * @return 保存后的用户期权策略
     */
    @RequestMapping(value = "/strategy/save", method = RequestMethod.POST)
    public WebResult<OwnerStrategy> saveStrategy(@RequestBody OwnerStrategy strategy) {
        String owner = SessionUtils.getCurrentOwner();
        strategy.setOwner(owner);
        OwnerStrategy savedStrategy = adminService.saveStrategy(strategy);
        return WebResult.success(savedStrategy);
    }

    /**
     * 更新用户期权策略状态
     *
     * @param id     策略ID
     * @param status 状态
     * @return 是否更新成功
     */
    @RequestMapping(value = "/strategy/status", method = RequestMethod.POST)
    public WebResult<Boolean> updateStrategyStatus(@RequestParam("id") Long id, @RequestParam("status") Integer status) {
        boolean result = adminService.updateStrategyStatus(id, status);
        return WebResult.success(result);
    }
} 