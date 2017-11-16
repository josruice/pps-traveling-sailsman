function undraw()
{
	var canvas = document.getElementById("canvas");
	var ctx = canvas.getContext("2d");
	ctx.clearRect(0, 0, canvas.width, canvas.height);
}

function draw_grid(min_x, min_y, max_x, max_y, rows, cols)
{
	var canvas = document.getElementById("canvas");
	var ctx = canvas.getContext("2d");
	if (min_x < 0 || max_x > canvas.width)
		throw "Invalid x-axis bounds: " + min_x + " - " + max_x;
	if (min_y < 0 || max_y > canvas.height)
		throw "Invalid y-axis bounds: " + min_y + " - " + max_y;
    // draw vertical lines
    for (var col = 0 ; col <= cols ; ++col) {
        ctx.beginPath();
        ctx.moveTo(min_x + col * (max_x - min_x) / cols, min_y);
        ctx.lineTo(min_x + col * (max_x - min_x) / cols, max_y);
        ctx.closePath();
        ctx.lineWidth = 2;
        ctx.strokeStyle = "grey";
        ctx.stroke();
    }
    // draw horizontal lines
    for (var row = 0 ; row <= rows ; ++row) {
        ctx.beginPath();
        ctx.moveTo(min_x, min_y + row * (max_y - min_y) / rows);
        ctx.lineTo(max_x, min_y + row * (max_y - min_y) / rows);
        ctx.closePath();
        ctx.lineWidth = 2;
        ctx.strokeStyle = "grey";
        ctx.stroke();
    }
}

function rand(n) {
    return Math.floor((Math.random() * n));
}

Number.prototype.hashCode = function(){
    return (this*2654435761) % 4294967296;
}

function draw_dots(min_x, min_y, max_x, max_y, num, xcoords, ycoords, numcols, colors, showID) {
    var ctx = document.getElementById('canvas').getContext('2d');
    for(var i = 0 ; i < num ; ++ i) {
        ctx.beginPath();
        //ctx.moveTo(min_x + xcoords[i] * (max_x - min_x) / cols,
        //    min_y + ycoords[i] * (max_y - min_y) / rows );
        if(showID) {
            radius = 2;
            
        } else {
            radius = 5;
            ctx.arc(
                min_x + (max_x - min_x) * xcoords[i]/10,
                min_y + (max_y - min_y) * ycoords[i]/10,
                radius, 0, 2 * Math.PI);
            ctx.strokeStyle = "black";
            ctx.lineWidth = 1;
            ctx.stroke();
        }
        ctx.arc(
            min_x + (max_x - min_x) * xcoords[i]/10,
            min_y + (max_y - min_y) * ycoords[i]/10,
            radius, 0, 2 * Math.PI);
        ctx.fillStyle = colors[i%numcols];
        ctx.fill();

        ctx.font = "14px Arial";
        ctx.textAlign = "left";
        ctx.lineWidth = 1;
        ctx.strokeStyle = "black";
        ctx.strokeText(i,        min_x + (max_x - min_x) * xcoords[i]/10 + 1,min_y + (max_y - min_y) * ycoords[i]/10- 1);
        
    }
}

function draw_boat(min_x, min_y, max_x, max_y, num, xcoords, ycoords, numcols, colors) {
    var ctx = document.getElementById('canvas').getContext('2d');
    for(var i = 0 ; i < num ; ++ i) {
        ox = min_x + (max_x - min_x) * xcoords[i]/10;
        oy = min_y + (max_y - min_y) * ycoords[i]/10;
        w = 12
        h = 12;
        
        ctx.beginPath();
        ctx.moveTo(ox - w/2, oy + h/4);
        ctx.lineTo(ox - w/4, oy + h/2);
        ctx.lineTo(ox + w/4, oy + h/2);
        ctx.lineTo(ox + w/2, oy + h/4);
        ctx.closePath();
        ctx.strokeStyle = "black";
        ctx.fillStyle = colors[i%numcols];
        ctx.lineWidth = 3;
        ctx.stroke();
        ctx.fill();

        ctx.beginPath();
        ctx.moveTo(ox, oy + h/4);
        ctx.lineTo(ox, oy - h/4);
        ctx.closePath();
        ctx.stroke();

        ctx.beginPath();
        ctx.moveTo(ox,oy - h/4);
        ctx.lineTo(ox, oy - 3*h/4);
        ctx.lineTo(ox + w/2, oy - h/4);
        ctx.closePath();
        ctx.stroke();
        ctx.fillStyle = colors[i % numcols];
        ctx.fill();

        ctx.font = "14px Arial";
        ctx.textAlign = "left";
        ctx.lineWidth = 1;
        ctx.strokeStyle = "black";
        ctx.strokeText(i,        min_x + (max_x - min_x) * xcoords[i]/10 + 5,min_y + (max_y - min_y) * ycoords[i]/10- 5);
        
    }
}

