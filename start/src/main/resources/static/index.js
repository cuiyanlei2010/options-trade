//JS
var currentStrategyId;
var currentCode;
var currentMarket;
var currentOwnerData;

var assistantWindow;
var currentPrompt;
var currentLabel;
var currentChart;

function filterStrategyByCode(jsonArray, code) {
    return jsonArray.filter(item => item.code === code);
}
function filterStrategyById(jsonArray, strategyId) {
    return jsonArray.filter(item => item.strategyId === strategyId);
}
function strategySelect(code,strategyId){
    currentStrategyId = strategyId;
    localStorage.setItem(code+'_strategyId', currentStrategyId);
}
function strategyLoad(code){
    currentStrategyId = localStorage.getItem(code+'_strategyId');
    console.log('strategyLoad code:'+ code+' currentStrategyId:'+currentStrategyId);
}
function resetContent(title){
    document.getElementById("title").innerHTML = title;
    chartCanvas.style.display = 'none';
    currentLabel = "";
    currentPrompt = "";
}
function assistant(title){
    if(!currentPrompt){
        layer.msg('请选择策略和行权日期！');
        return;
    }
    localStorage.setItem("title", title);
    localStorage.setItem("prompt", currentPrompt);
    if(!assistantWindow || assistantWindow.closed){
        assistantWindow = window.open("assistant.html", "assistant");
    }else{
        assistantWindow.focus();
    }
}
var chartCanvas = document.getElementById('chartZone');
function showChart(label, data, type, options) {
    if (currentChart) {
        currentChart.destroy();
        chartCanvas.removeAttribute('width');
    }
    if (chartCanvas.style.display != 'none' && currentLabel == label) {
        chartCanvas.style.display = 'none';
    } else {
        chartCanvas.style.display = 'block';
        currentLabel = label;

        var labels;
        const datasets = [];
        
        // 图表配置选项
        const chartOptions = options || {};
        
        // 默认颜色配置
        const defaultColors = [
            { borderColor: 'rgba(255, 99, 132, 1)', backgroundColor: 'rgba(255, 99, 132, 0.2)' },
            { borderColor: 'rgba(54, 162, 235, 1)', backgroundColor: 'rgba(54, 162, 235, 0.2)' },
            { borderColor: 'rgba(75, 192, 192, 1)', backgroundColor: 'rgba(75, 192, 192, 0.5)' },
            { borderColor: 'rgba(255, 206, 86, 1)', backgroundColor: 'rgba(255, 206, 86, 0.2)' },
            { borderColor: 'rgba(153, 102, 255, 1)', backgroundColor: 'rgba(153, 102, 255, 0.2)' }
        ];
        
        if (Array.isArray(data)) {
            // 单图表情况
            const reversedData = data.reverse();
            labels = reversedData.map(item => item.date);
            datasets.push({
                label: label,
                data: reversedData.map(item => item.value),
                borderWidth: 1,
                ...defaultColors[0]
            });
        } else {
            // 多图表情况
            let colorIndex = 0;
            
            for (const [key, value] of Object.entries(data)) {
                const reversedData = value.reverse();
                if(!labels || labels.length < reversedData.length){
                    labels = reversedData.map(item => item.date);
                }
                
                // 基本配置
                let datasetConfig = {
                    label: key,
                    data: reversedData.map(item => item.value),
                    borderWidth: 1,
                    ...defaultColors[colorIndex % defaultColors.length]
                };
                
                // 应用特定系列的配置
                if (chartOptions.seriesConfig && chartOptions.seriesConfig[key]) {
                    Object.assign(datasetConfig, chartOptions.seriesConfig[key]);
                }
                
                datasets.push(datasetConfig);
                colorIndex++;
            }
        }

        // 对齐数据 找到最大长度
        const maxLength = Math.max(...datasets.map(dataset => dataset.data.length));
        // 填充数据
        datasets.forEach(dataset => {
            while (dataset.data.length < maxLength) {
                dataset.data.unshift(null);
            }
        });

        // 使用传入的图表类型，如果有混合类型的配置则忽略
        let finalChartType = type;
        if (datasets.some(dataset => dataset.type)) {
            finalChartType = 'line'; // 当有混合类型时，基础类型设为line
        }

        currentChart = new Chart(chartCanvas, {
            type: finalChartType,
            data: {
                labels: labels,
                datasets: datasets
            },
            options: {
                responsive: false,
                scales: {
                    y: {
                        // beginAtZero: true
                    }
                },
                onClick: function (event, elements) {
                    chartCanvas.style.display = 'none';
                }
            }
        });
    }
}
function loadOptionsExpDate(code, market){
    currentCode = code;
    currentMarket = market;
    strategyLoad(code);
    console.log('loadOptionsExpDate code:'+ code+' market:'+ market+' currentStrategyId:'+currentStrategyId);
    $.ajax({
      url: "/options/strike/list",
      data: {
        code: code,
        market: market,
        time: new Date().getTime()
      },
      success: function( response ) {
        resetContent("请选择期权到期日");
        var result = response.data;
        var output = document.getElementById("strike-list");
        output.innerHTML = "";
        var moreHtml = ""
        for(var i=0; i<result.length; i++) {
            var obj = result[i];
            if (i < 8){
                output.innerHTML += '<li class="layui-nav-item layui-hide-xs" onclick="loadOptionsChain(\''+obj.strikeTime+'\',\''+obj.strikeTimestamp+'\',\''+obj.optionExpiryDateDistance+'\')"><a href="javascript:;">'+obj.strikeTime+'('+obj.optionExpiryDateDistance+')</a></li>'
            }else{
                moreHtml += '<dd onclick="loadOptionsChain(\''+obj.strikeTime+'\',\''+obj.strikeTimestamp+'\',\''+obj.optionExpiryDateDistance+'\')"><a href="javascript:;">'+obj.strikeTime+'('+obj.optionExpiryDateDistance+')</a></dd>'
            }
        }
        output.innerHTML += '<li class="layui-nav-item"><a href="javascript:;">More</a><dl class="layui-nav-child">' + moreHtml + '</dl></li>';
        render();

        // 渲染当前证券策略列表
        var currentStrategyList = filterStrategyByCode(currentOwnerData.strategyList,currentCode);
        laytpl(currentStrategy.innerHTML).render({list:currentStrategyList,strategyId:currentStrategyId}, function(html){
          document.getElementById('strategyIdZone').innerHTML = html;
          var form = layui.form;
          form.on('select(strategyId)', function(elem){
            strategySelect(code,elem.value);
          });
          form.render();
        });
      }
    });
}
// /options/chain/get?code=BABA&market=11&time=1733652854662&strikeTime=2024-12-13&strikeTimestamp=1734066000&optionExpiryDateDistance=5
function loadOptionsChain(strikeTime, strikeTimestamp, optionExpiryDateDistance){
    console.log('loadOptionsChain strikeTime:'+ strikeTime+' optionExpiryDateDistance:'+ optionExpiryDateDistance+' currentStrategyId:'+currentStrategyId);
    resetContent("loading...");
    $.ajax({
      url: "/options/chain/get",
      data: {
        code: currentCode,
        market: currentMarket,
        strikeTime: strikeTime,
        strikeTimestamp: strikeTimestamp,
        optionExpiryDateDistance: optionExpiryDateDistance,
        strategyId: currentStrategyId,
        time: new Date().getTime()
      },
      success: function( response ) {
        var result = response.data;
        currentPrompt = result.prompt;
        result.currentCode=currentCode;
        result.optionExpiryDateDistance=optionExpiryDateDistance
        var view = document.getElementById('title');
        laytpl(commonInfo.innerHTML).render(result, function(html){
          view.innerHTML = html;
        });

        var convertedData = result.optionList.map(item => {
            return {
                "LAY_CHECKED": item.call&&item.call.strategyData?item.call.strategyData.recommend:(item.put&&item.put.strategyData?item.put.strategyData.recommend:false),
                "data": item.call?item.call:item.put,
                "callObj": item.call,
                "putObj": item.put,
                "group": item.call?item.call.basic.name.match(/^([^ ]+)/)[1]:item.put.basic.name.match(/^([^ ]+)/)[1],
                "strikePrice": item.call?item.call.optionExData.strikePrice:item.put.optionExData.strikePrice,
                "call": item.call?JSON.stringify(item.call):null,
                "put": item.put?JSON.stringify(item.put):null,
                "putCurPrice": item.put && item.put.realtimeData?item.put.realtimeData.curPrice:'-',
                "callCurPrice": item.call && item.call.realtimeData?item.call.realtimeData.curPrice:'-',
                "putSellAnnualYield": item.put && item.put.strategyData?item.put.strategyData.sellAnnualYield + '%' : '-',
                "callSellAnnualYield": item.call && item.call.strategyData?item.call.strategyData.sellAnnualYield + '%' : '-',
                "putSellRecommend": item.put && item.put.strategyData?item.put.strategyData.recommend : false,
                "callSellRecommend": item.call && item.call.strategyData?item.call.strategyData.recommend : false,
            };
        });

        layui.use('table', function(){
          var table = layui.table;
          var inst = table.render({
            elem: '#result',
            cols: [[
              {field: 'data', title: '推荐-涨跌', width: 100, align: 'left', templet: '#TPL-recommendInfo'},
              {title: '交易参考信息', width: 260, rowspan: 3, templet: '#id-table-call-info'},
              {field: 'callCurPrice', title: '价格', width: 85},
              {field: 'call', title: '卖', width: 20, templet: '{{#  if(d.call){ }}<div><a title="{{= d.callObj.basic.name }}" class="layui-btn layui-btn-primary layui-btn-xs" onclick="sell({{= d.call }})" lay-event="sell">卖</a></div>{{#  } }}'},
              {field: 'callSellAnnualYield', title: '年化', width: 85},
              {field: 'strikePrice', title: '行权价', width: 90, sort: true},
              {field: 'putSellAnnualYield', title: '年化', width: 85},
              {field: 'put', title: '卖', width: 20, templet: '{{#  if(d.put){ }}<div><a title="{{= d.putObj.basic.name }}" class="layui-btn layui-btn-primary layui-btn-xs" onclick="sell({{= d.put }})" lay-event="sell">卖</a></div>{{#  } }}'},
              {field: 'putCurPrice', title: '价格', width: 85},
              {title: '交易参考信息', width: 260, templet: '#id-table-put-info'},
            ]],
            data: convertedData,
            toolbar: true,
            lineStyle: 'height: 100%;',
            defaultToolbar: [
              'filter', // 列筛选
              'exports', // 导出
              'print' // 打印
            ],
            //skin: 'line',
            //even: true,
            height: 'full-320',
            lineStyle: 'height: 100%;',
            initSort: {
              field: 'strikePrice',
              type: 'asc'
            },
            page: false,
            limits: [100, 200, 500],
            limit: 100
          });
        });

      }
    });
}

