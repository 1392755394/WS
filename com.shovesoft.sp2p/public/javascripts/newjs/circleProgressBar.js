function circleProgressBar(elementId, percent, radius, width, fontSize, fgColor, bgColor) {
	var element = $("#" + elementId);
	
	if (element.size() == 0)
		return false;
	fgColor = typeof (fgColor) == "string" && fgColor ? fgColor : "#d70b18";
	bgColor = typeof (bgColor) == "string" && bgColor ? bgColor : "#e7e7e7";
	width = isNaN(parseInt(width)) ? 5 : parseInt(width);
	radius = isNaN(parseInt(radius)) ? 35 : parseInt(radius);
	fontSize = isNaN(parseInt(fontSize)) ? Math.round(radius / 2) : parseInt(fontSize);
	percent = isNaN(parseFloat(percent)) ? 1 : parseFloat(percent) / 100;
	
	var bgId = elementId + "-bg";
	var textId = elementId + "-text";
	var paperWidth = radius * 2 +4;
	var paperHeight = radius * 2 +4;
	var bgElement = $("<div></div>").attr("id", bgId).appendTo(element);
	alert(bgElement);
	$("<div></div>").attr("id", textId).css({
		"width" : paperWidth,
		"height" : paperHeight,
		"line-height" : paperHeight-4 + "px",
		"position" : "absolute",
		"margin-top" : "-" + paperHeight + "px",
		"text-align" : "center",
		"color" : "#9e9fa3",
		"font-size" : fontSize + "px",
		"font-family" : "Arial"
	}).text(((percent * 100).toFixed(2).replace(".00","") + "%")).appendTo(element);
	//alert(Math.round(percent * 100,1));
	var paper = Raphael(bgElement.get(0), paperWidth, paperHeight);
	var drawPercent = percent >= 1 ? 0.9999 : percent;
	drawPercent = drawPercent == 0 ? 0.0001 : drawPercent;
	var r1 = radius - width/2,PI = Math.PI, p1 = {
		x : radius+2,
		y : radius * 2+2 - width/2
	}, p2 = {
		x : p1.x + r1 * Math.sin(2 * PI * (1 - drawPercent)),
		y : p1.y - r1 + r1 * Math.cos(2 * PI * (1 - drawPercent))
	};
	
	var path = [['M', p2.x, p2.y], ['A', r1, r1, 0, percent > 0.5 ? 1 : 0, 0, p1.x, p1.y]];
	paper.path(path).attr({
		"stroke-width" : width,
		"stroke" : fgColor
	});
	var path2 = [['M', p2.x, p2.y], ['A', r1, r1, 0, percent < 0.5 ? 1 : 0, 1, p1.x, p1.y]];
	paper.path(path2).attr({
		"stroke-width" : width,
		"stroke" : bgColor
	});
}



















