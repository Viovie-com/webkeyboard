'use strict'

var MsgPackRequest = function(onSuccess, onError) {
  this.onSuccess = onSuccess || function(xhr, data) {
    console.log(xhr.status);
  };
  this.onError = onError || function(xhr, data) {
    console.log(xhr.status);
  };
  this.xhr = new XMLHttpRequest();
  this.xhr.responseType = 'arraybuffer';
  this.xhr.onreadystatechange = (function() {
    if (this.xhr.readyState == 4) {
      if (this.xhr.status == 0) {
        try {
          this.onError(this.xhr, msgpack.decode(new Uint8Array(this.xhr.response)));
        } catch (e) {
          this.onError(this.xhr, null);
        }
      } else if (200 <= this.xhr.status && this.xhr.status < 400) {
        try {
          this.onSuccess(this.xhr, msgpack.decode(new Uint8Array(this.xhr.response)));
        } catch (e) {
          this.onSuccess(this.xhr, null);
        }
      }
    }
  }).bind(this);
};

MsgPackRequest.prototype.get = function(url) {
  this.xhr.open('GET', url, true);
  this.xhr.send();
};

MsgPackRequest.prototype.post = function(url, data) {
  var packData = msgpack.encode(data);
  this.xhr.open('POST', url, true);
  this.xhr.setRequestHeader('Content-Type', 'application/x-msgpack');
  this.xhr.send(packData);
};


var InputArea = function(area) {
  this.area = area;
  this.is_compositing = false;
  this.area.addEventListener("compositionstart", (function() {
    this.is_compositing = true;
  }).bind(this));
  this.area.addEventListener("compositionend", (function() {
    this.is_compositing = false;
  }).bind(this));
};

InputArea.prototype.setColor = function(color) {
  if (color == 'disable')
    this.area.style.backgroundColor = "#f1e1cd";
  if (color == 'local')
    this.area.style.backgroundColor = "#FFFFFF";
  if (color == 'direct')
    this.area.style.backgroundColor = "#f1f1ed";
};

InputArea.prototype.getText = function() {
  return this.area.value;
};

InputArea.prototype.putText = function(text) {
  this.area.value = text;
};

InputArea.prototype.clearText = function() {
  this.area.value = "";
};

InputArea.prototype.setKeyDownEvent = function(cb) {
  this.area.onkeydown = cb;
};

InputArea.prototype.setKeyUpEvent = function(cb) {
  this.area.onkeyup = cb;
};

InputArea.prototype.focusInput = function() {
  this.area.focus();
};


var WebKeyboard = function(area) {
  this.area = new InputArea(area);

  window.onblur = (function() {
    this.area.focusInput();
  }).bind(this);
  window.onfocus = (function() {
    this.area.focusInput();
  }).bind(this);
  this.area.focusInput();

  this.setDirect();
};

WebKeyboard.prototype.send_key = function(mode, code, shift_key, ctrl_key, alt_key) {
  var request = new MsgPackRequest();
  request.post("/key", {
    "mode": mode,
    "code": code,
    "shift": shift_key,
    "ctrl": ctrl_key,
    "alt": alt_key
  });
}

WebKeyboard.prototype.up = function(e) {
  this.appendText();
  this.send_key('U', e.keyCode, e.shiftKey, e.ctrlKey, e.altKey);
}

WebKeyboard.prototype.down = function(e) {
  if (e.ctrlKey || e.altKey ||
    e.keyCode == 13 || // Enter
    e.keyCode == 9 || // Tab
    (e.keyCode >= 112 && e.keyCode <= 123) // F1~F12
  ) {
    e.preventDefault();
  }
  if (e.keyCode == 115) {
    this.setDisable();
    this.getText();
    return false;
  }
  this.send_key('D', e.keyCode, e.shiftKey, e.ctrlKey, e.altKey);
}

WebKeyboard.prototype.getText = function() {
  var request = new MsgPackRequest((function(state, response) {
    this.area.putText(response);
    this.setLocal();
  }).bind(this), (function() {
    this.getText();
  }).bind(this));
  request.get("/text");
}

WebKeyboard.prototype.fillText = function() {
  var request = new MsgPackRequest((function() {
    this.area.clearText();
    this.setDirect();
  }).bind(this), (function() {
    this.fillText();
  }).bind(this));
  request.post('/fill', this.area.getText());
}

WebKeyboard.prototype.appendText = function() {
  if (this.area.is_compositing || this.area.getText() == '') return;

  var request = new MsgPackRequest();
  request.post('/append', this.area.getText());
  this.area.clearText();
}

WebKeyboard.prototype.setDisable = function() {
  this.area.setColor('disable');
  this.area.setKeyDownEvent(null);
  this.area.setKeyUpEvent(null);
}

WebKeyboard.prototype.setLocal = function() {
  this.area.setColor('local');
  this.area.setKeyUpEvent((function(e) {
    if (e.keyCode == 115) { // F4
      this.fillText();
      this.setDisable();
      return false;
    }
    return true;
  }).bind(this));
  this.area.setKeyDownEvent(null);
}

WebKeyboard.prototype.setDirect = function() {
  this.area.setColor('direct');
  this.area.setKeyDownEvent(this.down.bind(this));
  this.area.setKeyUpEvent(this.up.bind(this));
}


var VirtualKeyboard = function(keyboard, input_area) {
  this.shift = false;
  this.capslock = false;
  this.input_area = input_area;
  var keys = keyboard.getElementsByTagName('li');
  for (var i = 0; i < keys.length; i++) {
    keys[i].addEventListener('click', this.click.bind(this), false);
  }
}

VirtualKeyboard.prototype.toggleCase = function() {
  var letters = document.getElementsByClassName('letter');
  for (var i = 0; i < letters.length; i++) {
    letters[i].classList.toggle('uppercase');
  }
}

VirtualKeyboard.prototype.toggleSymbol = function() {
  var symbols = document.getElementsByClassName('symbol');
  for (var i = 0; i < symbols.length; i++) {
    var tags = symbols[i].getElementsByTagName('span');
    for (var j = 0; j < tags.length; j++) {
      if (tags[j].classList.contains('invisible'))
        tags[j].classList.remove('invisible');
      else
        tags[j].classList.add('invisible');
    }
  }
}

VirtualKeyboard.prototype.click = function(e) {
  var char = e.target.innerText;

  // Shift keys
  if (e.target.classList.contains('left-shift') || e.target.classList.contains('right-shift')) {
    this.toggleCase();
    this.toggleSymbol();
    this.shift = (this.shift === true) ? false : true;
    this.capslock = false;
    return false;
  }

  // Caps lock
  if (e.target.classList.contains('capslock')) {
    this.toggleCase();
    this.capslock = true;
    return false;
  }

  // Delete
  if (e.target.classList.contains('delete')) {
    var text = this.input_area.value;
    this.input_area.value = text.substr(0, text.length - 1);
    return false;
  }

  // Special characters
  if (e.target.classList.contains('space')) char = ' ';
  if (e.target.classList.contains('tab')) char = "\t";
  if (e.target.classList.contains('return')) char = "\n";

  // Uppercase letter
  if (e.target.classList.contains('uppercase')) char = char.toUpperCase();

  // Remove shift once a key is clicked.
  if (this.shift === true) {
    this.toggleSymbol();
    if (this.capslock === false)
      this.toggleCase();
    this.shift = false;
  }

  // Add the character
  this.input_area.value += char;
}


window.onload = function() {
  new WebKeyboard(document.getElementById('write'));
  new VirtualKeyboard(
    document.getElementById('keyboard'),
    document.getElementById('write'));
};
