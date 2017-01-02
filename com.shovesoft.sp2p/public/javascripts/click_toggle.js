/* ================================================================ 
This copyright notice must be untouched at all times.

The original version of this script and the associated (x)html
is available at http://www.stunicholls.com/various/more.html
Copyright (c) 2005-2007 Stu Nicholls. All rights reserved.
This script and the associated (x)html may be modified in any 
way to fit your requirements.
=================================================================== */


clickMenu = function(menu,element,cname) {
	var getEls = document.getElementById(menu).getElementsByTagName(element);

	for (var i=0; i<getEls.length; i++) {
			getEls[i].onclick=function() {
			if ((this.className.indexOf(cname))!=-1)
			{
			if ((this.className.indexOf('click'))!=-1) {
				this.className=this.className.replace("click", "");;
				}
				else {
				this.className+=" click";
				}
			}
		}
	}
}
