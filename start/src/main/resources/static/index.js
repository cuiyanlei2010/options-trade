//JS

var $ = layui.$;
var element = layui.element;

var currentCode;
var currentMarket;

function render(){
    layui.use(['element', 'layer', 'util'], function(){
      var element = layui.element;
      var layer = layui.layer;
      var util = layui.util;
      var $ = layui.$;
      element.render('nav');
    });
}


function loadOptionsExpDate(code, market){
    currentCode = code;
    currentMarket = market;
    console.log('loadOptionsExpDate code:'+ code+' market:'+ market);
    $.ajax({
      url: "/options/strike/list",
      data: {
        code: code,
        market: market,
        time: new Date().getTime()
      },
      success: function( result ) {
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
      }
    });
}
// /options/chain/get?code=BABA&market=11&time=1733652854662&strikeTime=2024-12-13&strikeTimestamp=1734066000&optionExpiryDateDistance=5
function loadOptionsChain(strikeTime, strikeTimestamp, optionExpiryDateDistance){
    console.log('loadOptionsChain strikeTime:'+ strikeTime+' optionExpiryDateDistance:'+ optionExpiryDateDistance);
    document.getElementById("title").innerHTML = "loading...";
    $.ajax({
      url: "/options/chain/get",
      data: {
        code: currentCode,
        market: currentMarket,
        strikeTime: strikeTime,
        strikeTimestamp: strikeTimestamp,
        optionExpiryDateDistance: optionExpiryDateDistance,
        time: new Date().getTime()
      },
      success: function( result ) {
        document.getElementById("title").innerHTML=currentCode + '(' + result.securityQuote.lastDone + ') - ' + result.strikeTime + '(' + optionExpiryDateDistance + ')';

        var convertedData = result.optionList.map(item => {
            return {
                "strikePrice": item.call.optionExData.strikePrice,
                "putDelta": item.put.realtimeData?item.put.realtimeData.delta:0,
                "callDelta": item.call.realtimeData?item.call.realtimeData.delta:0,
                "putGamma": item.put.realtimeData?item.put.realtimeData.gamma:0,
                "callGamma": item.call.realtimeData?item.call.realtimeData.gamma:0,
                "putTheta": item.put.realtimeData?item.put.realtimeData.theta:0,
                "callTheta": item.call.realtimeData?item.call.realtimeData.theta:0,
                "putCurPrice": item.put.realtimeData?item.put.realtimeData.curPrice:0,
                "callCurPrice": item.call.realtimeData?item.call.realtimeData.curPrice:0,
                "putSellAnnualYield": item.put.strategyData?item.put.strategyData.sellAnnualYield + '%' : '-',
                "callSellAnnualYield": item.call.strategyData?item.call.strategyData.sellAnnualYield + '%' : '-',
            };
        });

        layui.use('table', function(){
          var table = layui.table;
          var inst = table.render({
            elem: '#result',
            cols: [[
              {field: 'callGamma', title: 'Gamma', width: 100},
              {field: 'callTheta', title: 'Theta', width: 100},
              {field: 'callDelta', title: 'Delta', width: 100},
              {field: 'callCurPrice', title: 'Price', width: 100},
              {field: 'callSellAnnualYield', title: '年化', width: 100},
              {field: 'strikePrice', title: '行权价', width: 100, sort: true},
              {field: 'putSellAnnualYield', title: '年化', width: 100},
              {field: 'putCurPrice', title: 'Price', width: 100},
              {field: 'putDelta', title: 'Delta', width: 100},
              {field: 'putTheta', title: 'Theta', width: 100},
              {field: 'putGamma', title: 'Gamma', width: 100}
            ]],
            data: convertedData,
            //skin: 'line',
            //even: true,
            page: false,
            limits: [100, 200, 500],
            limit: 100
          });
        });

      }
    });
}

function reloadData(){
    $.ajax({
      url: "/underlying/list",
      data: {
        owner: $("#owner").val(),
        time: new Date().getTime()
      },
      success: function( result ) {
        var output = document.getElementById("underlying");
        output.innerHTML = "";

        for(var i=0; i<result.length; i++) {
            var obj = result[i];
            output.innerHTML += '<li class="layui-nav-item" onclick="loadOptionsExpDate(\''+obj.code+'\',\''+obj.market+'\')"><a href="javascript:;">'+obj.code+'</a></li>'
        }
        render();
      }
    });
}
reloadData();



