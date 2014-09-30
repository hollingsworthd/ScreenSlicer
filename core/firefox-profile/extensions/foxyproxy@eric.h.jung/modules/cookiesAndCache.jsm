/**
  FoxyProxy
  Copyright (C) 2006-2014 Eric H. Jung and FoxyProxy, Inc.
  http://getfoxyproxy.org/
  eric.jung@getfoxyproxy.org

  This source code is released under the GPL license, available in the LICENSE
  file at the root of this installation and also online at
  http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
**/

"use strict";

let CI = Components.interfaces, CC = Components.classes, CU = Components.utils,
  cachService = CC["@mozilla.org/network/cache-service;1"].
    getService(CI.nsICacheService),
  cookieService = CC["@mozilla.org/cookiemanager;1"].
    getService(CI.nsICookieManager);

CU.import("resource://foxyproxy/utils.jsm");

let cookiePrefs = utils.getPrefsService("network.cookie."),
  cachePrefs = utils.getPrefsService("browser.cache."),
  securityPrefs = utils.getPrefsService("security."),

  EXPORTED_SYMBOLS = ["cacheMgr", "cookieMgr"],

  cacheMgr = {
    clearCache : function() {
      try {
        cachService.evictEntries(CI.nsICache.STORE_ON_DISK);
        cachService.evictEntries(CI.nsICache.STORE_IN_MEMORY);
        // Thanks for this idea, Torbutton.
        try {
          // This exists in FF 3.6.x up to Gecko 21. Bug 683262 removes
          // logout().
          CC["@mozilla.org/security/crypto;1"].getService(CI.nsIDOMCrypto).
            logout();
        } catch(e) {
          // Failed to use nsIDOMCrypto to clear SSL Session ids.
          // See https://bugzilla.mozilla.org/show_bug.cgi?id=448747. The old
          // approach (using "security.enable_ssl3" worked up to Gecko 23; bug
          // 733642 removes it) is not reliable anymore either, thus trying a
          // new one:
          // Clear all crypto auth tokens. This includes calls to
          // PK11_LogoutAll(), nsNSSComponent::LogoutAuthenticatedPK11()
          // and clearing the SSL session cache.
          let sdr = CC["@mozilla.org/security/sdr;1"].
            getService(CI.nsISecretDecoderRing);
          sdr.logoutAndTeardown();
        }
      }
      catch(e) {
        let fp = CC["@leahscape.org/foxyproxy/service;1"].getService().
          wrappedJSObject;
        fp.notifier.alert(fp.getMessage("foxyproxy"),
          fp.getMessage("clearcache.error", [e]));
      }
    },

    disableCache : function() {
      cachePrefs.setBoolPref("disk.enable", false);
      cachePrefs.setBoolPref("memory.enable", false);
      cachePrefs.setBoolPref("offline.enable", false);
      cachePrefs.setBoolPref("disk_cache_ssl", false);
    }
  },

  cookieMgr = {
    clearCookies : function() {
      cookieService.removeAll();
    },

    rejectCookies : function() {
      cookiePrefs.setIntPref("cookieBehavior", 2);
    }
  };