function draw_landmarks(min_x, min_y, max_x, max_y, num, xcoords, ycoords, color) {
    var ctx = document.getElementById('canvas').getContext('2d');
    for(var i = 0 ; i < num ; ++ i) {
        ctx.beginPath();
        offset_x = min_x + (max_x - min_x) * xcoords[i]/10;
        offset_y = min_y + (max_y - min_y) * ycoords[i]/10;
        width = 10;
        height = 10;
        ctx.moveTo(offset_x - width/2, offset_y - height/2);
        ctx.lineTo(offset_x + width/2, offset_y - height/2);
        ctx.lineTo(offset_x + width/2, offset_y + height/2);
        ctx.lineTo(offset_x - width/2, offset_y + height/2);

        ctx.closePath();
        ctx.lineWidth = 2;
        ctx.strokeStyle = "black";
        ctx.stroke();
        ctx.fillStyle=color;
        ctx.fill();
    }
}

function draw_outpost(min_x, min_y, max_x, max_y, rows, cols) {
    var ctx = document.getElementById('canvas').getContext('2d');
    var width = (max_x - min_x) / (cols);
    var height = (max_y - min_y) / rows;
    ctx.beginPath();
    offset_x = min_x;
    offset_y = min_y;
    ctx.moveTo(offset_x + width/4, offset_y + height/4);
    ctx.lineTo(offset_x + width/2, offset_y);
    ctx.lineTo(offset_x + 3*width/4, offset_y + height/4);
    ctx.lineTo(offset_x + 3*width/4, offset_y + 3*height/4);
    ctx.lineTo(offset_x + width/4, offset_y + 3*height/4);
    ctx.closePath();
    ctx.lineWidth = 4;
    ctx.strokeStyle = "black";
    ctx.stroke();
    ctx.fillStyle="yellow";
    ctx.fill();


    ctx.beginPath();
    offset_x = max_x - width;
    offset_y = min_y;
    ctx.moveTo(offset_x + width/4, offset_y + height/4);
    ctx.lineTo(offset_x + width/2, offset_y);
    ctx.lineTo(offset_x + 3*width/4, offset_y + height/4);
    ctx.lineTo(offset_x + 3*width/4, offset_y + 3*height/4);
    ctx.lineTo(offset_x + width/4, offset_y + 3*height/4);
    ctx.closePath();
    ctx.lineWidth = 4;
    ctx.strokeStyle = "black";
    ctx.stroke();
    ctx.fillStyle="yellow";
    ctx.fill();


    ctx.beginPath();
    offset_x = min_x;
    offset_y = max_y - height;
    ctx.moveTo(offset_x + width/4, offset_y + height/4);
    ctx.lineTo(offset_x + width/2, offset_y);
    ctx.lineTo(offset_x + 3*width/4, offset_y + height/4);
    ctx.lineTo(offset_x + 3*width/4, offset_y + 3*height/4);
    ctx.lineTo(offset_x + width/4, offset_y + 3*height/4);
    ctx.closePath();
    ctx.lineWidth = 4;
    ctx.strokeStyle = "black";
    ctx.stroke();
    ctx.fillStyle="yellow";
    ctx.fill();


    ctx.beginPath();
    offset_x = max_x - width;
    offset_y = max_y - height;
    ctx.moveTo(offset_x + width/4, offset_y + height/4);
    ctx.lineTo(offset_x + width/2, offset_y);
    ctx.lineTo(offset_x + 3*width/4, offset_y + height/4);
    ctx.lineTo(offset_x + 3*width/4, offset_y + 3*height/4);
    ctx.lineTo(offset_x + width/4, offset_y + 3*height/4);
    ctx.closePath();
    ctx.lineWidth = 4;
    ctx.strokeStyle = "black";
    ctx.stroke();
    ctx.fillStyle="yellow";
    ctx.fill();
}