//owner: $("#owner").val(),
function trade(side, options, orderBook){
    if(!currentStrategyId){
        layer.msg('请先选择策略！');
        return;
    }
    var selectStrategyArr = filterStrategyById(currentOwnerData.strategyList, currentStrategyId);
    if(!selectStrategyArr || selectStrategyArr.length != 1){
        layer.msg('无法匹配策略！');
        return;
    }

    layer.confirm('确认交易策略名称：'+selectStrategyArr[0].strategyName, {
            btn: ['确定', '取消'] //按钮
          }, function(){
            // 确认
            layer.prompt({title: '请输入卖出份数', value: 1}, function(value, index, elem){
                if(value === ''){
                    return elem.focus();
                }
                var quantity = util.escape(value);
                layer.close(index);
                layer.prompt({title: '请输入卖出价格（ask:'+orderBook.askList+' bid:'+orderBook.bidList+'）', value: options.realtimeData.curPrice}, function(value, index, elem){
                    if(value === ''){
                        return elem.focus();
                    }
                    // 下单
                    var price = util.escape(value);
                    $.ajax({
                      url: "/trade/submit",
                      method: 'POST',
                      data: {
                        password: $("#totp").val(),
                        side: side,
                        strategyId: currentStrategyId,
                        quantity: quantity,
                        price: price,
                        options: JSON.stringify(options),
                      },
                      success: function( response ) {
                        layer.msg(response.success ? '操作成功' : response.message);
                        layer.close(index);
                      }
                    });
                });
            });
       }, function(){
                // 取消
       });
}

