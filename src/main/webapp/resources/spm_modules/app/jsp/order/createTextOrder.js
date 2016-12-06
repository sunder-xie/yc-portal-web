define('app/jsp/order/createTextOrder', function (require, exports, module) {
    'use strict';
    var $=require('jquery'),
	    Widget = require('arale-widget/1.2.0/widget'),
		Dialog = require("optDialog/src/dialog"),
	    AjaxController = require('opt-ajax/1.0.0/index');
    require("jsviews/jsrender.min");

    require("jquery-validation/1.15.1/jquery.validate");
	require("app/util/aiopt-validate-ext");
	require('jquery-i18n/1.2.2/jquery.i18n.properties.min');
	require('webuploader/webuploader');
    var CountWordsUtil = require("app/util/countWords");
    
    //实例化AJAX控制处理对象
    var ajaxController = new AjaxController();
    var uploader = null;
    var textOrderAddPager = Widget.extend({
    	//属性，使用时由类的构造函数传入
    	attrs: {
    		clickId:""
    	},
    	
    	//事件代理
    	events: {
			"click #recharge-popo":"_addTextOrderTemp",
			"click #urgentOrder":"_transPrice",
			"click .dropdown":"_transPrice",
			"click #saveContact":"_saveContact",
			"click #editContact":"_editContactDiv",
			"click #fy-btn": "_uploadFile",
			"click #fy-btn1": "_inputText",
			"click #clear-btn": "_clearText",
			"keyup #translateContent": "_clearControl",
			"change #selectFormatConv": "_formatControl"
           	},
            
    	//重写父类
    	setup: function () {
    		textOrderAddPager.superclass.setup.call(this);
			//初始化国际化
			$.i18n.properties({//加载资浏览器语言对应的资源文件
				name: ["orderInfo"], //资源文件名称，可以是数组
				path: _i18n_res, //资源文件路径
				mode: 'both',
				language: currentLan
			});

			var formValidator=this._initValidate();
			$(":input").bind("focusout",function(){
				formValidator.element(this);
			});

			this._transGrade();
			this._transPrice();
			this._initPage();
    	},

        _initValidate: function () {
            var _this = this;
            var formValidator = $("#textOrderForm").validate({
                onkeyup: false,
                focusInvalid: true,
                errorPlacement: function (error, element) {
                    if (element.is(":checkbox")) {
                        error.appendTo(element.parent().parent().parent());
                    } else {
                        error.insertAfter(element);
                    }

                },
                highlight: function (element, errorClass) {

                },
                unhighlight: function (element, errorClass) {

                },
                errorClass: "x-label",
                showErrors: function (errorMap, errorList) {
                    this.defaultShowErrors();
                    $('ul li p label').each(function (index, element) {
                        if (index > 0)
                            element.remove();
                    });
                    //$('ul li p label').remove();//删除所有隐藏的li p label标签
                },
                rules: {
                    translateContent: {
                        required: true,
                        maxlength: 2000,
                        // remote: {                                          //验证检查语言
                        //     type: "POST",
                        //     url: _base + "/translateLan",
                        //     data: {
                        //         text: function () {
                        //             return $("#translateContent").val();
                        //         }
                        //     },
                        //     dataType: 'json',
                        //     dataFilter: function (data) {//判断控制器返回的内容
                        //         data = jQuery.parseJSON(data);
                        //
                        //         var sourlan = 'en';
                        //         $("#selectDuad").find('option').each(function () {
                        //             var val = $(this).val();
                        //             if (val == $(".dropdown .selected").attr('value')) {
                        //                 var selected = $(this);
                        //                 sourlan = selected.attr("sourceCode");
                        //                 return false;
                        //             }
                        //         });
                        //
                        //         if (sourlan == data.data) {
                        //             return true;
                        //         } else {
                        //             return false;
                        //         }
                        //     }
                        // }
                    },
                    isAgree: {
                        required: true
                    }
                },
                messages: {
                    translateContent: {
                        required: $.i18n.prop('order.place.error.translation'), //"请输入翻译内容",
                        maxlength: $.i18n.prop('order.place.error.Maximum'),//"最大长度不能超过{0}",
                       // remote:  $.i18n.prop('order.place.error.contentConsis')//"您输入的内容和源语言不一致"
                    },
                    isAgree: {
                        required: $.i18n.prop('order.place.error.agree')//"请阅读并同意翻译协议",
                    }
                }
            });

            return formValidator;
        },


    	//提交文本订单
		_addTextOrderTemp:function(){
			var _this= this;
        	var formValidator=_this._initValidate();
			formValidator.form();
			if(!$("#textOrderForm").valid()){
				//alert('验证不通过！！！！！');
				return formValidator.focusInvalid();
			}

			//文档类型 判断是否上传文件
			if(!_this._isTextTransType() && $("#fileList ul").length < 1) {
				_this._showWarn($.i18n.prop('order.upload.error.nofile'));
				return;
			}

			//计算字数
			var totalWords = CountWordsUtil.count($("#translateContent").val());

			var baseInfo = {};
			var productInfo = {};
			var orderSummary = {};
			orderSummary.duadName = $.trim($("#selectDuad").find("option:selected").text());
			orderSummary.purposeName = $.trim($("#selectPurpose").find("option:selected").text());
			orderSummary.domainName = $.trim($("#selectDomain").find("option:selected").text());
			orderSummary.translevel = $.trim($("#transGrade ul.current").find("p").eq(0).html());
			//区分国内外订单
			if (currentLan.indexOf("zh") >= 0) {
				baseInfo.orderType = "0";
				baseInfo.flag = "0";
				baseInfo.chlId = "0";
			}else {
				baseInfo.orderType = "1";
				baseInfo.flag = "1";//业务标识 0:国内业务 1：国际业务 ??
				baseInfo.chlId = "1";
			}

			baseInfo.orderType = "1"; //??
			baseInfo.busiType = 1;
			if(!_this._isTextTransType()) { //文档类型
				baseInfo.translateType = "1"

				var fileInfoList = [];
				//判断是文档翻译
				$("li[fileid]").each(function(){
					var tempObj = {};
					tempObj.fileName = $(this).text();
					tempObj.fileSaveId = $(this).attr("fileid");
					tempObj.fileSie = $(this).attr("size");
					fileInfoList.push(tempObj);
				});
				productInfo.fileInfoList = fileInfoList;

				baseInfo.subFlag = "1";
				//productInfo.needTranslateInfo = JSON.stringify(fileInfoList);
				productInfo.translateInfo = "";
				baseInfo.translateName = $("#fileList").find('li:first').text().substring(0,15);
			} else {
				baseInfo.translateType = "0"; //0：快速翻译 1：文档翻译
				baseInfo.subFlag = "0"; // "0：系统自动报价 1：人工报价"
				productInfo.needTranslateInfo = $("#translateContent").val();
				productInfo.translateInfo = "";
				baseInfo.translateName = $("#translateContent").val().substring(0,15);
			}
			baseInfo.orderLevel = "1";
			baseInfo.userType = "10"; //"10：个人 11：企业 12：代理人 "??
			baseInfo.remark = $("#remark").val(); //备注 给译员留言
			baseInfo.orderDesc=$("#inputFormatConv").val();//格式转换
			//baseInfo.corporaId
			//baseInfo.accountId

			var today = new Date();
			if(today.stdTimezoneOffset()/60 > 0)
				baseInfo.timeZone = 'GMT-'+Math.abs(today.stdTimezoneOffset()/60);
			else
				baseInfo.timeZone = 'GMT+'+Math.abs(today.stdTimezoneOffset()/60);

			productInfo.translateSum = totalWords;
			productInfo.useCode = $("#selectPurpose").val();  //用途
			productInfo.fieldCode = $("#selectDomain").val();  //领域

			if($("#selectAddedSer").val() == 1)
				productInfo.isSetType = "Y"; //是否排版
			else
				productInfo.isSetType = "N";

			if ( $("#urgentOrder").is(':checked') )
				productInfo.isUrgent = "Y";
			else
				productInfo.isUrgent = "N";

			var duadList =[];
			var tempLanPairObj = {};
			tempLanPairObj.languagePairId = $(".dropdown .selected").attr('value');
			$("#selectDuad").find('option').each(function() {
				var val = $(this).val();
				if (val ==  $(".dropdown .selected").attr('value')) {
					var selected = $(this);

					tempLanPairObj.languagePairName = selected.attr('sourceCn') + "→" + selected.attr('targertCn');
					tempLanPairObj.languageNameEn = selected.attr('sourceEn') + "→" + selected.attr('targertEn');
					return false;
				}
			});

			duadList.push(tempLanPairObj);
			productInfo.languagePairInfoList = duadList;

			var translateLevelInfoList=[];
			var tempTranlevObj={};
			tempTranlevObj.translateLevel = $("#transGrade ul.current").first().attr('name');
			translateLevelInfoList.push(tempTranlevObj);

			productInfo.translateLevelInfoList = translateLevelInfoList;

            //创建订单
			ajaxController.ajax({
				type: "post",
                processing: true,
				url: _base + "/order/add",
				data: {
					baseInfo: JSON.stringify(baseInfo),
					productInfo: JSON.stringify(productInfo),
					orderSummary: JSON.stringify(orderSummary),
					fileInfoList: JSON.stringify(fileInfoList)
				},
				success: function (data) {
                    if ("1" === data.statusCode) {
						window.location.href =  _base + "/p/order/contact?skip="+data.data;
					}
				}
			});
		},

		//判断是否是文档翻译类型
		_isTextTransType:function () {
			if ($("#fy2").css("display") == "none")
				return true
			else
				return false;
		},

		//翻译等级改变，翻译速度改变
		_transGrade:function() {
			var _this = this;
			$("#transGrade ul").each(function () {
				$(this).click(function () {
					
				
					$(this).children('label').remove();
					$(this).addClass("current");
					$(this).append('<label></label>');
					
					$($(this).siblings()).removeClass("current");
					$($(this).siblings()).children('label').remove();
					
					_this._getSpeed();
				});
			}) 
		},
		
		//初始化页面后做的操作
		_initPage:function() {
			//页面初始化，从session取订单信息
			//改变上传div高度
			// $("#selectFile").children("div:last").css("height", '70px');

			//根据翻译类型，显示不同
			if ($("#transType").val() == "1") {
				//文档
				$("#fy1").hide();
				$("#fy2").show();
				$("#selectAddedSer").attr("disabled",false);
				$("#selectFormatConv").attr("disabled",false);

				//格式转换
				if ($("#format").val() != '') {
					$("#selectFormatConv").val("1");
					$("#inputFormatConv").show();
					$("#inputFormatConv").val($("#format").val());
				}
			} else {
				//文字
				$("#fy2").hide();
				$("#selectFormatConv").val("2");
				$("#selectAddedSer").attr("disabled",true);
				$("#selectFormatConv").attr("disabled",true);
			}


			//session 语言对
			if ($("#duadName").val() != '') {
				$(".dropdown .selected").val($("#duadName").val());
			}

			//翻译级别
			if ($("#transLv").val() != '') {
				$("#transGrade ul").each(function () {
					if ($("#transLv").val() == $(this).attr("name")) {
						$(this).children('label').remove();
						$(this).addClass("current");
						$(this).append('<label></label>');

						$($(this).siblings()).removeClass("current");
						$($(this).siblings()).children('label').remove();
					}
				});
			}

			//首页传过来的参数
			var selPurpose = this.getUrlParam("selPurpose");
			if (selPurpose != '' || selPurpose != undefined) {
				$("#selectPurpose").val(selPurpose);
			}

			//用途
			if ($("#useCode").val() != '') {
				$("#selectPurpose").val($("#useCode").val());
			}

			//领域
			if ($("#fieldCode").val() != '') {
				$("#selectDomain").val($("#fieldCode").val());
			}

			//排版
			if ($("#isSetType").val() == 'Y') {
				$("#selectAddedSer").val("1");
			}

			//加急
			if ($("#isUrgent").val() == 'Y') {
				$("#urgentOrder").attr("checked", true);
			}


		},
		
		//语言对改变，价格改变,翻译速度改变
		_transPrice:function() {
			var _this = this;
			
        	$("#selectDuad").find('option').each(function() {
        		var val = $(this).val();
        		if (val ==  $(".dropdown .selected").attr('value')) {
        			var selected = $(this);

        			var ordinary = selected.attr("ordinary");
        			var ordinaryUrgent = selected.attr("ordinaryUrgent");
        			var professional = selected.attr("professional");
        			var professionalUrgent = selected.attr("professionalUrgent");
        			var publish = selected.attr("publish");
        			var publishUrgent = selected.attr("publishUrgent");
        			
        			if ($("#urgentOrder").is(':checked') ) {
        				$("#stanPrice").html(professionalUrgent);
        				$("#proPrice").html(professionalUrgent);
        				$("#pubPrice").html(publishUrgent);
        			} else {
        				$("#stanPrice").html(ordinary);
        				$("#proPrice").html(professional);
        				$("#pubPrice").html(publish);
        			}
        			
        		}
            });
		
			this._getSpeed();
		},
		
		//获取翻译速度价格
		_getSpeed:function() {
			var ordSpeed = 2;
			var ordSpeedUrgent = 1;
			var proSpeed = 3;
			var proSpeedUrgent = 2;
			var pubSpeed = 4;
			var pubSpeedUrgent = 3;
			
			if ($("#urgentOrder").is(':checked')) {
				if($(".none-ml.current").attr('name') == 0) {
					$("#speedValue").html(ordSpeedUrgent);
				} else if($(".none-ml.current").attr('name') == 1) {
					$("#speedValue").html(proSpeedUrgent);
				} else {
					$("#speedValue").html(pubSpeedUrgent);
				}
			} else {
				if($(".none-ml.current").attr('name') == 0) {
					$("#speedValue").html(ordSpeed);
				} else if($(".none-ml.current").attr('name') == 1) {
					$("#speedValue").html(proSpeed);
				} else {
					$("#speedValue").html(pubSpeed);
				}
			}
		},

		//文字输入，js控制
		_inputText:function() {
			$("#selectAddedSer").val(2);
			$("#selectFormatConv").val(2);
			$("#selectAddedSer").attr("disabled",true);
			$("#selectFormatConv").attr("disabled",true);
			$("#inputFormatConv").hide();
			$("#inputFormatConv").val("");
		},
	
		//上传文档，js控制
		_uploadFile:function() {
			$("#selectAddedSer").attr("disabled",false);
			$("#selectFormatConv").attr("disabled",false);
            if ( !WebUploader.Uploader.support() ) {
                alert( 'Web Uploader 不支持您的浏览器！如果你使用的是IE浏览器，请尝试升级 flash 播放器');
                throw new Error( 'WebUploader does not support the browser you are using.' );
            }else if(uploader==null){
                this._initUpdate();
            }
		},
		
		//清空输入文字
		_clearText:function() {
			$("#translateContent").val("");
			$("#clear-btn").hide();
		},

		//清空 按钮出现控制
		_clearControl:function() {
			var variable = $("#translateContent").val();
			if (variable !== '')  {
				$("#clear-btn").show();
			} else {
				$("#clear-btn").hide();
			}
		},

		//input 格式转换控制
		_formatControl:function() {
			if (1 == $("#selectFormatConv").val()) {
				$("#inputFormatConv").show();
			} else {
				$("#inputFormatConv").hide();
				$("#inputFormatConv").val("");
			}
				
			
		},

		_showWarn:function(msg){
			new Dialog({
				content:msg,
				icon:'warning',
				okValue: $.i18n.prop("order.info.dialog.ok"),
				title:  $.i18n.prop("order.info.dialog.prompt"),
				ok:function(){
					this.close();
				}
			}).show();
		},

		_showFail:function(msg){
			new Dialog({
				title: $.i18n.prop("order.info.dialog.prompt"),
				content:msg,
				icon:'fail',
				okValue: $.i18n.prop("order.info.dialog.ok"),
				ok:function(){
					this.close();
				}
			}).show();
		},

		//获取url中参数
		getUrlParam:function(name) {
			var reg = new RegExp("(^|&)" + name + "=([^&]*)(&|$)"); //构造一个含有目标参数的正则表达式对象
			var r = window.location.search.substr(1).match(reg);  //匹配目标参数
			if (r != null) return unescape(r[2]); return null; //返回参数值
		},

		//格式化金钱
		fmoney:function (s, n) {
			var result = '0.00';
			if(isNaN(s) || !s){
				return result;
			}

			n = n > 0 && n <= 20 ? n : 2;
			s = parseFloat((s + "").replace(/[^\d\.-]/g, "")).toFixed(n) + "";
			var l = s.split(".")[0].split("").reverse(),
			r = s.split(".")[1];
			var t = "";
			for(var i = 0; i < l.length; i ++ ){
				t += l[i] + ((i + 1) % 3 == 0 && (i + 1) != l.length ? "," : "");
			}
			return t.split("").reverse().join("") + "." + r;
		},
		//初始化上传控件
		_initUpdate:function () {
			var _this= this;
            var FILE_TYPES=['rar','zip','doc','docx','pdf','jpg','png','jif'];
            uploader = WebUploader.create({
                swf : _base+"/resources/spm_modules/webuploader/Uploader.swf",
                server: _base+'/order/uploadFile',
                auto : true,
                pick : "#selectFile",
                dnd: '#fy2', //拖拽
                accept: {
                    title: 'intoTypes',
                    extensions: 'rar,zip,doc,docx,pdf,jpg,png,gif',
                    mimeTypes: 'application/zip,application/msword,application/pdf,image/jpeg,image/png,image/gif'
                },
                resize : false,
                // 禁掉全局的拖拽功能。这样不会出现图片拖进页面的时候，把图片打开。
                disableGlobalDnd: true,
                fileNumLimit: 10,
                fileSizeLimit: 100 * 1024 * 1024,    // 100 M
            });

            uploader.on("beforeFileQueued", function (file) {
                var allSize = file.size;
                var allCount = $("#fileList ul").length + 1
                $("#fileList ul li").each(function() {
                    allSize += $(this).attr("size");
                });

                if (allSize > 100*1024*1024) {
					_this._showWarn($.i18n.prop('order.upload.error.fileSize'));
                    return false;
                }

                if (allCount > 10) {
					_this._showWarn($.i18n.prop('order.upload.error.fileNum'));
                    return false;
                }

                if ($.inArray(file.ext, FILE_TYPES)<0) {
					_this._showWarn($.i18n.prop('order.upload.error.type'));
                    return false;
                }

            });

            uploader.on("fileQueued",function(file){
                $("#fileList ul").css('"border-bottom","none"');
                $("#fileList").append('<ul style="border-bottom: medium none;"><li class="word" size="'+file.size+'" id="'+file.id+'">'+file.name+'</li><li><p class="ash-bj"><span style="width:0%;"></span></p><p name="percent">0%</p></li><li class="right"><i class="icon iconfont" >&#xe618;</i></li></ul>');
            });

            uploader.on("uploadProgress",function(file,percentage){

                var fileId = $("#"+file.id),
                    percent = fileId.find(".progress .progress-bar");
                if(!percent.length){//避免重复创建
                    percent = $('<div class="progress progress-striped active"><div class="progress-bar" role="progressbar" style="width: 0%"></div></div>')
                        .appendTo(fileId).find('.progress-bar');
                }
                fileId.next().find('span').css('width',percentage*100+"%");
                fileId.next().find('p[name="percent"]').text(percentage*100+"%");
                percent.css( 'width', percentage * 100 + '%' );

            });

            uploader.on( 'uploadSuccess', function( file, responseData ) {
                if(responseData.statusCode=="1"){
                    var fileData = responseData.data;
                    console.log(fileData);
                    //文件上传成功
                    if(fileData){
                        $("#"+file.id).attr("fileId", fileData);
                        return;
                    }
                }//上传失败
                else{
					_this._showFail($.i18n.prop('order.upload.error.upload'));
                    //删除文件
                    $("#"+file.id).parent('ul').remove();
                    var file = uploader.getFile(file.id);
                    uploader.removeFile(file);
                    return;
                }
            });

            uploader.on( 'uploadError', function( file, reason ) {
				_this._showFail($.i18n.prop('order.upload.error.upload'));

                //删除文件
                $("#"+file.id).parent('ul').remove();
                var file = uploader.getFile(file.id);
                uploader.removeFile(file);
            });

            uploader.on( 'uploadComplete', function( file ) {
                $( '#'+file.id ).find('.progress').fadeOut();
            });
        },

    });
    
    module.exports = textOrderAddPager;
});