function draw_side(min_x, min_y, max_x, max_y, n, groups,  colors, scores, windx ,windy)
{
	var canvas = document.getElementById("canvas");
	var ctx = canvas.getContext("2d");
	if (min_x < 0 || max_x > canvas.width)
		throw "Invalid x-axis bounds: " + min_x + " - " + max_x;
	if (min_y < 0 || max_y > canvas.height)
		throw "Invalid y-axis bounds: " + min_y + " - " + max_y;
    // draw message

    

    ctx.beginPath();
    radius = 20
    ctx.arc(
        min_x + 30,
        min_y + 30,
        radius, 0, 2 * Math.PI);
    ctx.fillStyle = "white";
    ctx.strokeStyle = "black";
    ctx.fill();
    ctx.stroke();

    ctx.beginPath();
    ctx.moveTo(min_x+30, min_y + 30);
    console.log(windx);
    console.log(windy);
    ctx.lineTo(min_x + 30 + windx * 20, min_y + 30 + windy * 20);
    //ctx.closePath();
    ctx.lineWidth = 4;
    ctx.strokeStyle = "grey";
    ctx.stroke();



    ctx.fill();
    ctx.beginPath();
    radius = 3
    ctx.arc(
        min_x + 30 + windx * 20,
        min_y + 30 + windy * 20,
        radius, 0, 2 * Math.PI);
    ctx.fillStyle = "red";
    ctx.fill();

    for(var i = 0 ; i < n ; ++ i) {
        ctx.font = "32px Arial";
        ctx.textAlign = "left";
        ctx.lineWidth = 4;
        ctx.strokeStyle = colors[i];
        ctx.strokeText(groups[i] + ": " + scores[i],        min_x, min_y + 30*(3+i));
        // ctx.strokeText("CPU time: " + cpu + " s", min_x, min_y + 90);
        // ctx.strokeText("Legend:", min_x, min_y + 150);
        ctx.fillStyle = colors[i];
    }

}

function process(data)
{
    // parse data
    data = data.split(",");
    var cur = 0
    var state = data[cur++];
    var n = Number(data[cur++]);
    var groups = new Array(n);
    for(var i = 0 ; i < n ; ++ i) {
        groups[i] = data[cur++];
    }

    var scores = new Array(n);
    for(var i = 0 ; i < n ; ++ i) {
        scores[i] = Number(data[cur++]);
    }

    var playerx = new Array(n);
    var playery = new Array(n);
    for(var i = 0 ; i < n ; ++ i) {
        playerx[i] = Number(data[cur++]);
        playery[i] = Number(data[cur++]);
    }

    var initplayerx = new Array(n);
    var initplayery = new Array(n);
    for(var i = 0 ; i < n ; ++ i) {
        initplayerx[i] = Number(data[cur++]);
        initplayery[i] = Number(data[cur++]);
    }
    var t = Number(data[cur++]);
    var tx = new Array(t);
    var ty = new Array(t);
    for(var i = 0 ; i < t ; ++ i) {
        tx[i] = Number(data[cur++]);
        ty[i] = Number(data[cur++]);
    }
    var windx = Number(data[cur++]);
    var windy = Number(data[cur++]);
    var refresh = Number(data[cur++]);

    console.log("data", data);
    if (refresh < 0.0) refresh = -1;
    else refresh = Math.round(refresh);

    // draw grid
    undraw();
    var minx = 300;
    var miny = 50;
    var maxx = 900;
    var maxy = 650;
    draw_grid(300, 50, 900, 650,1, 1, "black");
    // draw for 1st player
    var colors = ["orange",  "purple", "green", "darkblue", "yellow","lightseagreen"];

    draw_landmarks(minx, miny, maxx, maxy, t, tx, ty, "red");
    draw_dots(minx, miny, maxx, maxy, n, initplayerx, initplayery, 1, ["black"], true);
    draw_boat(minx, miny, maxx, maxy, n, playerx, playery, 6, colors);
    draw_side ( 10,  40,  190, 690, n, groups, colors, scores, windx, windy);
    //draw_outpost(250, 50, 850, 650 , n+2, n+2);
    //draw_shape(250,  50,  850, 650, 50, 50, buildings, cuts, colors, types, highlight == 0);
    return refresh;
}

var latest_version = -1;

function ajax(version, retries, timeout)
{
	var xhr = new XMLHttpRequest();
	xhr.onload = (function() {
		var refresh = -1;
		try {
			if (xhr.readyState != 4)
				throw "Incomplete HTTP request: " + xhr.readyState;
			if (xhr.status != 200)
				throw "Invalid HTTP status: " + xhr.status;
			refresh = process(xhr.responseText);
			if (latest_version < version && paused == 0)
				latest_version = version;
			else
				refresh = -1;
		} catch (message) { alert(message); }
		if (refresh >= 0)
			setTimeout(function() { ajax(version + 1, 10, 100); }, refresh);
	});
	xhr.onabort   = (function() { location.reload(true); });
	xhr.onerror   = (function() { location.reload(true); });
	xhr.ontimeout = (function() {
		if (version <= latest_version)
			console.log("AJAX timeout (version " + version + " <= " + latest_version + ")");
		else if (retries == 0)
			location.reload(true);
		else {
			console.log("AJAX timeout (version " + version + ", retries: " + retries + ")");
			ajax(version, retries - 1, timeout * 2);
		}
	});
	xhr.open("GET", "data.txt", true);
	xhr.responseType = "text";
	xhr.timeout = timeout;
	xhr.send();
}

function pause() {
    paused = (paused + 1) % 2;
}

var paused = 0;
ajax(0, 10, 100);
