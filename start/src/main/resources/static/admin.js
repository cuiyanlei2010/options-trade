/**
 * 期权交易管理系统 - 管理页面
 */
layui.use(['table', 'form', 'layer', 'util', 'element'], function () {
    const table = layui.table;
    const form = layui.form;
    const layer = layui.layer;
    const util = layui.util;
    const element = layui.element;

    // 公共函数区域
    const CommonUtils = {
        // 加载标的列表
        loadSecurityList: function(selectElementId, callback) {
            $.ajax({
                url: '/admin/security/list',
                type: 'GET',
                success: function (res) {
                    if (res.success) {
                        var securityList = res.data;
                        var securitySelect = $(selectElementId);
                        securitySelect.empty();
                        securitySelect.append('<option value="">请选择标的代码</option>');
                        $.each(securityList, function (index, item) {
                            securitySelect.append('<option value="' + item.code + '">' + item.code + ' - ' + item.name + '</option>');
                        });
                        form.render('select');
                        if (callback) callback(true);
                    } else {
                        layer.msg('加载标的列表失败：' + res.message, {icon: 2});
                        if (callback) callback(false);
                    }
                },
                error: function(xhr, status, error) {
                    console.error('加载标的列表请求失败：', error);
                    layer.msg('加载标的列表请求失败，请检查网络连接', {icon: 2});
                    if (callback) callback(false);
                }
            });
        },

        // 加载策略代码列表
        loadStrategyList: function(callback) {
            $.ajax({
                url: '/admin/knowledge/listByType?type=1',
                type: 'GET',
                success: function (res) {
                    if (res.success) {
                        var strategyList = res.data;
                        var strategySelect = $('select[name="strategyCode"]');
                        strategySelect.empty();
                        strategySelect.append('<option value="">请在【知识库管理】添加期权策略知识</option>');
                        $.each(strategyList, function (index, item) {
                            strategySelect.append('<option value="' + item.code + '">' + item.title + '</option>');
                        });
                        form.render('select');
                        if (callback) callback(true);
                    } else {
                        layer.msg('加载策略代码列表失败：' + res.message, {icon: 2});
                        if (callback) callback(false);
                    }
                },
                error: function(xhr, status, error) {
                    console.error('加载策略代码列表请求失败：', error);
                    layer.msg('加载策略代码列表请求失败，请检查网络连接', {icon: 2});
                    if (callback) callback(false);
                }
            });
        },

        // 根据策略代码显示或隐藏特定配置
        toggleStrategyConfig: function(strategyCode) {
            if (strategyCode === 'wheel_strategy') {
                $('#wheelStrategyConfig').show();
            } else {
                $('#wheelStrategyConfig').hide();
                // 清空sellPutStrikePrice字段
                $('input[name="sellPutStrikePrice"]').val('');
            }
        },

        // 处理ext字段数据
        processExtData: function(data) {
            if (data.ext && typeof data.ext === 'object') {
                if (data.ext.wheel_sellput_strike_price) {
                    $('input[name="sellPutStrikePrice"]').val(data.ext.wheel_sellput_strike_price);
                }
                if (data.ext.initial_stock_num) {
                    $('input[name="initialStockNum"]').val(data.ext.initial_stock_num);
                }
                if (data.ext.initial_stock_cost) {
                    $('input[name="initialStockCost"]').val(data.ext.initial_stock_cost);
                }
            }
        },

        // 处理表单提交数据
        processFormData: function(formData) {
            // 处理ext字段
            if (!formData.ext || formData.ext === '') {
                formData.ext = {};
            } else if (typeof formData.ext === 'string') {
                try {
                    formData.ext = JSON.parse(formData.ext);
                } catch (e) {
                    formData.ext = {};
                }
            }

            // 处理车轮策略特有的配置
            if (formData.strategyCode === 'wheel_strategy' && formData.sellPutStrikePrice) {
                formData.ext.wheel_sellput_strike_price = formData.sellPutStrikePrice;
            }

            // 处理通用配置 - 初始股票数
            if (formData.initialStockNum) {
                formData.ext.initial_stock_num = formData.initialStockNum;
            }

            // 处理通用配置 - 初始股票成本价
            if (formData.initialStockCost) {
                formData.ext.initial_stock_cost = formData.initialStockCost;
            }

            // 删除临时字段
            delete formData.sellPutStrikePrice;
            delete formData.initialStockNum;
            delete formData.initialStockCost;

            return formData;
        },

        // 保存策略数据
        saveStrategy: function(formData, layerIndex, tableInstance) {
            $.ajax({
                url: '/admin/strategy/save',
                type: 'POST',
                contentType: 'application/json',
                data: JSON.stringify(formData),
                success: function (res) {
                    if (res.success) {
                        layer.close(layerIndex);
                        layer.msg('保存成功');
                        tableInstance.reload();
                    } else {
                        layer.msg('保存失败：' + res.message);
                    }
                },
                error: function(xhr, status, error) {
                    console.error('保存策略失败:', xhr.responseText);
                    layer.msg('保存策略失败: ' + error);
                }
            });
        },

        // 初始化策略表单
        initStrategyForm: function(isEdit, data, layerIndex, tableInstance) {
            // 先渲染表单
            form.render();

            var loadDataCount = 0;
            var totalLoads = 2;
            
            function checkLoadComplete() {
                loadDataCount++;
                if (loadDataCount === totalLoads) {
                    if (isEdit && data) {
                        // 编辑模式：设置表单值
                        form.val('strategyForm', data);
                        CommonUtils.processExtData(data);
                        CommonUtils.toggleStrategyConfig(data.strategyCode);
                    } else {
                        // 新增模式：默认隐藏车轮策略配置
                        $('#wheelStrategyConfig').hide();
                    }
                    form.render('select');
                }
            }

            // 加载数据
            CommonUtils.loadSecurityList('#securitySelect', checkLoadComplete);
            CommonUtils.loadStrategyList(checkLoadComplete);

            // 监听策略代码选择变化
            form.on('select(strategyCode)', function(selectData){
                CommonUtils.toggleStrategyConfig(selectData.value);
            });

            // 监听表单提交事件
            form.on('submit(strategySubmit)', function (submitData) {
                var processedData = CommonUtils.processFormData(submitData.field);
                console.log('提交的数据:', JSON.stringify(processedData));
                CommonUtils.saveStrategy(processedData, layerIndex, tableInstance);
                return false;
            });
        }
    };

    // 期权标的表格
    const securityTable = table.render({
        elem: '#securityTable',
        url: '/admin/security/list',
        page: true,
        toolbar: true,
        defaultToolbar: ['filter', 'exports', 'print'],
        cols: [[
            //{type: 'checkbox'},
            //{field: 'id', title: 'ID', sort: true, width: 200},
            {field: 'code', title: '标的代码', width: 100},
            {field: 'name', title: '标的名称', width: 200},
            {field: 'market', title: '市场', width: 80},
            {
                field: 'createTime', title: '创建时间', width: 200, sort: true,
                templet: function (d) {
                    return util.toDateString(d.createTime);
                }
            },
            {
                field: 'status', title: '状态', width: 80,
                templet: function (d) {
                    return d.status === 1 ?
                        '<span class="layui-badge layui-bg-green">启用</span>' :
                        '<span class="layui-badge layui-bg-gray">禁用</span>';
                }
            },
            {title: '操作', toolbar: '#securityTableBar', width: 180}
        ]],
        response: {
            statusCode: 0
        },
        parseData: function (res) {
            return {
                "code": res.success ? 0 : 1,
                "msg": res.message,
                "count": res.data.total,
                "data": res.data.records || []
            };
        }
    });

    // 期权策略表格
    let strategyTable;

    // 账户表格
    let accountTable;

    // 初始化策略表格
    function initStrategyTable() {
        strategyTable = table.render({
            elem: '#strategyTable',
            url: '/admin/strategy/list',
            page: true,
            toolbar: true,
            defaultToolbar: ['filter', 'exports', 'print'],
            cols: [[
                //{type: 'checkbox'},
                {field: 'strategyId', title: '策略ID', sort: true, width: 280},
                {field: 'strategyName', title: '策略名称', width: 200},
                {field: 'strategyCode', title: '策略类型', width: 160},
                {field: 'stage', title: '策略阶段', width: 100},
                {field: 'code', title: '标的代码', width: 100},
                {field: 'lotSize', title: '合约股数', width: 100},
                {
                    field: 'startTime', title: '开始时间', width: 180, sort: true,
                    templet: function (d) {
                        return util.toDateString(d.startTime);
                    }
                },
                {
                    field: 'ext', title: '扩展配置', width: 300, templet: function(d) {
                        var extStr = '';
                        if (d.ext && typeof d.ext === 'object') {
                            if (d.ext.initial_stock_num) {
                                extStr += '初始股票数:' + d.ext.initial_stock_num+'; ';
                            }
                            if (d.ext.initial_stock_cost) {
                                extStr += '成本价:' + d.ext.initial_stock_cost+'; ';
                            }
                            if (d.ext.wheel_sellput_strike_price) {
                                extStr += 'SellPut行权价:' + d.ext.wheel_sellput_strike_price+'; ';
                            }
                        }
                        return extStr;
                    }
                },
                {
                    field: 'status', title: '状态', width: 80,
                    templet: function (d) {
                        return d.status === 1 ?
                            '<span class="layui-badge layui-bg-green">启用</span>' :
                            '<span class="layui-badge layui-bg-gray">禁用</span>';
                    }
                },
                {title: '操作', toolbar: '#strategyTableBar', width: 180}
            ]],
            response: {
                statusCode: 0
            },
            parseData: function (res) {
                return {
                    "code": res.success ? 0 : 1,
                    "msg": res.message,
                    "count": res.data.total,
                    "data": res.data.records || []
                };
            }
        });
    }

    // 初始化账户表格
    function initAccountTable() {
        accountTable = table.render({
            elem: '#accountTable',
            url: '/admin/account/list',
            page: true,
            toolbar: true,
            defaultToolbar: ['filter', 'exports', 'print'],
            cols: [[
                //{type: 'checkbox'},
                {field: 'owner', title: 'Owner', width: 200},
                {field: 'accountId', title: '账号', width: 200},
                {field: 'platform', title: '平台', width: 100},
                {field: 'market', title: '市场', width: 80},
                {
                    field: 'createTime', title: '创建时间', width: 200, sort: true,
                    templet: function (d) {
                        return util.toDateString(d.createTime);
                    }
                },
                {title: '操作', toolbar: '#accountTableBar', width: 150}
            ]],
            response: {
                statusCode: 0
            },
            parseData: function (res) {
                return {
                    "code": res.success ? 0 : 1,
                    "msg": res.message,
                    "count": res.data.total,
                    "data": res.data.records || []
                };
            }
        });
    }

    // 监听选项卡切换事件
    element.on('tab(adminTab)', function(data){
        if(data.index === 1) {
            // 切换到策略管理选项卡
            if(!strategyTable) {
                initStrategyTable();
            } else {
                strategyTable.reload();
            }
        } else if(data.index === 0) {
            // 切换到标的管理选项卡
            securityTable.reload();
        } else if(data.index === 2) {
            // 切换到账户管理选项卡
            if(!accountTable) {
                initAccountTable();
            } else {
                accountTable.reload();
            }
        }
    });

    // 初始化策略表格
    initStrategyTable();

    // 初始化账户表格
    initAccountTable();

    // 期权标的表格工具条事件
    table.on('tool(securityTable)', function (obj) {
        const data = obj.data;
        if (obj.event === 'edit') {
            // 编辑
            layer.open({
                type: 1,
                title: '编辑期权标的',
                area: ['500px', '400px'],
                content: $('#securityFormTpl').html(),
                success: function (layero, index) {
                    form.val('securityForm', data);
                    form.render();

                    // 监听表单提交事件
                    form.on('submit(securitySubmit)', function (data) {
                        $.ajax({
                            url: '/admin/security/save',
                            type: 'POST',
                            contentType: 'application/json',
                            data: JSON.stringify(data.field),
                            success: function (res) {
                                if (res.success) {
                                    layer.close(index);
                                    layer.msg('保存成功');
                                    securityTable.reload();
                                } else {
                                    layer.msg('保存失败：' + res.message);
                                }
                            }
                        });
                        return false;
                    });
                }
            });
        } else if (obj.event === 'delete') {
            // 删除
            layer.confirm('确定要删除该期权标的吗？', function (index) {
                $.ajax({
                    url: '/admin/security/status',
                    type: 'POST',
                    data: {
                        id: data.id,
                        status: 0
                    },
                    success: function (res) {
                        if (res.success) {
                            layer.msg('删除成功');
                            securityTable.reload();
                        } else {
                            layer.msg('删除失败：' + res.message);
                        }
                    }
                });
                layer.close(index);
            });
        } else if (obj.event === 'toggle') {
            // 切换状态
            const newStatus = data.status === 1 ? 0 : 1;
            const statusText = newStatus === 1 ? '启用' : '禁用';
            $.ajax({
                url: '/admin/security/status',
                type: 'POST',
                data: {
                    id: data.id,
                    status: newStatus
                },
                success: function (res) {
                    if (res.success) {
                        layer.msg(statusText + '成功');
                        securityTable.reload();
                    } else {
                        layer.msg(statusText + '失败：' + res.message);
                    }
                }
            });
        }
    });

    // 期权策略表格工具条事件
    table.on('tool(strategyTable)', function (obj) {
        const data = obj.data;
        if (obj.event === 'edit') {
            // 编辑
            layer.open({
                type: 1,
                title: '编辑期权策略',
                area: ['500px', '600px'],
                content: $('#strategyFormTpl').html(),
                success: function (layero, index) {
                    CommonUtils.initStrategyForm(true, data, index, strategyTable);
                }
            });
        } else if (obj.event === 'delete') {
            // 删除
            layer.confirm('确定要删除该期权策略吗？', function (index) {
                $.ajax({
                    url: '/admin/strategy/status',
                    type: 'POST',
                    data: {
                        id: data.id,
                        status: 0
                    },
                    success: function (res) {
                        if (res.success) {
                            layer.msg('删除成功');
                            strategyTable.reload();
                        } else {
                            layer.msg('删除失败：' + res.message);
                        }
                    }
                });
                layer.close(index);
            });
        } else if (obj.event === 'toggle') {
            // 切换状态
            const newStatus = data.status === 1 ? 0 : 1;
            const statusText = newStatus === 1 ? '启用' : '禁用';
            $.ajax({
                url: '/admin/strategy/status',
                type: 'POST',
                data: {
                    id: data.id,
                    status: newStatus
                },
                success: function (res) {
                    if (res.success) {
                        layer.msg(statusText + '成功');
                        strategyTable.reload();
                    } else {
                        layer.msg(statusText + '失败：' + res.message);
                    }
                }
            });
        }
    });

    // 添加期权标的按钮事件
    $('#addSecurityBtn').click(function () {
        layer.open({
            type: 1,
            title: '添加期权标的',
            area: ['500px', '400px'],
            content: $('#securityFormTpl').html(),
            success: function (layero, index) {
                form.render();

                // 监听表单提交事件
                form.on('submit(securitySubmit)', function (data) {
                    $.ajax({
                        url: '/admin/security/save',
                        type: 'POST',
                        contentType: 'application/json',
                        data: JSON.stringify(data.field),
                        success: function (res) {
                            if (res.success) {
                                layer.close(index);
                                layer.msg('保存成功');
                                securityTable.reload();
                            } else {
                                layer.msg('保存失败：' + res.message);
                            }
                        }
                    });
                    return false;
                });
            }
        });
    });

    // 添加期权策略按钮事件
    $('#addStrategyBtn').click(function () {
        layer.open({
            type: 1,
            title: '添加期权策略',
            area: ['500px', '600px'],
            content: $('#strategyFormTpl').html(),
            success: function (layero, index) {
                CommonUtils.initStrategyForm(false, null, index, strategyTable);
            }
        });
    });

    // 刷新期权标的表格按钮事件
    $('#refreshSecurityBtn').click(function () {
        securityTable.reload();
    });

    // 刷新期权策略表格按钮事件
    $('#refreshStrategyBtn').click(function () {
        strategyTable.reload();
    });

    // 批量删除期权标的按钮事件
    $('#deleteSecurityBtn').click(function () {
        const checkStatus = table.checkStatus('securityTable');
        const data = checkStatus.data;
        if (data.length === 0) {
            layer.msg('请选择要删除的期权标的');
            return;
        }
        layer.confirm('确定要删除选中的' + data.length + '个期权标的吗？', function (index) {
            let count = 0;
            let success = 0;
            data.forEach(function (item) {
                $.ajax({
                    url: '/admin/security/status',
                    type: 'POST',
                    async: false,
                    data: {
                        id: item.id,
                        status: 0
                    },
                    success: function (res) {
                        count++;
                        if (res.success) {
                            success++;
                        }
                        if (count === data.length) {
                            layer.msg('成功删除' + success + '个期权标的');
                            securityTable.reload();
                        }
                    }
                });
            });
            layer.close(index);
        });
    });

    // 批量删除期权策略按钮事件
    $('#deleteStrategyBtn').click(function () {
        const checkStatus = table.checkStatus('strategyTable');
        const data = checkStatus.data;
        if (data.length === 0) {
            layer.msg('请选择要删除的期权策略');
            return;
        }
        layer.confirm('确定要删除选中的' + data.length + '个期权策略吗？', function (index) {
            let count = 0;
            let success = 0;
            data.forEach(function (item) {
                $.ajax({
                    url: '/admin/strategy/status',
                    type: 'POST',
                    async: false,
                    data: {
                        id: item.id,
                        status: 0
                    },
                    success: function (res) {
                        count++;
                        if (res.success) {
                            success++;
                        }
                        if (count === data.length) {
                            layer.msg('成功删除' + success + '个期权策略');
                            strategyTable.reload();
                        }
                    }
                });
            });
            layer.close(index);
        });
    });

    // 获取账户扩展字段元数据
    function getAccountExtFields() {
        return $.ajax({
            url: '/field/ext/account',
            type: 'GET',
            async: false
        }).responseJSON.data;
    }

    // 根据字段元数据动态渲染表单
    function renderAccountExtFields(extFields, extData) {
        let html = '';
        // 渲染字段
        extFields.forEach(field => {
            const value = extData && extData[field.key] ? extData[field.key] : '';

            if (field.type === 'text') {
                html += `
        <div class="layui-form-item">
            <label class="layui-form-label">${field.desc}</label>
            <div class="layui-input-block">
                <input type="text" id="${field.key}" placeholder="请输入${field.desc}" autocomplete="off" class="layui-input" value="${value}">
            </div>
        </div>`;
            } else if (field.type === 'textarea') {
                html += `
        <div class="layui-form-item">
            <label class="layui-form-label">${field.desc}</label>
            <div class="layui-input-block">
                <textarea id="${field.key}" placeholder="请输入${field.desc}" class="layui-textarea" rows="4">${value}</textarea>
            </div>
        </div>`;
            }
        });

        return html;
    }

    // 账户表格工具条事件
    table.on('tool(accountTable)', function (obj) {
        const data = obj.data;
        if (obj.event === 'edit') {
            // 编辑
            layer.open({
                type: 1,
                title: '编辑账户',
                area: ['800px', '800px'],
                content: $('#accountFormTpl').html(),
                success: function (layero, index) {
                    // 设置表单值
                    form.val('accountForm', data);

                    // 获取账户扩展字段元数据
                    const extFields = getAccountExtFields();

                    // 处理ext字段
                    if (data.ext) {
                        // 动态渲染扩展字段
                        const extHtml = renderAccountExtFields(extFields, data.ext);
                        $('#accountExtFields').html(extHtml);
                    }

                    // 渲染表单
                    form.render();

                    // 监听表单提交事件
                    form.on('submit(accountSubmit)', function (data) {
                        // 处理ext字段
                        const ext = {};

                        // 根据字段元数据收集扩展字段值
                        extFields.forEach(field => {
                            const value = $('#' + field.key).val();
                            if (value !== undefined && value !== null && value !== '') {
                                ext[field.key] = value;
                            }
                        });

                        // 设置ext字段
                        data.field.ext = ext;

                        // 确保ext字段是对象而不是字符串
                        const formData = {...data.field};

                        $.ajax({
                            url: '/admin/account/save',
                            type: 'POST',
                            contentType: 'application/json',
                            data: JSON.stringify(formData),
                            success: function (res) {
                                if (res.success) {
                                    layer.close(index);
                                    layer.msg('保存成功');
                                    accountTable.reload();
                                } else {
                                    layer.msg('保存失败：' + res.message);
                                }
                            },
                            error: function(xhr, status, error) {
                                console.error('保存账户失败:', xhr.responseText);
                                layer.msg('保存账户失败: ' + error);
                            }
                        });
                        return false;
                    });
                }
            });
        } else if (obj.event === 'delete') {
            // 删除
            layer.confirm('确定要删除该账户吗？', function (index) {
                $.ajax({
                    url: '/admin/account/delete',
                    type: 'POST',
                    data: {
                        id: data.id
                    },
                    success: function (res) {
                        if (res.success) {
                            layer.msg('删除成功');
                            accountTable.reload();
                        } else {
                            layer.msg('删除失败：' + res.message);
                        }
                    }
                });
                layer.close(index);
            });
        }
    });

    // 添加账户按钮事件
    $('#addAccountBtn').click(function () {
        layer.open({
            type: 1,
            title: '添加账户',
            area: ['800px', '800px'],
            content: $('#accountFormTpl').html(),
            success: function (layero, index) {
                // 获取账户扩展字段元数据
                const extFields = getAccountExtFields();

                // 动态渲染扩展字段
                const extHtml = renderAccountExtFields(extFields, {});
                $('#accountExtFields').html(extHtml);

                // 渲染表单
                form.render();

                // 监听表单提交事件
                form.on('submit(accountSubmit)', function (data) {
                    // 处理ext字段
                    const ext = {};

                    // 根据字段元数据收集扩展字段值
                    extFields.forEach(field => {
                        const value = $('#' + field.key).val();
                        if (value !== undefined && value !== null && value !== '') {
                            ext[field.key] = value;
                        }
                    });

                    // 设置ext字段
                    data.field.ext = ext;

                    // 确保ext字段是对象而不是字符串
                    const formData = {...data.field};

                    $.ajax({
                        url: '/admin/account/save',
                        type: 'POST',
                        contentType: 'application/json',
                        data: JSON.stringify(formData),
                        success: function (res) {
                            if (res.success) {
                                layer.close(index);
                                layer.msg('保存成功');
                                accountTable.reload();
                            } else {
                                layer.msg('保存失败：' + res.message);
                            }
                        },
                        error: function(xhr, status, error) {
                            console.error('保存账户失败:', xhr.responseText);
                            layer.msg('保存账户失败: ' + error);
                        }
                    });
                    return false;
                });
            }
        });
    });

    // 刷新账户表格按钮事件
    $('#refreshAccountBtn').click(function () {
        accountTable.reload();
    });

    // 批量删除账户按钮事件
    $('#deleteAccountBtn').click(function () {
        const checkStatus = table.checkStatus('accountTable');
        const data = checkStatus.data;
        if (data.length === 0) {
            layer.msg('请选择要删除的账户');
            return;
        }
        layer.confirm('确定要删除选中的' + data.length + '个账户吗？', function (index) {
            let count = 0;
            let success = 0;
            data.forEach(function (item) {
                $.ajax({
                    url: '/admin/account/delete',
                    type: 'POST',
                    async: false,
                    data: {
                        id: item.id
                    },
                    success: function (res) {
                        count++;
                        if (res.success) {
                            success++;
                        }
                        if (count === data.length) {
                            layer.msg('成功删除' + success + '个账户');
                            accountTable.reload();
                        }
                    }
                });
            });
            layer.close(index);
        });
    });

    // 知识库表格
    const knowledgeTable = table.render({
        elem: '#knowledgeTable',
        url: '/admin/knowledge/list',
        cols: [[
            //{type: 'checkbox', fixed: 'left'},
            {field: 'code', title: '编码', width: 160},
            {field: 'title', title: '标题', width: 260},
            {field: 'type', title: '类型', width: 150, templet: function(d) {
                if (d.type === 'OPTION_STRATEGY' || d.type === 1) {
                    return '期权策略知识';
                } else if (d.type === 'RULE' || d.type === 2) {
                    return '规则知识';
                }
                return d.type;
            }},
            {field: 'description', title: '描述', width: 380},
            {field: 'status', title: '状态', width: 100, templet: function(d) {
                return d.status === 1 ? '<span style="color: green;">启用</span>' : '<span style="color: red;">禁用</span>';
            }},
            {field: 'createTime', title: '创建时间', width: 180, sort: true},
            {fixed: 'right', title: '操作', toolbar: '#knowledgeTableBar', width: 200}
        ]],
        page: true,
        limit: 10,
        limits: [10, 20, 50],
        request: {
            pageName: 'page',
            limitName: 'size'
        },
        response: {
            statusCode: 0
        },
        parseData: function(res) {
            return {
                "code": res.success ? 0 : 1,
                "msg": res.message,
                "count": res.data.total,
                "data": res.data.records || []
            };
        }
    });

    // 知识库表格工具条事件
    table.on('tool(knowledgeTable)', function (obj) {
        const data = obj.data;
        if (obj.event === 'edit') {
            // 编辑
            layer.open({
                type: 1,
                title: '编辑知识库',
                area: ['800px', '600px'],
                content: $('#knowledgeFormTpl').html(),
                success: function (layero, index) {
                    // 设置表单值
                    form.val('knowledgeForm', data);
                    form.render();

                    // 监听表单提交事件
                    form.on('submit(knowledgeSubmit)', function (formData) {
                        $.ajax({
                            url: '/admin/knowledge/save',
                            type: 'POST',
                            contentType: 'application/json',
                            data: JSON.stringify(formData.field),
                            success: function (res) {
                                if (res.success) {
                                    layer.close(index);
                                    layer.msg('保存成功');
                                    knowledgeTable.reload();
                                } else {
                                    layer.msg('保存失败：' + res.message);
                                }
                            }
                        });
                        return false;
                    });
                }
            });
        } else if (obj.event === 'delete') {
            // 删除
            layer.confirm('确定要删除该知识库吗？', function (index) {
                $.ajax({
                    url: '/admin/knowledge/delete',
                    type: 'POST',
                    data: {
                        id: data.id
                    },
                    success: function (res) {
                        if (res.success) {
                            layer.msg('删除成功');
                            knowledgeTable.reload();
                        } else {
                            layer.msg('删除失败：' + res.message);
                        }
                    }
                });
                layer.close(index);
            });
        } else if (obj.event === 'toggle') {
            // 切换状态
            const newStatus = data.status === 1 ? 0 : 1;
            const statusText = newStatus === 1 ? '启用' : '禁用';
            $.ajax({
                url: '/admin/knowledge/status',
                type: 'POST',
                data: {
                    id: data.id,
                    status: newStatus
                },
                success: function (res) {
                    if (res.success) {
                        layer.msg(statusText + '成功');
                        knowledgeTable.reload();
                    } else {
                        layer.msg(statusText + '失败：' + res.message);
                    }
                }
            });
        }
    });

    // 添加知识库按钮事件
    $('#addKnowledgeBtn').click(function () {
        layer.open({
            type: 1,
            title: '添加知识库',
            area: ['800px', '600px'],
            content: $('#knowledgeFormTpl').html(),
            success: function (layero, index) {
                form.render();

                // 监听表单提交事件
                form.on('submit(knowledgeSubmit)', function (data) {
                    $.ajax({
                        url: '/admin/knowledge/save',
                        type: 'POST',
                        contentType: 'application/json',
                        data: JSON.stringify(data.field),
                        success: function (res) {
                            if (res.success) {
                                layer.close(index);
                                layer.msg('保存成功');
                                knowledgeTable.reload();
                            } else {
                                layer.msg('保存失败：' + res.message);
                            }
                        }
                    });
                    return false;
                });
            }
        });
    });

    // 刷新知识库表格按钮事件
    $('#refreshKnowledgeBtn').click(function () {
        knowledgeTable.reload();
    });

    // 批量删除知识库按钮事件
    $('#deleteKnowledgeBtn').click(function () {
        const checkStatus = table.checkStatus('knowledgeTable');
        const data = checkStatus.data;
        if (data.length === 0) {
            layer.msg('请选择要删除的知识库');
            return;
        }
        layer.confirm('确定要删除选中的' + data.length + '个知识库吗？', function (index) {
            let count = 0;
            let success = 0;
            data.forEach(function (item) {
                $.ajax({
                    url: '/admin/knowledge/delete',
                    type: 'POST',
                    async: false,
                    data: {
                        id: item.id
                    },
                    success: function (res) {
                        count++;
                        if (res.success) {
                            success++;
                        }
                        if (count === data.length) {
                            layer.msg('成功删除' + success + '个知识库');
                            knowledgeTable.reload();
                        }
                    }
                });
            });
            layer.close(index);
        });
    });
});
