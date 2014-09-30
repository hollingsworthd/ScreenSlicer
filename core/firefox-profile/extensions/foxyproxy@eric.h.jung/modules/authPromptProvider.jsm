/**
  FoxyProxy
  Copyright (C) 2006-2014 Eric H. Jung and FoxyProxy, Inc.
  http://getfoxyproxy.org/
  eric.jung@getfoxyproxy.org

  This source code is released under the GPL license, available in the LICENSE
  file at the root of this installation and also online at
  http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
**/

let EXPORTED_SYMBOLS = ["AuthPromptProvider"];

let CC = Components.classes, CI = Components.interfaces, CR = Components.
  results;

function AuthPromptProvider(fp, originalNotificationCallbacks, prePath) {
  this.fp = fp;
  this.fpc = CC["@leahscape.org/foxyproxy/common;1"].getService().
    wrappedJSObject;
  this.originalNotificationCallbacks = originalNotificationCallbacks;
  this.prePath = prePath;
}

AuthPromptProvider.prototype = {
  fp : null,
  fpc : null,
  originalNotificationCallbacks : null,

  prompt : function(dialogTitle, text, passwordRealm, savePassword,
                    defaultText, result) {
    return true;
  },

  promptUsernameAndPassword : function(dialogTitle, text, passwordRealm,
                                       savePassword, user, pwd) {
    user = {value: "qq"};
    pwd = {value: "rr"};
    return true;
  },

  promptPassword : function(dialogTitle, text, passwordRealm, savePassword,
                            pwd) {
  },

  asyncPromptAuth : function(channel, callback, context, level, authInfo) {
    /* 04:03:16 AM) dolske: ericjung: I assume you've looked at
       http://mxr.mozilla.org/mozilla-central/source/toolkit/components/passwordmgr/src/nsLoginManagerPrompter.js#592 ?
    (04:09:19 AM) bz: That would be a bug
    (04:09:21 AM) bz: in our documentation
    (04:09:26 AM) bz: for which I apologize
    (04:09:34 AM) ericjung: no worries, that's why i'm in here :)
    (04:09:37 AM) bz: well
    (04:10:01 AM) ***bz got all sorts of other stuff documented here but not the
                  basic "the callee must call the appropriate method on
                  aCallback" thing
    (04:10:22 AM) bz: It does _sorta_ say it
    (04:10:27 AM) bz: 102    * This has largely the same semantics as
                  promptUsernameAndPassword(),
    (04:10:27 AM) bz: 103    * but must return immediately after calling and
                  return the entered
    (04:10:27 AM) bz: 104    * data in a callback.
    (04:11:08 AM) bz: anyway
    (04:11:14 AM) bz: I think this is sufficiently sorted out
    (04:11:17 AM) bz: g'night!
    (04:11:26 AM) bz: please file a bug as desired on improving the docs.  Best
                  with patch.  ;)
    (04:11:33 AM) ericjung: one sec....so after I populate authInfo in my
                  asyncPromptAuth() impl,
    (04:11:41 AM) bz: yes?
    (04:11:59 AM) ericjung: i should call onAuth... on the nsIAuthPromptCallback
                  argument?
    (04:12:02 AM) ericjung: correct?
    (04:12:17 AM) bz: yes
    (04:12:22 AM) bz: and pass it the context and the authInfo
    (04:12:24 AM) bz: but
    (04:12:26 AM) bz: and this is very important
    (04:12:35 AM) bz: you must do this _after_ your asyncPromptAuth returns
    (04:12:45 AM) ericjung: how??
    (04:12:48 AM) ericjung: this is JS
    (04:12:51 AM) bz: um
    (04:12:56 AM) bz: setTimeout?
    (04:12:59 AM) bz: xpcom timer?
    (04:13:01 AM) ericjung: hm
    (04:13:10 AM) bz: nsIRunnable posted to the thread?
    (04:13:13 AM) ericjung: how long, 10 msec?
    (04:13:15 AM) bz: all sorts of options
    (04:13:18 AM) bz: doesn't matter
    (04:13:24 AM) bz: just needs to be after the function returns
    (04:13:29 AM) bz: as in... asynchronously
    (04:13:36 AM) ericjung: :)
    (04:13:38 AM) ericjung: many thanks
    (04:13:46 AM) bz: no problem
    (04:14:20 AM) bz: That's all assuming you need the async behavior
    (04:14:24 AM) bz: it's not clear to me why you do here.
    (04:14:31 AM) ericjung: i don't, really
    (04:14:39 AM) ericjung: there is no user interaction
    (04:14:59 AM) bz: then just don't bother with it....
    (04:15:12 AM) bz: have asyncPromptAuth throw, or just don't implement it
    (04:15:20 AM) bz: and HTTP will fall back on promptAuth
    (04:15:34 AM) ericjung: any performance gains by implemeting it asynch?
    (04:15:38 AM) bz: nope
    (04:15:45 AM) ericjung: ok then :)
    (04:15:51 AM) bz is now known as bz_sleep
    (04:15:55 AM) bz_sleep: and now, sleep for real */
    throw CR.NS_ERROR_NOT_AVAILABLE;
  },

  promptAuth : function(channel, level, authInfo) {
    let proxyInUse = this.fp.applyMode(channel.URI.spec).proxy;
    // We need this hack here as we have to avoid an infinite loop if wrong
    // credentials got entered. The loop occurs as Mozilla is trying to show
    // a dialog after wrong credentials are sent in order to allow the user
    // to enter the correct ones. But we suppress this dialog and (if entered
    // once) rather send the wrong credentials again (and again, and again...).
    // See: https://mxr.mozilla.org/mozilla-central/source/netwerk/protocol/
    // http/nsHttpChannelAuthProvider.cpp (PromptForIdentiy() and
    // GetCredentialsForChallenge()) for details.
    // Checking whether the counter is < 3 and firing up the dialog if the
    // counter === 2 turns out to be error-prone due to background requests
    // (safebrowsing et.al.).
    // TODO: We have per channel notification callbacks but just one general
    // authCounter?? Hrmm...
    this.fp.authCounter++;
    if (this.fp.authCounter < 10) {
      // Once we recognize that the credentials are wrong (i.e. the counter is
      // > 8) we contact the user and ask her whether she wants to change them
      // now.
      if (this.fp.authCounter === 9) {
        try {
          let win = this.fpc.getMostRecentWindow(null);
          if (proxyInUse && !this.fp.warnings.showWarningIfDesired(win,
              ["authentication.credentials.retry"], "retryAuthCredentials",
              true)) {
            let params = {inn: {proxy: proxyInUse}, out: null};
            win.
              openDialog("chrome://foxyproxy/content/addeditproxy.xul", "",
                "chrome, dialog, modal, resizable=yes", params).focus();
            this.fp.writeSettingsAsync();
          } else {
            this.fp.authCounter = 0;
            return null;
          }
        } catch (e) {
          dump("Error while trying to get the proxy window: " + e + "\n");
        }
      }
      return this._getCredentials(channel, level, authInfo, proxyInUse);
    } else {
      this.fp.authCounter = 0;
      return null;
    }
  },

  // Looking for credentials. Even if we find any, we don't reset the auth
  // counter as we can't be sure that these credentials are still valid/the
  // correct ones. If we would reset the counter we would get caught in an
  // indefinite loop (again).
  _getCredentials : function(channel, level, authInfo, proxy) {
    if (!proxy || !proxy.manualconf.username || !proxy.manualconf.password) {
      let ps = CC["@mozilla.org/embedcomp/prompt-service;1"].
        getService(CI.nsIPromptService2);

      // Pre-populate the prompt with username, if we have one
      if (proxy && proxy.manualconf.username)
        authInfo.username = proxy.manualconf.username;
      // Pre-populate the prompt with password, if we have one
      if (proxy && proxy.manualconf.password)
        authInfo.password = proxy.manualconf.password;

      // If we had no luck with prepopulating the auth dialog try finding some
      // credentials from the login manager. We only consult it if we neither
      // found a username nor a password as we do not know what the user wants
      // if only one of them is available.
      if (!(authInfo.username || authInfo.password)) {
        let proxyLogins;
        let loginManager = CC["@mozilla.org/login-manager;1"].getService(CI.
          nsILoginManager);
        // Does the user have saved something of interest at all?
        proxyLogins = loginManager.countLogins("", null, authInfo.realm);
        if (proxyLogins > 0) {
          // At least one entry that could be interesting. Let's look closer.
          if (channel instanceof CI.nsIProxiedChannel && channel.proxyInfo) {
            // See: |_getAuthTarget()| in https://mxr.mozilla.org/
            // mozilla-central/source/toolkit/components/passwordmgr/
            // nsLoginManagerPrompter.js#1383
            let idnService = CC["@mozilla.org/network/idn-service;1"].
              getService(CI.nsIIDNService);
            let hostname = "moz-proxy://" + idnService.
              convertUTF8toACE(channel.proxyInfo.host) + ":" + channel.
              proxyInfo.port;
            let login = loginManager.findLogins({}, hostname, null,
              authInfo.realm);
            // We don't know what to do if there is more than one entry for a
            // particular hostname + realm. Thus, we only handle the case with
            // |login.length| === 1. Firefox just uses the first one, which
            // seems less than ideal:
            // https://bugzilla.mozilla.org/show_bug.cgi?id=227632
            if (login.length === 1) {
              authInfo.username = login[0].username;
              authInfo.password = login[0].password;
              authInfo.domain = login[0].domain;
              // Save our settings so the user doesn't have to enter them again
              if (proxy) {
                proxy.manualconf.username = authInfo.username;
                proxy.manualconf.password = authInfo.password;
                proxy.manualconf.domain = authInfo.domain;
                this.fp.writeSettingsAsync();
              }
              return authInfo;
            }
          }
        }
      }
      if (ps.promptAuth(this.fpc.getMostRecentWindow(), channel, level,
          authInfo, null, {value:null})) {
        // Save our settings so the user doesn't have to enter them again
        if (proxy) {
          proxy.manualconf.username = authInfo.username;
          proxy.manualconf.password = authInfo.password;
          proxy.manualconf.domain = authInfo.domain;
          this.fp.writeSettingsAsync();
        }
      } else
        return null;
    } else {
      authInfo.username = proxy.manualconf.username;
      authInfo.password = proxy.manualconf.password;
      authInfo.domain = proxy.manualconf.domain;
    }
    return authInfo;
  },

  getAuthPrompt : function(reason, iid, result) {
    if (reason == CI.nsIAuthPromptProvider.PROMPT_PROXY)
      return this;
    else {
      // We have a normal auth prompt. Check if we are using Thunderbird and
      // Lightning. If so check whether we have a CalDAV request and if so let
      // the Lightning code take over...
      if (this.fp.fpc.isThunderbird()) {
        // Lets take the direct path to check whether Lightning is enabled...
        try {
          Components.utils.
            import("resource://calendar/modules/calAuthUtils.jsm", this);
          // We are still here, thus it is enabled. Check whether we have a
          // CalDAV request. Thanks to Fallen for the kind help.
          let that = this;
          let calDAVRequest = this.cal.getCalendarManager().getCalendars({}).
            some(function(x) x.uri.prePath === that.prePath);
          if (calDAVRequest) {
            return new this.cal.auth.Prompt();
          }
        } catch (e) {}
      }
      throw CR.NS_ERROR_NOT_AVAILABLE;
    }
  },

  getInterface : function(aIID) {
    // (06:56:39 PM) bz: ericjung: how about just returning the original
    //               notificationCallbacks?
    // (06:56:51 PM) bz: ericjung: if asked for an nsIBadCertListener/2?

    // We need to throw NS_ERROR_NO_INTERFACE to avoid freezing the application
    // to due bad redirects. For steps to reproduce this, see:
    // https://bugzilla.mozilla.org/show_bug.cgi?id=878998
    if (aIID.equals(CI.nsIBadCertListener2) && this.
        originalNotificationCallbacks) {
      try {
        return this.originalNotificationCallbacks.getInterface(aIID);
      } catch (e if e.name === "NS_NOINTERFACE") {
        throw CR.NS_ERROR_NO_INTERFACE;
      } catch (e) {}
    }
    try {
      return this.QueryInterface(aIID);
    } catch (e) {
      try {
        if (this.originalNotificationCallbacks) {
          // Hand-off all interfaces we don't handle to
          // originalNotificationCallbacks
          return this.originalNotificationCallbacks.getInterface(aIID);
        }
      } catch (e if e.name === "NS_NOINTERFACE") {
        throw CR.NS_ERROR_NO_INTERFACE;
      } catch (e) {}
    }
  },

  QueryInterface: function(aIID) {
    if (aIID.equals(CI.nsISupports) || aIID.equals(CI.nsIInterfaceRequestor) ||
        aIID.equals(CI.nsIAuthPrompt) || aIID.equals(CI.nsIAuthPrompt2) ||
        aIID.equals(CI.nsIAuthPromptProvider)) {
        return this;
    }
    throw CR.NS_ERROR_NO_INTERFACE;
  }
};
