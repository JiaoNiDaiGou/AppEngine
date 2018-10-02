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
        <td align="left">${overview[key]}</td>
      </tr>
      </#list>
    </table>
    <hr>

    <#if statusMaps?size != 0>

        <#list statusMaps as statusMap>

            <#if statusMap.dat?size != 0>
                <p>以下 ${statusMap.dat?size}单: ${statusMap.name}</p>

                       <table border=1>
                          <tr>
                            <th align="left">小熊单号</th>
                            <th align="left">下单时间</th>
                            <th align="left">上次更新时间</th>
                            <th align="left">收件人</th>
                          </tr>
                          <#list statusMap.dat as order>
                          <tr class="tr_head">
                            <td align="left">
                              <a href="http://rnbex.us/Member/OrderView.aspx?ID=${order.teddyOrderId}">${order.teddyFormattedId}</a>
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
                尚未发现有任何 ${statusMap.name}
            </#if>

        </#list>

    </#if>

  </body>
</html>
