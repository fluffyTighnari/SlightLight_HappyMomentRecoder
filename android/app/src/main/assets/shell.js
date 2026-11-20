/*
 * Injected by the Android shell before the page's own scripts run.
 * It only exists inside the APK; the web version never loads this file.
 */
(function () {
  var shell = window.WeiguangShell;
  if (!shell) return;

  var mediaFailed = false;

  /* ------------------------------------------------------------------ */
  /* Camera / microphone                                                */
  /*                                                                    */
  /* Some WebViews reject the page's exact constraints (they ask for    */
  /* disabled echo cancellation, or a strict facingMode). Try the same  */
  /* request once more with plain audio/video, then report the real     */
  /* error instead of letting the page collapse it into a generic hint. */
  /* ------------------------------------------------------------------ */
  var media = navigator.mediaDevices;
  if (media && typeof media.getUserMedia === 'function') {
    var nativeGUM = media.getUserMedia.bind(media);

    media.getUserMedia = function (constraints) {
      return nativeGUM(constraints).catch(function (err) {
        if (err && (err.name === 'NotAllowedError' || err.name === 'SecurityError')) {
          report(err);
          throw err;
        }
        var relaxed = relax(constraints);
        if (!relaxed) {
          report(err);
          throw err;
        }
        return nativeGUM(relaxed).catch(function (err2) {
          report(err2 || err);
          throw err2 || err;
        });
      });
    };
  }

  function relax(c) {
    if (!c || typeof c !== 'object') return null;
    var out = {};
    if (c.audio) out.audio = true;
    if (c.video) out.video = true;
    if (!out.audio && !out.video) return null;
    if (c.audio === true && c.video === undefined) return null;
    if (c.video === true && c.audio === undefined) return null;
    if (c.audio === true && c.video === true) return null;
    return out;
  }

  function report(err) {
    mediaFailed = true;

    var name = (err && err.name) || 'Error';
    var message = (err && err.message) || '';
    var detail = name + (message ? ': ' + message : '');
    var env = 'secure=' + window.isSecureContext +
      ' · media=' + !!navigator.mediaDevices +
      ' · webview=' + call('webViewVersion') +
      ' · 权限=' + call('permissionLog');

    try {
      if (shell.toast) shell.toast('麦克风/相机：' + detail);
    } catch (e) {}

    try {
      var note = document.getElementById('secureNote');
      if (note) {
        note.innerHTML = '相机 / 麦克风没能启动。<br><b>' + esc(detail) + '</b><br>' + esc(env);
      }
    } catch (e) {}
  }

  function call(name) {
    try {
      return shell && typeof shell[name] === 'function' ? String(shell[name]()) : '';
    } catch (e) {
      return '';
    }
  }

  function esc(value) {
    return String(value).replace(/[&<>"]/g, function (c) {
      return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c];
    });
  }

  /* ------------------------------------------------------------------ */
  /* Backup export                                                      */
  /*                                                                    */
  /* The export button builds a JSON Blob and clicks an <a download>. A  */
  /* plain WebView cannot download blob: URLs, so the click is handed to */
  /* the native side: the blob is streamed in slices to a temp file, and */
  /* the system file picker decides where the backup goes.              */
  /* ------------------------------------------------------------------ */
  var nativeClick = HTMLAnchorElement.prototype.click;

  HTMLAnchorElement.prototype.click = function () {
    try {
      if (!this.__wgNative && this.download && /^blob:/i.test(this.href)) {
        this.__wgNative = true;
        saveBlob(this.href, this.download);
        return;
      }
    } catch (e) {
      /* fall through to the browser behaviour */
    }
    return nativeClick.apply(this, arguments);
  };

  function saveBlob(url, name) {
    var mime = /\.json$/i.test(name) ? 'application/json' : 'application/octet-stream';
    shell.beginSave(name, mime);

    fetch(url).then(function (res) {
      return res.blob();
    }).then(function (blob) {
      var step = 128 * 1024;
      var offset = 0;

      function next() {
        if (offset >= blob.size) {
          shell.endSave();
          return;
        }
        var slice = blob.slice(offset, Math.min(offset + step, blob.size));
        offset += step;

        var reader = new FileReader();
        reader.onload = function () {
          var data = String(reader.result || '');
          var comma = data.indexOf(',');
          shell.appendChunk(comma >= 0 ? data.slice(comma + 1) : '');
          next();
        };
        reader.onerror = function () {
          shell.saveFailed('读取备份失败');
        };
        reader.readAsDataURL(slice);
      }

      next();
    }).catch(function () {
      shell.saveFailed('读取备份失败');
    });
  }

  /* The page explains that recording needs https; inside the shell it is already secure. */
  window.addEventListener('load', function () {
    setTimeout(function () {
      if (mediaFailed) return;
      var note = document.getElementById('secureNote');
      if (note) {
        note.innerHTML = '录音与录像已就绪。<br>数据全部留在本机，换设备请用「导出备份」。';
      }
    }, 0);
  });
})();
