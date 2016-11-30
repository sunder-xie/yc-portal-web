<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
    <%@ include file="/inc/inc.jsp" %>
    <title><spring:message code="account.recharge"/></title>
</head>
<body>
<!--头部-->
<%@ include file="/inc/userTopMenu.jsp" %>
<!--二级主体-->
<!--外侧背景-->
<div class="cloud-container">
    <!--内侧内容区域-->
    <div class="cloud-wrapper">
        <!--左侧菜单-->
        <%@ include file="/inc/leftmenu.jsp"%>
        <!--右侧内容-->
        <!--右侧大块-->
        <div class="right-wrapper">
            <!--右侧第一块-->

            <!--右侧第二块-->
            <div class="right-list mt-0">
                <div class="right-list-title pb-10 pl-20">
                    <p>账户充值</p>
                </div>
                <!--充值-->
                <div class="recharge mt-30">
                 <!--充值成功-->
                    <div class="recharge-success">
                        <ul>
                            <li><img src="../images/rech-win.png" /></li>
                            <li class="word">充值成功</li>
                            <li class="list mt-50">
                                <p>请您牢记充值单号： <a href="#">5000020965</a>,您可以在“<a href="我的账户.html">我的账户</a>”中查看您的充值信息</p>
                                <p>若有任何疑问，欢迎致电咨询：400-119-8080</p>
                            </li>
                        </ul>
                    </div>


                </div>

            </div>
        </div>
    </div>

</div>
<script type="text/javascript" src="${_base}/resources/template/scripts/modular/jquery-1.11.1.min.js"></script>
<%--<script type="text/javascript" src="../scripts/modular/frame.js"></script--%>>
</body>
<%@ include file="/inc/incJs.jsp" %>
<script type="text/javascript">
    var current = "myaccount";
</script>
</html>
