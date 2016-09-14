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
          this.set_status("disconnected");
          try {
            this.onError(this.xhr, msgpack.decode(new Uint8Array(this.xhr.response)));
          } catch (e) {
            this.onError(this.xhr, null);
          }
        } else if (200 <= this.xhr.status && this.xhr.status < 400) {
          this.set_status("connected");
          try {
            this.onSuccess(this.xhr, msgpack.decode(new Uint8Array(this.xhr.response)));
          } catch (e) {
            this.onSuccess(this.xhr, null);
          }
        }
      }
    };
  }

  set_status(status) {
    var status_bar = document.getElementById("status");
    if (status == "connected") {
      status_bar.innerHTML = "Connected";
      status_bar["color"] = "green";
    } else if (status == "Disconnected") {
      status_bar.innerHTML = "Disconnected";
      status_bar["color"] = "red";
    }
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

    this.direct_input();
    this.send();
    this.focus();
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
    if (e.ctrlKey || e.altKey || e.keyCode == 13) { // Enter
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
      this.area.value = response;
      this.local_input();
    }, null);
    request.get("/text");
  }

  submit_text() {
    var request = new MsgPackRequest(() => {
      this.focus();
      this.direct_input();
    }, null);
    request.post('/fill', this.area.value);
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
    this.area.value = "";
    this.area.onkeydown = this.down.bind(this);
    this.area.onkeyup = this.up.bind(this);
    this.area.addEventListener("compositionstart", function() {
      this.is_compositing = true;
    });
    this.area.addEventListener("compositionend", function() {
      this.is_compositing = false;
      this.inputCallback();
    });

    window.onblur = () => {
      this.focus();
    }
    window.onfocus = () => {
      this.focus();
    }
  }

  inputCallback() {
    if (this.area.is_compositing || this.area.value == "")
      return;

    var request = new MsgPackRequest();
    request.post('/append', this.area.value);
    this.area.value = "";
  }

  focus() {
    this.area.focus();
  }
}

window.onload = function() {
  new InputArea(document.getElementById("in"));
};
