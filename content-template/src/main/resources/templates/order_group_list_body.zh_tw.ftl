<html>
<head>
    <title>楊媽媽家常菜 ${orderGroupTag} 綜覽</title>
    <style type="text/css">

table.redTable {
  border: 2px solid #A40808;
  background-color: #EEE7DB;
  width: 100%;
  text-align: center;
  border-collapse: collapse;
  margin: 20px;
}
table.redTable td, table.redTable th {
  border: 1px solid #AAAAAA;
  padding: 3px 2px;
}
table.redTable tbody td {
  font-size: 13px;
}
table.redTable tr:nth-child(even) {
  background: #F5C8BF;
}
table.redTable thead {
  background: #A40808;
}
table.redTable thead th {
  font-size: 19px;
  font-weight: bold;
  color: #FFFFFF;
  text-align: center;
  border-left: 2px solid #A40808;
}
table.redTable thead th:first-child {
  border-left: none;
}

table.redTable tfoot {
  font-size: 13px;
  font-weight: bold;
  color: #FFFFFF;
  background: #A40808;
}
table.redTable tfoot td {
  font-size: 13px;
}
table.redTable tfoot .links {
  text-align: right;
}
table.redTable tfoot .links a{
  display: inline-block;
  background: #FFFFFF;
  color: #A40808;
  padding: 2px 8px;
  border-radius: 5px;
}

    </style>
</head>
<body>
    <#list orderGroupByDelivery as orderGroup>
        <p>時間: ${orderGroup.rawTime}</p>
        <p>地點: ${orderGroup.rawAddress}</p>

        <#list orderGroup.orders as orderForOnePerson>
            <table class="redTable">
                <tr>
                    <td align="center" colspan="3">${orderForOnePerson.customerName} ${orderForOnePerson.customerPhone}</td>
                <tr>
                <#list orderForOnePerson.orders as order>
                    <tr>
                        <td align="left">${order.name}</td>
                        <td align="center">${order.quantity}</td>
                        <td align="right">$${order.unitPrice}</td>
                    </tr>
                </#list>
                <tr>
                    <td align="left" colspan="2">總價</td>
                    <td align="right">$${orderForOnePerson.totalPrice}</td>
                </tr>
            </table>
        </#list>

        <hr />
    </#list>
</body>
</html>
