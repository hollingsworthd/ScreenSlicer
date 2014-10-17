/* 
 * ScreenSlicer (TM) -- automatic, zero-config web scraping (TM)
 * Copyright (C) 2013-2014 Machine Publishers, LLC
 * ops@machinepublishers.com | screenslicer.com | machinepublishers.com
 * 717 Martin Luther King Dr W Ste I, Cincinnati, Ohio 45220
 *
 * You can redistribute this program and/or modify it under the terms of the
 * GNU Affero General Public License version 3 as published by the Free
 * Software Foundation. Additional permissions or commercial licensing may be
 * available--see LICENSE file or contact Machine Publishers, LLC for details.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License version 3
 * for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * version 3 along with this program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * For general details about how to investigate and report license violations,
 * please see: https://www.gnu.org/licenses/gpl-violation.html
 * and email the author: ops@machinepublishers.com
 * Keep in mind that paying customers have more rights than the AGPL alone offers.
 */

var {Cc, Ci, Cm, Cr, Cu} = require("chrome");
var windows = require("sdk/window/utils");
var browserWindows = require("sdk/windows").browserWindows;
var {viewFor} = require("sdk/view/core");
var tabUtil = require("sdk/tabs/utils");
var tabs = require("sdk/tabs");
var handler = {
  QueryInterface: function(iid){
    if (iid.equals(Ci.nsISupports)
        || iid.equals(Ci.nsISupportsWeakReference)
        || iid.equals(Ci.nsIWebProgressListener)
        || iid.equals(Ci.nsIObserver)){
      return this;
    }
    throw Cr.NS_NOINTERFACE;
  },
  send: function(status){
    try{
      var req = Cc["@mozilla.org/xmlextras/xmlhttprequest;1"].createInstance(Ci.nsIXMLHttpRequest);
      req.open('GET', 'http://127.0.0.1:8888/httpstatus/'+status, false);
      req.send(null);
    }catch(e){}
  },
  observe: function(aSubject, aTopic, aData){
    try{
      aSubject.QueryInterface(Ci.nsIHttpChannel);       
      if((aSubject.loadFlags & Ci.nsIChannel.LOAD_DOCUMENT_URI) && aSubject.loadGroup && aSubject.loadGroup.groupObserver){
        var groupObserver = aSubject.loadGroup.groupObserver;
        groupObserver.QueryInterface(Ci.nsIWebProgress);
        groupObserver.addProgressListener(this, Ci.nsIWebProgress.NOTIFY_ALL);
      }
    }catch(e){}
  },
  onStateChange: function(aWebProgress, aRequest, aStateFlags, aStatus){
    try{
      if((aStateFlags & Ci.nsIWebProgressListener.STATE_STOP)
          && (aStateFlags & Ci.nsIWebProgressListener.STATE_IS_WINDOW)
          && !(aStateFlags & Ci.nsIWebProgressListener.STATE_REDIRECTING)){
        var status = aRequest.responseStatus;
        if(status){
          var statusClean = (status >= 200 && status < 300) || status == 304? 200 : status;
          this.send(statusClean);
        }
      }
    }catch(e){}
  },
  onLocationChange: function(aWebProgress, aRequest, aLocation, aFlags){
    try{
      if(aFlags & Ci.nsIWebProgressListener.LOCATION_CHANGE_ERROR_PAGE){
        this.send(0);
        this.send(499);
      }else if(!(aFlags & Ci.nsIWebProgressListener.LOCATION_CHANGE_SAME_DOCUMENT)){
        this.send(0);
      }
    }catch(e){}
  },
  onProgressChange: function (aWebProgress, aRequest, aCurSelfProgress, aMaxSelfProgress, aCurTotalProgress, aMaxTotalProgress){},
  onStatusChange: function(aWebProgress, aRequest, aStatus, aMessage){},
  onSecurityChange: function(aWebProgress, aRequest, aState){},
};
Cc["@mozilla.org/observer-service;1"].getService(Ci.nsIObserverService).addObserver(handler, "http-on-modify-request", false);
function handleWindow(){
  try{
    var myProgress = tabUtil.getTabBrowser(viewFor(windows.getMostRecentBrowserWindow())).webProgress;
    myProgress.addProgressListener(handler, myProgress.NOTIFY_ALL);
  }catch(e){}
}
tabs.on("activate",handleWindow);
handleWindow();