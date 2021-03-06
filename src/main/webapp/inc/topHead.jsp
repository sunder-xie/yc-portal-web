<%@page import="java.util.Locale"%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<!--面包屑导航-->
<div class="placeorder-breadcrumb-big">
    <div class="placeorder-breadcrumb">
        <ul>
            <%--<li class="left"><i class="icon-volume-off"></i><A href="#">网站公告栏，通知网站各种事件</A></li>--%>
            <li class="right">
                <p>
                    <%--登录用户信息--%>
                    <c:set var="loginUser" value="${sessionScope.user_session_key}"/>
                    <c:choose>
                        <c:when test="${loginUser !=null}">
                        	<c:choose>
								<c:when test="${fn:length(loginUser.username)>8}">
									<a href="${_base}/p/security/index">${fn:substring(loginUser.username,0,8)}...</a>
								</c:when>
								<c:otherwise>
                                    <a href="${_base}/p/security/index">${loginUser.username}</a>
								</c:otherwise>
							</c:choose>
                            <%--退出--%>
                            <a href="${_base}/ssologout"><spring:message code="user.topMenu.exit"/></a>
                        </c:when>
                        <c:otherwise><a href="${_base}/p/index"><spring:message code="topMenue.Login"/></a>
                            <a href="${_base}/reg/toRegister" class="blue"><spring:message code="topMenue.Regist"/></a>
                        </c:otherwise>
                    </c:choose>
                </p>
                <%--我的订单--%>
                <p><a href="${_base}/p/customer/order/list/view"><spring:message code="topMenue.myOrder"/></a></p>
                <%--我是客户--%>
                <p><a href="${_base}/p/security/index"><spring:message code="topMenue.Customers"/></a></p>
                <%--我是服务商--%>
                <p><a href="${_base}/p/security/interpreterIndex"><spring:message code="topMenue.Suppliers"/></a></p>
                <%--手机版--%>
                <p class="none-border">
                    <a href="${_base}/findyee"><i class="icon iconfont">&#xe60b;</i><spring:message code="topMenue.Mobile"/></a></p>
                <p class="in-switch" id="in-switch">
                    <a href="javaScript:void(0);"><%= Locale.SIMPLIFIED_CHINESE.equals(response.getLocale())? "中文简体":"English"%><i class="icon-caret-down"></i></a>
                </p>
                <div class="in-switch-hover" style="display: none;">
                    <ul>
                        <li><a href="javaScript:void(0);" onclick="changeLang('zh_CN');return false;" style="display: block;width: 100%;height: 100%">中文简体</a></li>
                        <li class="border-none"><a href="javaScript:void(0);" onclick="changeLang('en_US');return false;" style="display: block;width: 100%;height: 100%">English</a></li>
                    </ul>
                </div>
                <%--<p class="none-border none-top">--%>
                    <%--<select id="langHeadSel" class="select select-topmini none-select ash-select"--%>
                            <%--onchange="changeLang(this);">--%>
                        <%--<option value="<%= Locale.SIMPLIFIED_CHINESE%>">简体中文</option>--%>
                        <%--<option value="<%= Locale.US%>"--%>
                                <%--<%= !Locale.SIMPLIFIED_CHINESE.equals(response.getLocale())?"selected":""%>--%>
                        <%-->ENGLISH</option>--%>
                    <%--</select>--%>
                    <%--<i class="icon-caret-down"></i>--%>
                <%--</p>--%>
            </li>
        </ul>
    </div>
</div>
