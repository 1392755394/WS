jQuery(document).ready(function($) {
	// 侧边导航
	/*$('.ul-snv li').each(function(){
	    if( $(this).find('dl').length > 0 ){
			if($(this).find('dd').length == 0){
				$(this).find('dl').remove();
			}
			else{
				$(this).addClass('has-sub');	
			}
	    }
	});*/

	//回到顶部
	$('.right-bar ul li.s5 a').click(function(){

	    $('html,body').animate({'scrollTop':0},600);
	});


	$('.ul-snv li a.v1').click(function(){
    	$(".ul-snv li a.v1").attr("href","javascript:void(0);");
	    var dl = $(this).parents('li').find('dl');
	    $(this).parents('li').find('dl').slideToggle();
	    $(this).parents('li').toggleClass('has-sub');
	    return false;
	});

	$('.ul-snv li').last().addClass('last');

	function init_side(){
	    $('.ul-snv li').each(function(){
	        if( $(this).hasClass('hover') ){
	            $(this).find('dl').show();            
	        }
	    });
	}
	init_side();


	$(document).ready(function(){
	//    initState();
	    move();
	    
	})
	function initState(){
	    $('.ul-cooperation .animated,.ul-txt-yp .animated,.box-con ul').each(function(){
	        var dataAnimate = $(this).data('animate');
	        $(this).removeClass(dataAnimate);
	    })
	}
	function move(){
	    var li = $('.ul-cooperation,.ul-txt-yp,.box-con ul').find('li');
	    li.each(function(i){
	    	$(this).delay(i*500).addClass( li.data('animate'));
	    })
	}


	//点击加减100
	$(".money .span2").click(function(){
		var input = $('.money-tz .inp-1'),
			num  = input.val()*1;
		if(num == 0){
			input.val(900)
		}
			num  = input.val()*1;
		$(this).parents(".money").find(".inp-1").val(num + 100);
	})
	 
	$(".money .span1").click(function(){
		var input = $('.money-tz .inp-1'),
			num  = input.val()*1;
		$(this).parents(".money").find(".inp-1").val(num-100);

		if(num <= 1000){
			input.val(1000)
		}
	})

	$(window).scroll(function(){
		if($(window).scrollTop() > 60){
			$('.right-bar ul li.s5').slideDown('300');
		}else{
			$('.right-bar ul li.s5').slideUp('300');
		}
	});


	$('.sub-z').click(function(){
		$('.s-tip').show();
	})


	$('.layer .layer-btns .btn').click(function(){
		$.fancybox.close( true );
	})


	!function(){$("[role=checkbox],[role=radio]").addClass("custom-checkbox-radio"),!function(){var a=".custom-checkbox-radio label input{position: absolute;left: -9999999999px; }",b=document.getElementsByTagName("head")[0],c=document.createElement("style");c.type="text/css",c.styleSheet?c.styleSheet.cssText=a:c.appendChild(document.createTextNode(a)),b.appendChild(c)}(),$("[role=checkbox]").each(function(){var a=$(this).find('input[type="checkbox"]');a.each(function(){$(this).attr("checked")&&($(this).parents("label").addClass("checked"),$(this).prop("checked",!0))}),a.change(function(){$(this).parents("label").toggleClass("checked")})}),$("[role=radio]").each(function(){var a=$(this).find('input[type="radio"]'),b=$(this).find("label");a.each(function(){$(this).attr("checked")&&($(this).parents("label").addClass("checked"),$(this).prop("checked",!0))}),a.change(function(){b.removeClass("checked"),$(this).parents("label").addClass("checked"),a.removeAttr("checked"),$(this).prop("checked",!0)})})}();




});

