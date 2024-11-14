(function() {

  const TELEGRAM_AUTH_HOST = "oauth.telegram.org";

  // https://www.quirksmode.org/js/cookies.html
  function setCookie(name,value,days) {
    var expires = "";
    if (days) {
        var date = new Date();
        date.setTime(date.getTime() + (days*24*60*60*1000));
        expires = "; expires=" + date.toUTCString();
    }
    document.cookie = name + "=" + (value || "")  + expires + "; path=/";
  }

  function getCookie(name) {
    var nameEQ = name + "=";
    var ca = document.cookie.split(';');
    for(var i=0;i < ca.length;i++) {
        var c = ca[i];
        while (c.charAt(0)==' ') c = c.substring(1,c.length);
        if (c.indexOf(nameEQ) == 0) return c.substring(nameEQ.length,c.length);
    }
    return null;
  }

  function eraseCookie(name) {
    document.cookie = name +'=; Path=/; Expires=Thu, 01 Jan 1970 00:00:01 GMT;';
  }

  // Overwrite XMLHttpRequest.open();
  var __XMLHttpRequest_open = XMLHttpRequest.prototype.open;
  XMLHttpRequest.prototype.open = function(method, url, async, user, password) {
    if (document.location.host === "oauth.telegram.org" && url.startsWith("/auth")) {
      console.log(`XHR opened. Method: ${method}, URL: ${url}`);
      this.addEventListener('readystatechange', function(event) {
        if (this.readyState === XMLHttpRequest.OPENED) {
          this.setRequestHeader("Access-Control-Allow-Headers", "true");
          this.setRequestHeader("Access-Control-Expose-Headers", "Set-Cookie");
          this.withCredentials = true;
        } else if (this.readyState === XMLHttpRequest.DONE) {
          var response = event.target.responseText;
          Object.defineProperty(this, 'response', {writable: true});
          Object.defineProperty(this, 'responseText', {writable: true});
          this.response = this.responseText = response;
          console.log(response);
          console.log(event.target.getResponseHeader("Set-Cookie"));
        }
      });
    }
    __XMLHttpRequest_open.apply(this, arguments);
  };

  // Overwrite XMLHttpRequest.send();
  var __XMLHttpRequest_send = XMLHttpRequest.prototype.send;
  XMLHttpRequest.prototype.send = function(data) {
    if (this.readyState == 4 && this.status >= 200 && this.status < 300) {
      console.log(`XHR opened. ResponseURL: ${this.responseURL}, ResponseData: ${this.data}`);
    }
    __XMLHttpRequest_send.apply(this, arguments);
  }

  return "{}";
})(XMLHttpRequest.prototype.open, XMLHttpRequest.prototype.send);
