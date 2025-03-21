package me.dingtou.options.web;

import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.model.OwnerTradeTask;
import me.dingtou.options.service.TradeTaskService;
import me.dingtou.options.web.model.WebResult;
import me.dingtou.options.web.util.SessionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 交易任务控制器
 *
 * @author qiyan
 */
@Slf4j
@RestController
public class WebTradeTaskController {

    @Autowired
    private TradeTaskService tradeTaskService;

    /**
     * 查询用户的交易任务
     *
     * @return 交易任务列表
     */
    @GetMapping("/task/list")
    public WebResult<List<OwnerTradeTask>> listTasks() {
        try {
            String owner = SessionUtils.getCurrentOwner();
            return WebResult.success(tradeTaskService.queryTradeTask(owner));
        } catch (Exception e) {
            log.error("查询交易任务失败", e);
            return WebResult.failure("查询交易任务失败: " + e.getMessage());
        }
    }

    /**
     * 根据会话ID查询交易任务
     *
     * @param sessionId 会话ID
     * @return 交易任务列表
     */
    @GetMapping("/task/list/session")
    public WebResult<List<OwnerTradeTask>> listTasksBySessionId(@RequestParam("sessionId") String sessionId) {
        try {
            String owner = SessionUtils.getCurrentOwner();
            return WebResult.success(tradeTaskService.queryTradeTaskBySessionId(owner, sessionId));
        } catch (Exception e) {
            log.error("根据会话ID查询交易任务失败", e);
            return WebResult.failure("根据会话ID查询交易任务失败: " + e.getMessage());
        }
    }

    /**
     * 查询待执行的交易任务
     *
     * @return 交易任务列表
     */
    @GetMapping("/task/list/pending")
    public WebResult<List<OwnerTradeTask>> listPendingTasks() {
        try {
            String owner = SessionUtils.getCurrentOwner();
            return WebResult.success(tradeTaskService.queryPendingTradeTask(owner));
        } catch (Exception e) {
            log.error("查询待执行的交易任务失败", e);
            return WebResult.failure("查询待执行的交易任务失败: " + e.getMessage());
        }
    }

    /**
     * 采纳交易任务
     *
     * @param taskId 任务ID
     * @return 是否成功
     */
    @PostMapping("/task/execute")
    public WebResult<Boolean> executeTask(@RequestParam("taskId") Long taskId) {
        try {
            String owner = SessionUtils.getCurrentOwner();
            boolean result = tradeTaskService.executeTradeTask(owner, taskId);
            return WebResult.success(result);
        } catch (Exception e) {
            log.error("采纳交易任务失败", e);
            return WebResult.failure("采纳交易任务失败: " + e.getMessage());
        }
    }

    /**
     * 取消交易任务
     *
     * @param taskId 任务ID
     * @return 是否成功
     */
    @PostMapping("/task/cancel")
    public WebResult<Boolean> cancelTask(@RequestParam("taskId") Long taskId) {
        try {
            String owner = SessionUtils.getCurrentOwner();
            boolean result = tradeTaskService.cancelTradeTask(owner, taskId);
            return WebResult.success(result);
        } catch (Exception e) {
            log.error("取消交易任务失败", e);
            return WebResult.failure("取消交易任务失败: " + e.getMessage());
        }
    }

    /**
     * 删除交易任务
     *
     * @param taskId 任务ID
     * @return 是否成功
     */
    @DeleteMapping("/task/delete")
    public WebResult<Boolean> deleteTask(@RequestParam("taskId") Long taskId) {
        try {
            String owner = SessionUtils.getCurrentOwner();
            boolean result = tradeTaskService.deleteTradeTask(owner, taskId);
            return WebResult.success(result);
        } catch (Exception e) {
            log.error("删除交易任务失败", e);
            return WebResult.failure("删除交易任务失败: " + e.getMessage());
        }
    }

    /**
     * 编辑交易任务
     *
     * @param taskId 任务ID
     * @param tradeTask 任务信息
     * @return 是否成功
     */
    @PostMapping("/task/update")
    public WebResult<Boolean> updateTask(@RequestParam("taskId") Long taskId, @RequestBody OwnerTradeTask tradeTask) {
        try {
            String owner = SessionUtils.getCurrentOwner();
            // 设置任务ID
            tradeTask.setId(taskId);
            boolean result = tradeTaskService.updateTradeTask(owner, tradeTask);
            return WebResult.success(result);
        } catch (Exception e) {
            log.error("编辑交易任务失败", e);
            return WebResult.failure("编辑交易任务失败: " + e.getMessage());
        }
    }
} 