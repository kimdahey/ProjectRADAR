
/*** Define global variables ***/


let c, ctx, offsetX, offsetY, mapHeight, entities, markers, items, data, player, name;
let scale = 5;
let gameStart = false;

$(document).ready(() => {

	/*** Establish the WebSocket connection and set up event handlers ***/
	pathname = location.pathname.substring(6, location.pathname.length);
    var webSocket = new WebSocket("ws://" + location.hostname + ":" + location.port + "/websocket?roomID=" + pathname);

   	webSocket.onopen = function(event) {
	  $("#socketStatus").innerHTML = 'Connected to: ' + event.currentTarget.url;
	};

	$("#game").hide();
	$("#waitingRoom").hide();
	$("#getName").show();


    // Send message if enter is pressed in the input field
    $("#codename").keypress(event => {
        if (event.keyCode === 13) {
        	websocketSend(webSocket, "name", event.target.value, false);
        	name = event.target.value;
        	$("#getName").fadeOut();
        	$("#waitingRoom").fadeIn();
        }
    });

    //starts game when start button clicked.
    $("#start").click(event => {
    	websocketSend(webSocket, "game", "start", false);
    })


    webSocket.onmessage = function (msg) {
    	data = JSON.parse(msg.data);
    	console.log(data);
    	if (data.type === "gameMessage") {
    		if (data.message === "start") {
    			$("#waitingRoom").fadeOut();
		    	$("#game").fadeIn();
		    	gameStart = true;
		    	init();

    		}
    	} else {
    		if (gameStart) {
    			player = data.player;
    			entities = data.entities;
    			items = data.items;
    			markers = data.markers;

	    		clearCanvas();
	            determineOffset();
	            drawEntities();
	            drawPlayer();
	            ctx.globalAlpha = "1.0";	
	    	}
    	}
    	
    };

    webSocket.onclose = function () {
    	console.log("websocket connection closed.")
    };

	webSocket.onerror = function(error) {
	  console.log(error);
	  console.log('WebSocket Error: ' + error);
	};

	/*** Interpreting keypress events ***/
	$(document).keydown(event => {

		if (gameStart) {
			switch(event.key){
				case "a": // a for wasd
				case "ArrowLeft":
					websocketSend(webSocket, "key", "left", true);
					break;
				case "d": // d in wasd
				case "ArrowDown":
					websocketSend(webSocket, "key", "right", true);
					break;
				case "w": // w in wasd
				case "ArrowUp":
					websocketSend(webSocket, "key", "up", true);
					break;
				case "s": // s in wasd
				case "ArrowRight":
					websocketSend(webSocket, "key", "down", true);
					break;
        case " ":
          websocketSend(webSocket, "key", "space", true);
          break;
        case "f":
          websocketSend(webSocket, "key", "f", true);
          break;
			}
		}
	})

	$(document).keyup(event => {
		if (gameStart) {
			switch(event.key){
				case "a": // a for wasd
				case "ArrowLeft":
					websocketSend(webSocket, "key", "left", false);
					break;
				case "d": // d in wasd
				case "ArrowRight":
					websocketSend(webSocket, "key", "right", false);
					break;
				case "w": // w in wasd
				case "ArrowUp":
					websocketSend(webSocket, "key", "up", false);
					break;
				case "s": // s in wasd
				case "ArrowDown":
					websocketSend(webSocket, "key", "down", false);
					break;

			}
		}
	})

	$(document).keypress(event => {
		if (gameStart) {
			switch(event.keyCode){
				// case " ": // space bar for attack
				// 	websocketSend(webSocket, "key", "space", false); break;
				case "f": // f for items
					websocketSend(webSocket, "key", "f", false); break;
				case "r": // r for radar
					websocketSend(webSocket, "key", "r", false); break;
			}
		}
	});
});


function websocketSend(webSocket, type, status, held) {
	let x = {
		type: type,
		status: status,
		held: held
	}
	webSocket.send(JSON.stringify(x));
}


/*** CANVAS SPECIFIC FUNCTIONS ***/

//initializes canvas with context
function init() {
	c = document.getElementById("gameCanvas");
	ctx = c.getContext("2d", {alpha: false});
	c.width = 500;
	c.height = 500;
	offsetX = 0;
	offsetY = 0;

	//drawPlayer();
};

function drawPlayer() {
	ctx.beginPath();
	ctx.strokeStyle = "#b8dbd9";
	ctx.lineWidth = 2;
	ctx.arc(c.width/2, c.height/2, 5, 0, 2*Math.PI);
	ctx.stroke();

	drawHP();
    drawName();
    drawPlayerHitbox();
}

function drawPlayerHitbox() {

  let boxes = player.inventory.weapon.attack.currentAttackFrame.hitbox.boxes;
  for(let i = 0; i < boxes.length; i++) {
    let xOff = boxes[i].offset.x;
    let yOff = boxes[i].offset.y;
    let x = xOff + player.center.x;
    let y = yOff + player.center.y;

    drawCircle(offsetX + x, offsetY + convertToCoord(y), boxes[i].radius, "hitbox");
  }
}

function drawCircle(x, y, radius, type) {
	ctx.beginPath();
	switch(type) {
		case "weapon":
			ctx.strokeStyle = "red";
			ctx.fillStyle = "red";
			// maybe change color?? can pick up
			break;
	    case "hitbox":
	      ctx.strokeStyle = "red";
	      ctx.globalAlpha = "0.5";
	      ctx.fillStyle = "red";
      	  break;
		case "item":
			ctx.strokeStyle = "white";
			ctx.fillStyle = "white";
			// change color ??? can pick up
			break;
		case "deco":
			ctx.strokeStyle = "green";
			ctx.fillStyle = "green";
			// change color ??? can pick up
			break;
		case "none":
			ctx.strokeStyle = "orange";
			ctx.fillStyle = "orange";
			break;
	}
	ctx.lineWidth = 2;
	//TODO CHANGE OBSTACLE
	ctx.arc(x, y, radius, 0, 2*Math.PI);
	ctx.stroke();
	ctx.fill();
}

function clearCanvas() {
	ctx.clearRect(0, 0, c.width, c.height);
}


/*** MISCELLANEOUS FUNCTIONS ***/

function drawHP() {
	achepee = player.health;
	ctx.font = "25px Arial";
	ctx.strokeText(achepee,30,30);
}

function drawName() {
	ctx.strokeStyle = "#b8dbd9";
	ctx.font = "13px Arial";
	ctx.textAlign = "center"
	ctx.fillText(name, c.width/2, c.height/2 - 15);
}

function determineOffset() {
	 offsetX = convertToCoord(player.center.x) + c.width/2;
	 offsetY = player.center.y + c.height/2;
	// how much we have to offset from (0,0) to keep player at center
	// that is the offset, ADD to each number so that we can keep it visible onscreen + properly displayed
}

function drawEntities() {
	for (let i = 0; i < entities.length; i++) {
		drawCircle(entities[i].center.x+offsetX, convertToCoord(entities[i].center.y)+offsetY, entities[i].radius, "none");
	}
  for(let i = 0 ; i < items.length; i++) {
    drawCircle(items[i].center.x + offsetX, convertToCoord(items[i].center.y) + offsetY, 3, "item");
  }
  for(let i = 0; i < markers.length; i++){
    drawCircle(markers[i].center.x + offsetX, convertToCoord(markers[i].center.y) + offsetY, 3, "none");

  }
}

function convertToCoord(y) {
	return -1*y;
}
