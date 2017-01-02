$("#loginname").click(
function(){
	var a=$(this).attr("ischoose"); 
		"1"==a?($(this).removeClass("rgn-checked"),$(this).attr("ischoose","0")):($(this).addClass("rgn-checked"),$(this).attr("ischoose","1"))
})
c();
function c(){
var a=$.cookie("syd_login_name");  
	if(null!=a&&""!=a){
		$("#sjhm").val(a).parent().find("label").hide();
		$("#loginname").attr("ischoose","1");
		$("#loginname").addClass("rgn-checked");
		$("#loginname").attr("checked","true");
	}
}
$(".rgn-nowLogin-submit a").on("click",function(){
	var username = $("#sjhm").val();
	var password=$("#pwd").val();
	if(username==""){
		$("#sjhm").parent().find("label").addClass("rgn-error-msg");
	}else{
		$("#sjhm").parent().find("label").removeClass("rgn-error-msg");
	}
	if(password==""){
		$("#pwd").parent().find("label").addClass("rgn-error-msg");
	}else{
		$("#pwd").parent().find("label").removeClass("rgn-error-msg");
	}
	if(username==""||password==""){return;}
    $.ajax({
    	type: "POST",
        url: "jsonlogin.jhtml",
        data: {username:username,password:password},
        dataType: 'json',
			success: function (resp) {
				if($("#loginname").attr("ischoose")=="1"){
					$.cookie("syd_login_name", username, { path: "/"});
				}else{
					$.cookie('syd_login_name', '', { expires: -1 });
				}
				if(resp.rmsg =="ok") { 
					location.reload();	
					//	bbslogin(resp.bbstime); 
						
				} else {
					$(".rgn-login-error").html(resp.rmsg).removeClass("rgn-hide");
				}
			}
		});
	});
function bbslogin(bbstime){
	$.ajax({
    	type: "get",
        url: bbstime,
        dataType : 'jsonp', 
		jsonp: 'jsoncallback',
		 success: function (json) {},
		 error:function(){
			location.reload();	
			}
		});
}
function bbslogout(){
$.ajax({
    	type: "get",
        url: bbstime,
        dataType : 'jsonp', 
		jsonp: 'jsoncallback',
		 success: function (json) {},
		 error:function(){
			location.reload();	
			}
		});
}

function strTrim(str) {
    return str.replace(/(^\s*)|(\s*$)/g, "")
}

$(function(){
	$("input#sjhm").on("keypress",function(event){
		if(event.keyCode == "13" && strTrim(this.value)!=""){
			$("input#pwd").focus();
		}else{
			return;
		}
	})
	
	$("input#pwd").on("keypress",function(event){
		if(event.keyCode == "13" && strTrim(this.value)!="" && strTrim($("input#pwd").val())!=""){
			$(".rgn-nowLogin-submit a").click();
		}else{
			return;
		}
	})
})