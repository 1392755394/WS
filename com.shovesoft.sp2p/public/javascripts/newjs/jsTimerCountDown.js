
 $('.time-c').each(function(){
		var this1 = $(this);  
		//alert($(this)[0].innerHTML);
		var EndTime= new Date(this1.attr('endDate').replace(new RegExp("-","gm"),"/"));
		
	//	var NowTime = new Date();
		var NowTime = new Date($.ajax({async: false}).getResponseHeader("Date"));
		var t =EndTime.getTime() - NowTime.getTime();
		//alert(EndTime.getTime());
		//alert( NowTime.getTime());
		var d=0;var h=0;var m=0;var s=0;
		if(t>=0){
		  d=Math.floor(t/1000/60/60/24);
		  h=Math.floor(t/1000/60/60%24);
		  m=Math.floor(t/1000/60%60);
		  s=Math.floor(t/1000%60);
		}else{
			return;
		}  
		var timeInterval = setInterval(function () {//开始执行倒计时
			if (d==0&& h == 0 && m == 0 && s == 0) { 
				clearInterval(timeInterval); 
				this1.html("");
				this1.append("");
				location.reload();
				return; 
			}//如果时、分、秒都为0时将停止当前的倒计时
			if (s == 0) { s = 60; }//当秒走到0时，再次为60秒
			if (s == 60) {
				if (m == 0 && h > 0) {//当分等于0时并且小时还多余1个小时的时候进里面看看
					h -= 1;//小时减一
					m = 60;//分钟自动默认为60分
					s = 60;//秒自动默认为60秒 
				}
				if(m==0 && h==0 && d > 0){
					d -=1;//天减一
					h = 23;
					m = 60
					s = 60
				}
				m -= 1;//每次当秒走到60秒时，分钟减一
			}
			
			s -= 1;//秒继续跳动，减一
			var ss=s;
			var hh=h;
			var mm=m; 
			if(ss<10){ss='0'+ss;}
			if(hh<10){hh='0'+hh;}
			if(mm<10){mm='0'+mm;} 
			//alert(this1.find('#day').text());
			 this1.html("");
             this1.append('<em>'+d+'</em>'+'天'+'<em>'+hh+'</em>'+'小时' + "<em>" + mm + "</em>"+'分' + '<em>'+ ss+'</em>'+'秒');
		}, 1000);
   });
 