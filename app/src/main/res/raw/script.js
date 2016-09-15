'use strict'

class MsgPackRequest {
  constructor(onSuccess, onError) {
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
  }

  get(url) {
    this.xhr.open('GET', url, true);
    this.xhr.send();
  }

  post(url, data) {
    var packData = msgpack.encode(data);
    this.xhr.open('POST', url, true);
    this.xhr.setRequestHeader('Content-Type', 'application/x-msgpack');
    this.xhr.send(packData);
  }
}

class InputArea {
  constructor(area) {
    this.area = area;
    this.is_compositing = false;

    window.onblur = () => {
      this.area.focus();
    }
    window.onfocus = () => {
      this.area.focus();
    }
    this.area.focus();

    this.direct_input();
    this.send();
  }

  send(mode, code, shift_key, ctrl_key, alt_key) {
    var request = new MsgPackRequest();
    request.post("/key", {
      "mode": mode,
      "code": code,
      "shift": shift_key,
      "ctrl": ctrl_key,
      "alt": alt_key
    });
  }

  up(e) {
    if (!e) e = window.event;
    this.inputCallback();
    this.send('U', e.keyCode, e.shiftKey, e.ctrlKey, e.altKey);
  }

  down(e) {
    if (!e) e = window.event;
    if (e.ctrlKey || e.altKey ||
      e.keyCode == 13 // Enter
      ||
      e.keyCode == 9) { // Tab
      e.preventDefault();
    }
    if (e.keyCode == 115) {
      this.no_input();
      this.recv_text();
      return false;
    }
    this.send('D', e.keyCode, e.shiftKey, e.ctrlKey, e.altKey);
  }

  recv_text() {
    var request = new MsgPackRequest((state, response) => {
      this.area.innerText = response;
      this.local_input();
    }, null);
    request.get("/text");
  }

  submit_text() {
    var request = new MsgPackRequest(() => {
      this.direct_input();
    }, null);
    request.post('/fill', this.area.innerText);
  }

  no_input() {
    this.area.style.backgroundColor = "#f1e1cd";
    this.area.onkeydown = null;
    this.area.onkeyup = null;
    window.onblur = null;
  }

  local_input() {
    this.area.style.backgroundColor = "#FFFFFF";
    this.area.onkeydown = () => {
      var e = window.event;
      if (e.keyCode == 115) { // F4
        this.submit_text();
        this.no_input();
        return false;
      }
      return true;
    }
    this.area.onkeyup = null;
  }

  direct_input() {
    this.area.style.backgroundColor = "#f1f1ed";
    this.area.innerText = "";
    this.area.onkeydown = this.down.bind(this);
    this.area.onkeyup = this.up.bind(this);
    this.area.addEventListener("compositionstart", () => {
      this.is_compositing = true;
    });
    this.area.addEventListener("compositionend", () => {
      this.is_compositing = false;
      this.inputCallback();
    });
  }

  inputCallback() {
    if (this.is_compositing || this.area.innerText == "")
      return;

    var request = new MsgPackRequest();
    request.post('/append', this.area.innerText);
    this.area.innerText = "";
  }
}

window.onload = function() {
  new InputArea(document.body);
};
