<html>
  <head>
    <title>小熊 发货单状态更新</title>
    <style>
    table {
        font-family: "Trebuchet MS", Arial, Helvetica, sans-serif;
        border-collapse: collapse;
        width: 100%;
    }

    td, th {
        border: 1px solid #ddd;
        padding: 8px;
    }

    tr:nth-child(even){background-color: #f2f2f2;}

    tr.tr_head {
        background-color: #f2f2f2;
        font-weight: bold;
    }

    td.td_notification {
        background-color: #e2f442;
        font-weight: bold;
    }

    th {
        padding-top: 12px;
        padding-bottom: 12px;
        text-align: left;
        background-color: #4CAF50;
        color: white;
    }
    </style>
  </head>
  <body>
    <h2>小熊 发货单状态更新</h2>
    <p>查询时间:${queryTime}</p>
    <br/>
    <table border=1>
      <tr>
        <th align="left">发货单状态</th>
        <th align="left">数量</th>
      </tr>
      <#list overview?keys?sort as key>
      <tr>
        <td align="left">${key}</td>
        <td align="left" <#if overview[key]?contains("新增数")>class="td_notification"</#if>>${overview[key]}</td>
      </tr>
      </#list>
    </table>
    <hr>

    <#if newlyCreated?size != 0>
    <p>以下${newlyCreated?size}新创建发货单 等待小熊处理</p>
    <table border=1>
      <tr>
        <th align="left">小熊单号</th>
        <th align="left">下单时间</th>
        <th align="left">上次更新时间</th>
        <th align="left">收件人</th>
      </tr>
      <#list newlyCreated as order>
      <tr>
        <td align="left">
          <a href="http://rnbex.us/Member/OrderView.aspx?ID=${order.id}">${order.formattedId}</a>
        </td>
        <td align="left">${order.creationTime}</td>
        <td align="left">${order.lastUpdateTime}</td>
        <td align="left">${order.receiverName}<br/>${order.receiverPhone}</td>
      </tr>
      <tr>
        <td colspan=4 align="left">商品信息:<br/>${order.productSummary}<br/><b>${order.price}</td>
      </tr>
      </#list>
    </table>
    <#else>
    尚未发现有任何新创建发货单
    </#if>

    <hr>

    <#if newlyPending?size != 0>
    <p>以下${newlyPending?size}发货单 小熊开始处理 国际运输 至 国内清关</p>
        <table border=1>
          <tr>
            <th align="left">小熊单号</th>
            <th align="left">下单时间</th>
            <th align="left">上次更新时间</th>
            <th align="left">收件人</th>
          </tr>
          <#list newlyPending as order>
          <tr class="tr_head">
            <td align="left">
              <a href="http://rnbex.us/Member/OrderView.aspx?ID=${order.id}">${order.formattedId}</a>
            </td>
            <td align="left">${order.creationTime}</td>
            <td align="left">${order.lastUpdateTime}</td>
            <td align="left">${order.receiverName}<br/>${order.receiverPhone}</td>
          </tr>
          <tr>
            <td colspan=4 align="left">商品信息:<br/>${order.productSummary}</td>
          </tr>
          <tr>
            <td colspan=4 align="left">快递状态:${order.shippingCarrier}<br/>快递单号:${order.trackingNumber}</td>
          </tr>
          <tr>
            <td colspan=4 align="left">${order.latestStatus}</td>
          </tr>
          </#list>
        </table>
    <#else>
    尚未发现有任何发货单处于国际运输至国内清关阶段
    </#if>


    <hr>
    <#if newlyTrackingNumberAssigned?size != 0>
    <p>以下${newlyTrackingNumberAssigned?size}发货单 有了新的快递追踪单号(尚无配送信息)</p>
        <table border=1>
          <tr>
            <th align="left">小熊单号</th>
            <th align="left">下单时间</th>
            <th align="left">上次更新时间</th>
            <th align="left">收件人</th>
          </tr>
          <#list newlyTrackingNumberAssigned as order>
          <tr class="tr_head">
            <td align="left">
              <a href="http://rnbex.us/Member/OrderView.aspx?ID=${order.id}">${order.formattedId}</a>
            </td>
            <td align="left">${order.creationTime}</td>
            <td align="left">${order.lastUpdateTime}</td>
            <td align="left">${order.receiverName}<br/>${order.receiverPhone}</td>
          </tr>
          <#if order.smsCustomerNotificationSend == true>
            <tr>
                <td class="td_notification" colspan=4 align="left" >已发送短信通知</td>
            </tr>
          </#if>
          <tr>
            <td colspan=4 align="left">商品信息:<br/>${order.productSummary}</td>
          </tr>
          <tr>
            <td colspan=4 align="left">快递状态:${order.shippingCarrier}<br/>快递单号:${order.trackingNumber}</td>
          </tr>
          <tr>
            <td colspan=4 align="left">${order.latestStatus}</td>
          </tr>
          </#list>
        </table>
    <#else>
    尚未发现有任何发货单有了新的快递追踪单号
    </#if>
    <hr>
    <#if newlyPostmanAssigned?size != 0>
    <p>以下${newlyPostmanAssigned?size}发货单 有了快递员信息：</p>
        <table border=1>
          <tr>
            <th align="left">小熊单号</th>
            <th align="left">下单时间</th>
            <th align="left">上次更新时间</th>
            <th align="left">收件人</th>
          </tr>
          <#list newlyPostmanAssigned as order>
          <tr class="tr_head">
            <td align="left">
              <a href="http://rnbex.us/Member/OrderView.aspx?ID=${order.id}">${order.formattedId}</a>
            </td>
            <td align="left">${order.creationTime}</td>
            <td align="left">${order.lastUpdateTime}</td>
            <td align="left">${order.receiverName}<br/>${order.receiverPhone}</td>
          </tr>
          <#if order.smsCustomerNotificationSend == true>
            <tr>
                <td class="td_notification" colspan=4 align="left">已发送短信通知</td>
            </tr>
          </#if>
          <tr>
            <td colspan=4 align="left">快递员:${order.postmanInfo}</td>
          </tr>
          <tr>
            <td colspan=4 align="left">商品信息:<br/>${order.productSummary}</td>
          </tr>
          <tr>
            <td colspan=4 align="left">快递状态:${order.shippingCarrier}<br/>快递单号:${order.trackingNumber}</td>
          </tr>
          <tr>
            <td colspan=4 align="left">${order.latestStatus}</td>
          </tr>
          </#list>
        </table>
    <#else>
    <p>尚未发现有任何发货单有了新的配送信息</p>
    </#if>
    <hr>
    <#if noUpdatesOverDays?size != 0>
    <p>以下${noUpdatesOverDays?size}发货单 15天以上没有国内快递单号：</p>
        <table border=1>
          <tr>
            <th align="left">小熊单号</th>
            <th align="left">下单时间</th>
            <th align="left">上次更新时间</th>
            <th align="left">收件人</th>
          </tr>
          <#list noUpdatesOverDays as order>
          <tr class="tr_head">
            <td align="left">
              <a href="http://rnbex.us/Member/OrderView.aspx?ID=${order.id}">${order.formattedId}</a>
            </td>
            <td align="left">${order.creationTime}</td>
            <td align="left">${order.lastUpdateTime}</td>
            <td align="left">${order.receiverName}<br/>${order.receiverPhone}</td>
          </tr>
          <tr>
            <td colspan=4 align="left">商品信息:<br/>${order.productSummary}</td>
          </tr>
          <tr>
            <td colspan=4 align="left">快递状态:${order.shippingCarrier}<br/>快递单号:${order.trackingNumber}</td>
          </tr>
          <tr>
            <td colspan=4 align="left">${order.latestStatus}</td>
          </tr>
          </#list>
        </table>
    <#else>
    <p>尚未发现有任何发货单超过15天仍没有快递追踪单号</p>
    </#if>

    <#if last30DaysSenderReports?size != 0>
    <p>以下${last30DaysSenderReports?size}最近30天小熊发货排名：</p>
            <table border=1>
              <#list last30DaysSenderReports as report>
              <tr>
                <td>${report}</td>
              </tr>
              </#list>
            </table>
    </#if>
  </body>
</html>