function sell(options){
    $.ajax({
         url: "/options/orderbook/get",
         method: 'GET',
         data: {
           code: options.basic.security.code,
           market: options.basic.security.market,
           time: new Date().getTime()
         },
         success: function( response ) {
            trade(2, options, response.data);
         }
       });
}

function reloadData(){
    $.ajax({
      url: "/owner/get",
      data: {
        time: new Date().getTime()
      },
      success: function( response ) {
        currentOwnerData = response.data;
        $("#owner").val(currentOwnerData.owner);

        var output = document.getElementById("security");
        output.innerHTML = "";
        for(var i=0; i<currentOwnerData.securityList.length; i++) {
            var obj = currentOwnerData.securityList[i];
            var unexercisedCount = obj.unexercisedOrders ? obj.unexercisedOrders.length : 0;
            var redDot = unexercisedCount > 0 ? 
                '<span class="layui-badge-dot layui-bg-red unexercised-dot" style="margin-left: 5px;" data-orders=\'' + JSON.stringify(obj.unexercisedOrders) + '\'></span>' : '';
            output.innerHTML += '<dd onclick="loadOptionsExpDate(\'' + obj.code + '\',\'' + obj.market + '\')"><a href="javascript:;">' + obj.name + obj.code + redDot + '</a></dd>';
        }
        render();
        
        // 添加鼠标悬停事件
        $('.unexercised-dot').each(function() {
            var $dot = $(this);
            var orders = JSON.parse($dot.attr('data-orders'));
            
            // 处理订单数据，解析ext字段
            orders = orders.map(order => {
                try {
                    if (order.ext) {
                        let sourceOptions = null;
                        if (typeof order.ext.sourceOptions === 'string') {
                            try {
                                sourceOptions = JSON.parse(order.ext.sourceOptions);
                            } catch (e) {
                                console.error('解析sourceOptions字符串失败:', e);
                            }
                        } else if (typeof order.ext.sourceOptions === 'object') {
                            sourceOptions = order.ext.sourceOptions;
                        }
                        
                        // 计算剩余天数
                        const strikeDate = new Date(order.strikeTime);
                        const today = new Date();
                        const curDTE = Math.ceil((strikeDate - today) / (1000 * 60 * 60 * 24));
                        
                        return {
                            ...order,
                            ext: {
                                ...order.ext,
                                curDTE: curDTE
                            }
                        };
                    }
                    return order;
                } catch (e) {
                    console.error('处理订单数据失败:', e);
                    return order;
                }
            });
            
            layui.use(['layer', 'laytpl'], function(){
                var layer = layui.layer;
                var laytpl = layui.laytpl;
                
                $dot.hover(function(e){
                    // 使用模板渲染未行权期权信息
                    laytpl(unexercisedOrders.innerHTML).render(orders, function(html){
                        layer.open({
                            type: 1,
                            title: '未行权期权信息',
                            shade: 0,
                            offset: [e.pageY + 10, e.pageX + 10],
                            area: ['400px'],
                            skin: 'layui-layer-molv',
                            content: html,
                            success: function(layero, index){
                                // 当鼠标离开弹出层和红点时关闭
                                $(layero).add($dot).one('mouseleave', function(){
                                    setTimeout(function(){
                                        if(!$(layero).is(':hover') && !$dot.is(':hover')){
                                            layer.close(index);
                                        }
                                    }, 100);
                                });
                            }
                        });
                    });
                }, function(){
                    // 鼠标离开红点时，不立即关闭，让用户有机会移动到弹出层上
                    setTimeout(function(){
                        if(!$('.layui-layer').is(':hover')){
                            layer.closeAll();
                        }
                    }, 100);
                });
            });
        });
      }
    });
}

