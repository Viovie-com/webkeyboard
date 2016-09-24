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
  this.xhr.onreadystatechange = () => {
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
  };
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
  this.area.addEventListener("compositionstart", () => {
    this.is_compositing = true;
  });
  this.area.addEventListener("compositionend", () => {
    this.is_compositing = false;
  });
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
  return this.area.innerText;
};

InputArea.prototype.putText = function(text) {
  this.area.innerText = text;
};

InputArea.prototype.clearText = function() {
  this.area.innerText = "";
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

  window.onblur = () => {
    this.area.focusInput();
  }
  window.onfocus = () => {
    this.area.focusInput();
  }
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
  if (!e) e = window.event;
  this.appendText();
  this.send_key('U', e.keyCode, e.shiftKey, e.ctrlKey, e.altKey);
}

WebKeyboard.prototype.down = function(e) {
  if (!e) e = window.event;
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
  var request = new MsgPackRequest((state, response) => {
    this.area.putText(response);
    this.setLocal();
  }, () => {
    this.getText();
  });
  request.get("/text");
}

WebKeyboard.prototype.fillText = function() {
  var request = new MsgPackRequest(() => {
    this.area.clearText();
    this.setDirect();
  }, () => {
    this.fillText();
  });
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
  this.area.setKeyUpEvent(() => {
    var e = window.event;
    if (e.keyCode == 115) { // F4
      this.fillText();
      this.setDisable();
      return false;
    }
    return true;
  });
  this.area.setKeyDownEvent(null);
}

WebKeyboard.prototype.setDirect = function() {
  this.area.setColor('direct');
  this.area.setKeyDownEvent(this.down.bind(this));
  this.area.setKeyUpEvent(this.up.bind(this));
}

window.onload = function() {
  new WebKeyboard(document.body);
};
