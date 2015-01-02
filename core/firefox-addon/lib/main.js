/* 
 * ScreenSlicer (TM) -- automatic, zero-config web scraping (TM)
 * Copyright (C) 2013-2015 Machine Publishers, LLC
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

// The following line of code is a Base 64 encoder/decoder from http://jsbase64.codeplex.com/ and is Copyright Vassilis Petroulias [DRDigit]
// Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Apache 2.0 License for the specific language governing permissions and limitations under the License.
// Modification notice: lightly edited to work outside of a browser execution environment and also minified
var B64={alphabet:"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=",lookup:null,encode:function(e){var t=B64.toUtf8(e),n=-1,r=t.length,i,s,o,u=[,,,];var a="";while(++n<r){i=t[n];s=t[++n];u[0]=i>>2;u[1]=(i&3)<<4|s>>4;if(isNaN(s))u[2]=u[3]=64;else{o=t[++n];u[2]=(s&15)<<2|o>>6;u[3]=isNaN(o)?64:o&63}a+=B64.alphabet[u[0]]+B64.alphabet[u[1]]+B64.alphabet[u[2]]+B64.alphabet[u[3]]}return a},decode:function(e){if(e.length%4)throw"InvalidCharacterError: 'B64.decode' failed: The string to be decoded is not correctly encoded.";var t=B64.fromUtf8(e),n=0,r=t.length;var i="";while(n<r){if(t[n]<128)i+=String.fromCharCode(t[n++]);else if(t[n]>191&&t[n]<224)i+=String.fromCharCode((t[n++]&31)<<6|t[n++]&63);else i+=String.fromCharCode((t[n++]&15)<<12|(t[n++]&63)<<6|t[n++]&63)}return i},toUtf8:function(e){var t=-1,n=e.length,r,i=[];if(/^[\x00-\x7f]*$/.test(e))while(++t<n)i.push(e.charCodeAt(t));else while(++t<n){r=e.charCodeAt(t);if(r<128)i.push(r);else if(r<2048)i.push(r>>6|192,r&63|128);else i.push(r>>12|224,r>>6&63|128,r&63|128)}return i},fromUtf8:function(e){var t=-1,n,r=[],i=[,,,];if(!B64.lookup){n=B64.alphabet.length;B64.lookup={};while(++t<n)B64.lookup[B64.alphabet.charAt(t)]=t;t=-1}n=e.length;while(++t<n){i[0]=B64.lookup[e.charAt(t)];i[1]=B64.lookup[e.charAt(++t)];r.push(i[0]<<2|i[1]>>4);i[2]=B64.lookup[e.charAt(++t)];if(i[2]==64)break;r.push((i[1]&15)<<4|i[2]>>2);i[3]=B64.lookup[e.charAt(++t)];if(i[3]==64)break;r.push((i[2]&3)<<6|i[3])}return r}};

var {Cc, Ci, Cm, Cr, Cu} = require("chrome");
var windows = require("sdk/window/utils");
var browserWindows = require("sdk/windows").browserWindows;
var {viewFor} = require("sdk/view/core");
var tabUtil = require("sdk/tabs/utils");
var tabs = require("sdk/tabs");

var prefs = Cc["@mozilla.org/preferences-service;1"].getService(Ci.nsIPrefBranch);
var header = null;
try{
  var headerValue = prefs.getComplexValue('extensions.screenslicer.headers', Ci.nsISupportsString);
  if(headerValue){
    header = headerValue.data;
    if(header){
      header = JSON.parse(B64.decode(header));
    }
  }
}catch(e){}

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
  prevStatus: -1,
  send: function(status){
    try{
      if((this.prevStatus == 0 && status != 0) || (this.prevStatus != 0 && status == 0)){
        this.prevStatus = status;
        var req = Cc["@mozilla.org/xmlextras/xmlhttprequest;1"].createInstance(Ci.nsIXMLHttpRequest);
        req.open('GET', 'http://127.0.0.1:8888/httpstatus/'+status, true);
        req.send(null);
      }
    }catch(e){}
  },
  observe: function(aSubject, aTopic, aData){
    try{
      var httpChannel = aSubject.QueryInterface(Ci.nsIHttpChannel);
      if(header){
        for(var headerName in header) {
          httpChannel.setRequestHeader(headerName, header[headerName], false);
        }
      }
      if((aSubject.loadFlags & Ci.nsIChannel.LOAD_DOCUMENT_URI) && aSubject.loadGroup && aSubject.loadGroup.groupObserver){
        var groupObserver = aSubject.loadGroup.groupObserver;
        groupObserver.QueryInterface(Ci.nsIWebProgress);
        groupObserver.addProgressListener(this, Ci.nsIWebProgress.NOTIFY_ALL);
      }
    }catch(e){}
  },
  onStateChange: function(aWebProgress, aRequest, aStateFlags, aStatus){
    try{
      if(aRequest
          && (aStateFlags & Ci.nsIWebProgressListener.STATE_STOP)
          && ((aStateFlags & Ci.nsIWebProgressListener.STATE_IS_DOCUMENT)
              || (aStateFlags & Ci.nsIWebProgressListener.STATE_RESTORING))
          && !(aStateFlags & Ci.nsIWebProgressListener.STATE_REDIRECTING)){
        var status = aRequest.responseStatus;
        if(status || (aWebProgress
            && aWebProgress.DOMWindow
            && aWebProgress.DOMWindow === aWebProgress.DOMWindow.top)){
          var statusClean = !status || (status >= 200 && status < 300) || status == 304? 200 : status;
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

Cc["@mozilla.org/observer-service;1"].getService(Ci.nsIObserverService).addObserver(handler, "http-on-modify-request", true);
function handleWindow(){
  try{
    var myProgress = tabUtil.getTabBrowser(viewFor(windows.getMostRecentBrowserWindow())).webProgress;
    myProgress.addProgressListener(handler, myProgress.NOTIFY_ALL);
  }catch(e){}
}
tabs.on("activate",handleWindow);
handleWindow();