reloadData();

let clientId = "realtime_data_" + new Date().getTime();
let source = null;
if (window.EventSource) {
    // 连接的建立
    source = new EventSource("/connect?requestId=" + clientId);

    source.addEventListener('open', function (e) {
        console.log("sse open.")
    }, false);

    source.addEventListener("message", function (e) {
        content = JSON.parse(e.data);
        if(content.data.nyc_time){
            document.getElementById("nyc_time").innerHTML = content.data.nyc_time;
        }
        if(content.data.stock_price){
            var currentData = content.data.stock_price;
            var priceEle = document.getElementById("stock_"+currentData.security.market+'_'+currentData.security.code);
            if(priceEle){
                priceEle.innerHTML = currentData.lastDone;
            }
        }
    });

    source.addEventListener('error', function (e) {
        if (e.readyState === EventSource.CLOSED) {
            console.log("sse close.")
        } else {
            console.log(e);
        }
    }, false);

}

// 监听窗口关闭事件，主动去关闭连接
window.onbeforeunload = function () {
    closeSse();
};

// 关闭Sse连接
function closeSse() {
    source.close();
    const httpRequest = new XMLHttpRequest();
    httpRequest.open('GET', '/close?requestId=' + clientId, true);
    httpRequest.send();
    console.log("close");
}
