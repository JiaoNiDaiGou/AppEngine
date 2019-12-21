<html>
<head>
  <title>感謝訂購 楊媽媽家常菜</title>
</head>
<body>
  <h2>感謝訂購 楊媽媽家常菜</h2>

  <p>親愛的${customerName}，您好。以下是您的訂購單。</p>
  <p>取貨時間：${deliveryTime}</p>
  <p>取貨地點：${deliveryAddress}</p>
  <p><font color="blue">记得带保温袋哦。</font></p>
  <br />
  <table>
    <tr>
        <th align="left">名稱</th>
        <th align="center">數量</th>
        <th align="center">單價</th>
    </tr>
    <#list orders as order>
        <tr>
            <td align="left">${order.name}</td>
            <td align="center">${order.quantity}</td>
            <td align="right">$${order.unitPrice}</td>
        </tr>
    </#list>
    <tr></tr>
    <tr>
        <td align="left" colspan="2">總價</td>
        <td align="right">$${totalPrice}</td>
    </tr>
  </table>

</body>
</html>
