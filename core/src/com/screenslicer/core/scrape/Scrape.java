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
package com.screenslicer.core.scrape;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.tika.Tika;
import org.jsoup.nodes.Node;
import org.openqa.selenium.io.TemporaryFilesystem;
import org.openqa.selenium.remote.BrowserDriver;
import org.openqa.selenium.remote.BrowserDriver.Fatal;
import org.openqa.selenium.remote.BrowserDriver.Profile;
import org.openqa.selenium.remote.BrowserDriver.Retry;

import com.screenslicer.api.datatype.HtmlNode;
import com.screenslicer.api.datatype.Proxy;
import com.screenslicer.api.datatype.Result;
import com.screenslicer.api.datatype.UrlTransform;
import com.screenslicer.api.request.Fetch;
import com.screenslicer.api.request.FormLoad;
import com.screenslicer.api.request.FormQuery;
import com.screenslicer.api.request.KeywordQuery;
import com.screenslicer.api.request.Query;
import com.screenslicer.api.request.Request;
import com.screenslicer.common.CommonUtil;
import com.screenslicer.common.Log;
import com.screenslicer.common.Random;
import com.screenslicer.core.scrape.Proceed.End;
import com.screenslicer.core.scrape.neural.NeuralNetManager;
import com.screenslicer.core.scrape.type.SearchResults;
import com.screenslicer.core.service.ScreenSlicerBatch;
import com.screenslicer.core.util.BrowserUtil;
import com.screenslicer.core.util.NodeUtil;
import com.screenslicer.core.util.UrlUtil;
import com.screenslicer.webapp.WebApp;

import de.l3s.boilerpipe.extractors.NumWordsRulesExtractor;

public class Scrape {
  public static class ActionFailed extends Exception {
    private static final long serialVersionUID = 1L;

    public ActionFailed() {
      super();
    }

    public ActionFailed(Throwable nested) {
      super(nested);
      Log.exception(nested);
    }

    public ActionFailed(String message) {
      super(message);
    }
  }

  public static class Cancelled extends Exception {

  }

  private static volatile BrowserDriver driver = null;
  private static final int MIN_SCRIPT_TIMEOUT = 30;
  private static final int MAX_INIT = 1000;
  private static final int HANG_TIME = 10 * 60 * 1000;
  private static final int RETRIES = 7;
  private static final long WAIT = 2000;
  private static AtomicLong latestThread = new AtomicLong();
  private static AtomicLong curThread = new AtomicLong();
  private static final Object cacheLock = new Object();
  private static final Map<String, List> nextResults = new HashMap<String, List>();
  private static List<String> cacheKeys = new ArrayList<String>();
  private static final int LIMIT_CACHE = 5000;
  private static final int MAX_CACHE = 500;
  private static final int CLEAR_CACHE = 250;
  private static AtomicBoolean done = new AtomicBoolean(false);
  private static final Object progressLock = new Object();
  private static String progress1Key = "";
  private static String progress2Key = "";
  private static String progress1 = "";
  private static String progress2 = "";

  public static final List<Result> WAITING = new ArrayList<Result>();

  public static void init() {
    NeuralNetManager.reset(new File("./resources/neural/config"));
    start(new Request());
    done.set(true);
  }

  private static String initDownloadCache() {
    File downloadCache = new File("./download_cache");
    FileUtils.deleteQuietly(downloadCache);
    downloadCache.mkdir();
    try {
      return downloadCache.getCanonicalPath();
    } catch (Throwable t) {
      Log.exception(t);
      return downloadCache.getAbsolutePath();
    }
  }

  private static void start(Request req) {
    Proxy[] proxies = CommonUtil.isEmpty(req.proxies) ? new Proxy[] { req.proxy } : req.proxies;
    for (int i = 0; i < RETRIES; i++) {
      try {
        Profile profile = new Profile(new File("./firefox-profile"));
        if (!"synthetic".equals(System.getProperty("slicer_events"))) {
          profile.setAlwaysLoadNoFocusLib(true);
          profile.setEnableNativeEvents(true);
        }
        String downloadCachePath = initDownloadCache();
        if (req.downloads) {
          profile.setPreference("browser.download.folderList", 2);
          profile.setPreference("pdfjs.disabled", true);
          profile.setPreference("browser.download.manager.showWhenStarting", false);
          profile.setPreference("browser.download.dir", downloadCachePath);
          profile
              .setPreference(
                  "browser.helperApps.neverAsk.saveToDisk",
                  "application/vnd.hzn-3d-crossword,video/3gpp,video/3gpp2,application/vnd.mseq,application/vnd.3m.post-it-notes,application/vnd.3gpp.pic-bw-large,application/vnd.3gpp.pic-bw-small,application/vnd.3gpp.pic-bw-var,application/vnd.3gpp2.tcap,application/x-7z-compressed,application/x-abiword,application/x-ace-compressed,application/vnd.americandynamics.acc,application/vnd.acucobol,application/vnd.acucorp,audio/adpcm,application/x-authorware-bin,application/x-authorware-map,application/x-authorware-seg,application/vnd.adobe.air-application-installer-package+zip,application/x-shockwave-flash,application/vnd.adobe.fxp,application/pdf,application/vnd.cups-ppd,application/x-director,application/vnd.adobe.xdp+xml,application/vnd.adobe.xfdf,audio/x-aac,application/vnd.ahead.space,application/vnd.airzip.filesecure.azf,application/vnd.airzip.filesecure.azs,application/vnd.amazon.ebook,application/vnd.amiga.ami,application/andrew-inset,application/vnd.android.package-archive,application/vnd.anser-web-certificate-issue-initiation,application/vnd.anser-web-funds-transfer-initiation,application/vnd.antix.game-component,application/vnd.apple.installer+xml,application/applixware,application/vnd.hhe.lesson-player,application/vnd.aristanetworks.swi,text/x-asm,application/atomcat+xml,application/atomsvc+xml,application/atom+xml,application/pkix-attr-cert,audio/x-aiff,video/x-msvideo,application/vnd.audiograph,image/vnd.dxf,model/vnd.dwf,text/plain-bas,application/x-bcpio,application/octet-stream,image/bmp,application/x-bittorrent,application/vnd.rim.cod,application/vnd.blueice.multipass,application/vnd.bmi,application/x-sh,image/prs.btif,application/vnd.businessobjects,application/x-bzip,application/x-bzip2,application/x-csh,text/x-c,application/vnd.chemdraw+xml,text/css,chemical/x-cdx,chemical/x-cml,chemical/x-csml,application/vnd.contact.cmsg,application/vnd.claymore,application/vnd.clonk.c4group,image/vnd.dvb.subtitle,application/cdmi-capability,application/cdmi-container,application/cdmi-domain,application/cdmi-object,application/cdmi-queue,application/vnd.cluetrust.cartomobile-config,application/vnd.cluetrust.cartomobile-config-pkg,image/x-cmu-raster,model/vnd.collada+xml,text/csv,application/mac-compactpro,application/vnd.wap.wmlc,image/cgm,x-conference/x-cooltalk,image/x-cmx,application/vnd.xara,application/vnd.cosmocaller,application/x-cpio,application/vnd.crick.clicker,application/vnd.crick.clicker.keyboard,application/vnd.crick.clicker.palette,application/vnd.crick.clicker.template,application/vnd.crick.clicker.wordbank,application/vnd.criticaltools.wbs+xml,application/vnd.rig.cryptonote,chemical/x-cif,chemical/x-cmdf,application/cu-seeme,application/prs.cww,text/vnd.curl,text/vnd.curl.dcurl,text/vnd.curl.mcurl,text/vnd.curl.scurl,application/vnd.curl.car,application/vnd.curl.pcurl,application/vnd.yellowriver-custom-menu,application/dssc+der,application/dssc+xml,application/x-debian-package,audio/vnd.dece.audio,image/vnd.dece.graphic,video/vnd.dece.hd,video/vnd.dece.mobile,video/vnd.uvvu.mp4,video/vnd.dece.pd,video/vnd.dece.sd,video/vnd.dece.video,application/x-dvi,application/vnd.fdsn.seed,application/x-dtbook+xml,application/x-dtbresource+xml,application/vnd.dvb.ait,application/vnd.dvb.service,audio/vnd.digital-winds,image/vnd.djvu,application/xml-dtd,application/vnd.dolby.mlp,application/x-doom,application/vnd.dpgraph,audio/vnd.dra,application/vnd.dreamfactory,audio/vnd.dts,audio/vnd.dts.hd,image/vnd.dwg,application/vnd.dynageo,application/ecmascript,application/vnd.ecowin.chart,image/vnd.fujixerox.edmics-mmr,image/vnd.fujixerox.edmics-rlc,application/exi,application/vnd.proteus.magazine,application/epub+zip,message/rfc822,application/vnd.enliven,application/vnd.is-xpr,image/vnd.xiff,application/vnd.xfdl,application/emma+xml,application/vnd.ezpix-album,application/vnd.ezpix-package,image/vnd.fst,video/vnd.fvt,image/vnd.fastbidsheet,application/vnd.denovo.fcselayout-link,video/x-f4v,video/x-flv,image/vnd.fpx,image/vnd.net-fpx,text/vnd.fmi.flexstor,video/x-fli,application/vnd.fluxtime.clip,application/vnd.fdf,text/x-fortran,application/vnd.mif,application/vnd.framemaker,image/x-freehand,application/vnd.fsc.weblaunch,application/vnd.frogans.fnc,application/vnd.frogans.ltf,application/vnd.fujixerox.ddd,application/vnd.fujixerox.docuworks,application/vnd.fujixerox.docuworks.binder,application/vnd.fujitsu.oasys,application/vnd.fujitsu.oasys2,application/vnd.fujitsu.oasys3,application/vnd.fujitsu.oasysgp,application/vnd.fujitsu.oasysprs,application/x-futuresplash,application/vnd.fuzzysheet,image/g3fax,application/vnd.gmx,model/vnd.gtw,application/vnd.genomatix.tuxedo,application/vnd.geogebra.file,application/vnd.geogebra.tool,model/vnd.gdl,application/vnd.geometry-explorer,application/vnd.geonext,application/vnd.geoplan,application/vnd.geospace,application/x-font-ghostscript,application/x-font-bdf,application/x-gtar,application/x-texinfo,application/x-gnumeric,application/vnd.google-earth.kml+xml,application/vnd.google-earth.kmz,application/vnd.grafeq,image/gif,text/vnd.graphviz,application/vnd.groove-account,application/vnd.groove-help,application/vnd.groove-identity-message,application/vnd.groove-injector,application/vnd.groove-tool-message,application/vnd.groove-tool-template,application/vnd.groove-vcard,video/h261,video/h263,video/h264,application/vnd.hp-hpid,application/vnd.hp-hps,application/x-hdf,audio/vnd.rip,application/vnd.hbci,application/vnd.hp-jlyt,application/vnd.hp-pcl,application/vnd.hp-hpgl,application/vnd.yamaha.hv-script,application/vnd.yamaha.hv-dic,application/vnd.yamaha.hv-voice,application/vnd.hydrostatix.sof-data,application/hyperstudio,application/vnd.hal+xml,text/html,application/vnd.ibm.rights-management,application/vnd.ibm.secure-container,text/calendar,application/vnd.iccprofile,image/x-icon,application/vnd.igloader,image/ief,application/vnd.immervision-ivp,application/vnd.immervision-ivu,application/reginfo+xml,text/vnd.in3d.3dml,text/vnd.in3d.spot,model/iges,application/vnd.intergeo,application/vnd.cinderella,application/vnd.intercon.formnet,application/vnd.isac.fcs,application/ipfix,application/pkix-cert,application/pkixcmp,application/pkix-crl,application/pkix-pkipath,application/vnd.insors.igm,application/vnd.ipunplugged.rcprofile,application/vnd.irepository.package+xml,text/vnd.sun.j2me.app-descriptor,application/java-archive,application/java-vm,application/x-java-jnlp-file,application/java-serialized-object,text/x-java-source,java,application/javascript,application/json,application/vnd.joost.joda-archive,video/jpm,image/jpeg,video/jpeg,application/vnd.kahootz,application/vnd.chipnuts.karaoke-mmd,application/vnd.kde.karbon,application/vnd.kde.kchart,application/vnd.kde.kformula,application/vnd.kde.kivio,application/vnd.kde.kontour,application/vnd.kde.kpresenter,application/vnd.kde.kspread,application/vnd.kde.kword,application/vnd.kenameaapp,application/vnd.kidspiration,application/vnd.kinar,application/vnd.kodak-descriptor,application/vnd.las.las+xml,application/x-latex,application/vnd.llamagraphics.life-balance.desktop,application/vnd.llamagraphics.life-balance.exchange+xml,application/vnd.jam,application/vnd.lotus-1-2-3,application/vnd.lotus-approach,application/vnd.lotus-freelance,application/vnd.lotus-notes,application/vnd.lotus-organizer,application/vnd.lotus-screencam,application/vnd.lotus-wordpro,audio/vnd.lucent.voice,audio/x-mpegurl,video/x-m4v,application/mac-binhex40,application/vnd.macports.portpkg,application/vnd.osgeo.mapguide.package,application/marc,application/marcxml+xml,application/mxf,application/vnd.wolfram.player,application/mathematica,application/mathml+xml,application/mbox,application/vnd.medcalcdata,application/mediaservercontrol+xml,application/vnd.mediastation.cdkey,application/vnd.mfer,application/vnd.mfmp,model/mesh,application/mads+xml,application/mets+xml,application/mods+xml,application/metalink4+xml,application/vnd.ms-powerpoint.template.macroenabled.12,application/vnd.ms-word.document.macroenabled.12,application/vnd.ms-word.template.macroenabled.12,application/vnd.mcd,application/vnd.micrografx.flo,application/vnd.micrografx.igx,application/vnd.eszigno3+xml,application/x-msaccess,video/x-ms-asf,application/x-msdownload,application/vnd.ms-artgalry,application/vnd.ms-cab-compressed,application/vnd.ms-ims,application/x-ms-application,application/x-msclip,image/vnd.ms-modi,application/vnd.ms-fontobject,application/vnd.ms-excel,application/vnd.ms-excel.addin.macroenabled.12,application/vnd.ms-excel.sheet.binary.macroenabled.12,application/vnd.ms-excel.template.macroenabled.12,application/vnd.ms-excel.sheet.macroenabled.12,application/vnd.ms-htmlhelp,application/x-mscardfile,application/vnd.ms-lrm,application/x-msmediaview,application/x-msmoney,application/vnd.openxmlformats-officedocument.presentationml.presentation,application/vnd.openxmlformats-officedocument.presentationml.slide,application/vnd.openxmlformats-officedocument.presentationml.slideshow,application/vnd.openxmlformats-officedocument.presentationml.template,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,application/vnd.openxmlformats-officedocument.spreadsheetml.template,application/vnd.openxmlformats-officedocument.wordprocessingml.document,application/vnd.openxmlformats-officedocument.wordprocessingml.template,application/x-msbinder,application/vnd.ms-officetheme,application/onenote,audio/vnd.ms-playready.media.pya,video/vnd.ms-playready.media.pyv,application/vnd.ms-powerpoint,application/vnd.ms-powerpoint.addin.macroenabled.12,application/vnd.ms-powerpoint.slide.macroenabled.12,application/vnd.ms-powerpoint.presentation.macroenabled.12,application/vnd.ms-powerpoint.slideshow.macroenabled.12,application/vnd.ms-project,application/x-mspublisher,application/x-msschedule,application/x-silverlight-app,application/vnd.ms-pki.stl,application/vnd.ms-pki.seccat,application/vnd.visio,video/x-ms-wm,audio/x-ms-wma,audio/x-ms-wax,video/x-ms-wmx,application/x-ms-wmd,application/vnd.ms-wpl,application/x-ms-wmz,video/x-ms-wmv,video/x-ms-wvx,application/x-msmetafile,application/x-msterminal,application/msword,application/x-mswrite,application/vnd.ms-works,application/x-ms-xbap,application/vnd.ms-xpsdocument,audio/midi,application/vnd.ibm.minipay,application/vnd.ibm.modcap,application/vnd.jcp.javame.midlet-rms,application/vnd.tmobile-livetv,application/x-mobipocket-ebook,application/vnd.mobius.mbk,application/vnd.mobius.dis,application/vnd.mobius.plc,application/vnd.mobius.mqy,application/vnd.mobius.msl,application/vnd.mobius.txf,application/vnd.mobius.daf,text/vnd.fly,application/vnd.mophun.certificate,application/vnd.mophun.application,video/mj2,audio/mpeg,video/vnd.mpegurl,video/mpeg,application/mp21,audio/mp4,video/mp4,application/mp4,application/vnd.apple.mpegurl,application/vnd.musician,application/vnd.muvee.style,application/xv+xml,application/vnd.nokia.n-gage.data,application/vnd.nokia.n-gage.symbian.install,application/x-dtbncx+xml,application/x-netcdf,application/vnd.neurolanguage.nlu,application/vnd.dna,application/vnd.noblenet-directory,application/vnd.noblenet-sealer,application/vnd.noblenet-web,application/vnd.nokia.radio-preset,application/vnd.nokia.radio-presets,text/n3,application/vnd.novadigm.edm,application/vnd.novadigm.edx,application/vnd.novadigm.ext,application/vnd.flographit,audio/vnd.nuera.ecelp4800,audio/vnd.nuera.ecelp7470,audio/vnd.nuera.ecelp9600,application/oda,application/ogg,audio/ogg,video/ogg,application/vnd.oma.dd2+xml,application/vnd.oasis.opendocument.text-web,application/oebps-package+xml,application/vnd.intu.qbo,application/vnd.openofficeorg.extension,application/vnd.yamaha.openscoreformat,audio/webm,video/webm,application/vnd.oasis.opendocument.chart,application/vnd.oasis.opendocument.chart-template,application/vnd.oasis.opendocument.database,application/vnd.oasis.opendocument.formula,application/vnd.oasis.opendocument.formula-template,application/vnd.oasis.opendocument.graphics,application/vnd.oasis.opendocument.graphics-template,application/vnd.oasis.opendocument.image,application/vnd.oasis.opendocument.image-template,application/vnd.oasis.opendocument.presentation,application/vnd.oasis.opendocument.presentation-template,application/vnd.oasis.opendocument.spreadsheet,application/vnd.oasis.opendocument.spreadsheet-template,application/vnd.oasis.opendocument.text,application/vnd.oasis.opendocument.text-master,application/vnd.oasis.opendocument.text-template,image/ktx,application/vnd.sun.xml.calc,application/vnd.sun.xml.calc.template,application/vnd.sun.xml.draw,application/vnd.sun.xml.draw.template,application/vnd.sun.xml.impress,application/vnd.sun.xml.impress.template,application/vnd.sun.xml.math,application/vnd.sun.xml.writer,application/vnd.sun.xml.writer.global,application/vnd.sun.xml.writer.template,application/x-font-otf,application/vnd.yamaha.openscoreformat.osfpvg+xml,application/vnd.osgi.dp,application/vnd.palm,text/x-pascal,application/vnd.pawaafile,application/vnd.hp-pclxl,application/vnd.picsel,image/x-pcx,image/vnd.adobe.photoshop,application/pics-rules,image/x-pict,application/x-chat,application/pkcs10,application/x-pkcs12,application/pkcs7-mime,application/pkcs7-signature,application/x-pkcs7-certreqresp,application/x-pkcs7-certificates,application/pkcs8,application/vnd.pocketlearn,image/x-portable-anymap,image/x-portable-bitmap,application/x-font-pcf,application/font-tdpfr,application/x-chess-pgn,image/x-portable-graymap,image/png,image/x-portable-pixmap,application/pskc+xml,application/vnd.ctc-posml,application/postscript,application/x-font-type1,application/vnd.powerbuilder6,application/pgp-encrypted,application/pgp-signature,application/vnd.previewsystems.box,application/vnd.pvi.ptid1,application/pls+xml,application/vnd.pg.format,application/vnd.pg.osasli,text/prs.lines.tag,application/x-font-linux-psf,application/vnd.publishare-delta-tree,application/vnd.pmi.widget,application/vnd.quark.quarkxpress,application/vnd.epson.esf,application/vnd.epson.msf,application/vnd.epson.ssf,application/vnd.epson.quickanime,application/vnd.intu.qfx,video/quicktime,application/x-rar-compressed,audio/x-pn-realaudio,audio/x-pn-realaudio-plugin,application/rsd+xml,application/vnd.rn-realmedia,application/vnd.realvnc.bed,application/vnd.recordare.musicxml,application/vnd.recordare.musicxml+xml,application/relax-ng-compact-syntax,application/vnd.data-vision.rdz,application/rdf+xml,application/vnd.cloanto.rp9,application/vnd.jisp,application/rtf,text/richtext,application/vnd.route66.link66+xml,application/rss+xml,application/shf+xml,application/vnd.sailingtracker.track,image/svg+xml,application/vnd.sus-calendar,application/sru+xml,application/set-payment-initiation,application/set-registration-initiation,application/vnd.sema,application/vnd.semd,application/vnd.semf,application/vnd.seemail,application/x-font-snf,application/scvp-vp-request,application/scvp-vp-response,application/scvp-cv-request,application/scvp-cv-response,application/sdp,text/x-setext,video/x-sgi-movie,application/vnd.shana.informed.formdata,application/vnd.shana.informed.formtemplate,application/vnd.shana.informed.interchange,application/vnd.shana.informed.package,application/thraud+xml,application/x-shar,image/x-rgb,application/vnd.epson.salt,application/vnd.accpac.simply.aso,application/vnd.accpac.simply.imp,application/vnd.simtech-mindmapper,application/vnd.commonspace,application/vnd.yamaha.smaf-audio,application/vnd.smaf,application/vnd.yamaha.smaf-phrase,application/vnd.smart.teacher,application/vnd.svd,application/sparql-query,application/sparql-results+xml,application/srgs,application/srgs+xml,application/ssml+xml,application/vnd.koan,text/sgml,application/vnd.stardivision.calc,application/vnd.stardivision.draw,application/vnd.stardivision.impress,application/vnd.stardivision.math,application/vnd.stardivision.writer,application/vnd.stardivision.writer-global,application/vnd.stepmania.stepchart,application/x-stuffit,application/x-stuffitx,application/vnd.solent.sdkm+xml,application/vnd.olpc-sugar,audio/basic,application/vnd.wqd,application/vnd.symbian.install,application/smil+xml,application/vnd.syncml+xml,application/vnd.syncml.dm+wbxml,application/vnd.syncml.dm+xml,application/x-sv4cpio,application/x-sv4crc,application/sbml+xml,text/tab-separated-values,image/tiff,application/vnd.tao.intent-module-archive,application/x-tar,application/x-tcl,application/x-tex,application/x-tex-tfm,application/tei+xml,text/plain,application/vnd.spotfire.dxp,application/vnd.spotfire.sfs,application/timestamped-data,application/vnd.trid.tpt,application/vnd.triscape.mxs,text/troff,application/vnd.trueapp,application/x-font-ttf,text/turtle,application/vnd.umajin,application/vnd.uoml+xml,application/vnd.unity,application/vnd.ufdl,text/uri-list,application/vnd.uiq.theme,application/x-ustar,text/x-uuencode,text/x-vcalendar,text/x-vcard,application/x-cdlink,application/vnd.vsf,model/vrml,application/vnd.vcx,model/vnd.mts,model/vnd.vtu,application/vnd.visionary,video/vnd.vivo,application/ccxml+xml,,application/voicexml+xml,application/x-wais-source,application/vnd.wap.wbxml,image/vnd.wap.wbmp,audio/x-wav,application/davmount+xml,application/x-font-woff,application/wspolicy+xml,image/webp,application/vnd.webturbo,application/widget,application/winhlp,text/vnd.wap.wml,text/vnd.wap.wmlscript,application/vnd.wap.wmlscriptc,application/vnd.wordperfect,application/vnd.wt.stf,application/wsdl+xml,image/x-xbitmap,image/x-xpixmap,image/x-xwindowdump,application/x-x509-ca-cert,application/x-xfig,application/xhtml+xml,application/xml,application/xcap-diff+xml,application/xenc+xml,application/patch-ops-error+xml,application/resource-lists+xml,application/rls-services+xml,application/resource-lists-diff+xml,application/xslt+xml,application/xop+xml,application/x-xpinstall,application/xspf+xml,application/vnd.mozilla.xul+xml,chemical/x-xyz,text/yaml,application/yang,application/yin+xml,application/vnd.zul,application/zip,application/vnd.handheld-entertainment+xml,application/vnd.zzazz.deck+xml,");
        }
        for (int curProxy = 0; curProxy < proxies.length; curProxy++) {
          Proxy proxy = proxies[curProxy];
          if (proxy != null) {
            if (!CommonUtil.isEmpty(proxy.username) || !CommonUtil.isEmpty(proxy.password)) {
              String user = proxy.username == null ? "" : proxy.username;
              String pass = proxy.password == null ? "" : proxy.password;
              profile.setPreference("extensions.closeproxyauth.authtoken",
                  Base64.encodeBase64String((user + ":" + pass).getBytes("utf-8")));
            } else {
              profile.setPreference("extensions.closeproxyauth.authtoken", "");
            }
            if (Proxy.TYPE_ALL.equals(proxy.type)
                || Proxy.TYPE_SOCKS_5.equals(proxy.type)
                || Proxy.TYPE_SOCKS_4.equals(proxy.type)) {
              profile.setPreference("network.proxy.type", 1);
              profile.setPreference("network.proxy.socks", proxy.ip);
              profile.setPreference("network.proxy.socks_port", proxy.port);
              profile.setPreference("network.proxy.socks_remote_dns", true);
              profile.setPreference("network.proxy.socks_version",
                  Proxy.TYPE_ALL.equals(proxy.type) || Proxy.TYPE_SOCKS_5.equals(proxy.type) ? 5 : 4);
            }
            if (Proxy.TYPE_ALL.equals(proxy.type)
                || Proxy.TYPE_SSL.equals(proxy.type)) {
              profile.setPreference("network.proxy.type", 1);
              profile.setPreference("network.proxy.ssl", proxy.ip);
              profile.setPreference("network.proxy.ssl_port", proxy.port);
            }
            if (Proxy.TYPE_ALL.equals(proxy.type)
                || Proxy.TYPE_HTTP.equals(proxy.type)) {
              profile.setPreference("network.proxy.type", 1);
              profile.setPreference("network.proxy.http", proxy.ip);
              profile.setPreference("network.proxy.http_port", proxy.port);
            }
          }
        }
        if (!CommonUtil.isEmpty(req.httpHeaders)) {
          profile.setPreference("extensions.screenslicer.headers",
              Base64.encodeBase64String(CommonUtil.gson.toJson(
                  req.httpHeaders, CommonUtil.stringType).getBytes("utf-8")));
        }
        if (req.browserPrefs != null) {
          for (Map.Entry<String, Object> entry : req.browserPrefs.entrySet()) {
            if (entry.getValue() instanceof Integer) {
              profile.setPreference(entry.getKey(), (Integer) entry.getValue());
            } else if (entry.getValue() instanceof Double) {
              profile.setPreference(entry.getKey(), (int) Math.rint(((Double) entry.getValue())));
            } else if (entry.getValue() instanceof Boolean) {
              profile.setPreference(entry.getKey(), (Boolean) entry.getValue());
            } else if (entry.getValue() instanceof String) {
              profile.setPreference(entry.getKey(), (String) entry.getValue());
            }
          }
        }
        if (req.timeout > MIN_SCRIPT_TIMEOUT) {
          profile.setPreference("dom.max_chrome_script_run_time", req.timeout);
          profile.setPreference("dom.max_script_run_time", req.timeout);
        }
        driver = new BrowserDriver(profile);
        try {
          driver.manage().timeouts().pageLoadTimeout(req.timeout, TimeUnit.SECONDS);
          driver.manage().timeouts().setScriptTimeout(req.timeout, TimeUnit.SECONDS);
          driver.manage().timeouts().implicitlyWait(0, TimeUnit.SECONDS);
        } catch (Throwable t) {
          //marionette connection doesn't allow setting timeouts
        }
        break;
      } catch (Throwable t1) {
        if (driver != null) {
          try {
            forceQuit();
            driver = null;
          } catch (Throwable t2) {
            Log.exception(t2);
          }
        }
        Log.exception(t1);
      }
    }
  }

  public static void forceQuit() {
    try {
      if (driver != null) {
        driver.kill();
        BrowserUtil.driverSleepStartup();
      }
    } catch (Throwable t) {
      Log.exception(t);
    }
    try {
      TemporaryFilesystem tempFS = TemporaryFilesystem.getDefaultTmpFS();
      tempFS.deleteTemporaryFiles();
    } catch (Throwable t) {
      Log.exception(t);
    }
  }

  private static void restart(Request req) {
    try {
      forceQuit();
    } catch (Throwable t) {
      Log.exception(t);
    }
    try {
      driver = null;
    } catch (Throwable t) {
      Log.exception(t);
    }
    start(req);
  }

  private static void push(String mapKey, List results) {
    synchronized (cacheLock) {
      nextResults.put(mapKey, results);
      if (nextResults.size() == LIMIT_CACHE) {
        List<String> toRemove = new ArrayList<String>();
        for (Map.Entry<String, List> entry : nextResults.entrySet()) {
          if (!cacheKeys.contains(entry.getKey())
              && !entry.getKey().equals(mapKey)) {
            toRemove.add(entry.getKey());
          }
        }
        for (String key : toRemove) {
          nextResults.remove(key);
        }
        nextResults.put(mapKey, results);
      }
      if (results != null && !results.isEmpty()) {
        if (cacheKeys.size() == MAX_CACHE) {
          List<String> newCache = new ArrayList<String>();
          for (int i = 0; i < CLEAR_CACHE; i++) {
            nextResults.remove(cacheKeys.get(i));
          }
          for (int i = CLEAR_CACHE; i < MAX_CACHE; i++) {
            newCache.add(cacheKeys.get(i));
          }
          cacheKeys = newCache;
        }
        cacheKeys.add(mapKey);
      }
    }
  }

  public static List<Result> cached(String mapKey) {
    synchronized (cacheLock) {
      if (nextResults.containsKey(mapKey)) {
        List<Result> ret = nextResults.get(mapKey);
        if (ret == null) {
          return WAITING;
        }
        return ret;
      } else {
        return null;
      }
    }
  }

  public static boolean busy() {
    return !done.get();
  }

  public static String progress(String mapKey) {
    synchronized (progressLock) {
      if (progress1Key.equals(mapKey)) {
        return progress1;
      }
      if (progress2Key.equals(mapKey)) {
        return progress2;
      }
      return "";
    }
  }

  private static String toCacheUrl(String url, boolean fallback) {
    if (url == null) {
      return null;
    }
    if (fallback) {
      return "http://webcache.googleusercontent.com/search?q=cache:" + url.split("://")[1];
    }
    String[] urlParts = url.split("://")[1].split("/", 2);
    String urlLhs = urlParts[0];
    String urlRhs = urlParts.length > 1 ? urlParts[1] : "";
    return "http://" + urlLhs + ".nyud.net:8080/" + urlRhs;
  }

  private static class Downloaded {
    String content;
    String mimeType;
    String extension;
    String filename;

    public Downloaded() {
      File file = new File("./download_cache");
      Collection<File> list = FileUtils.listFiles(file, null, false);
      if (!list.isEmpty()) {
        try {
          File download = list.iterator().next();
          byte[] bytes = FileUtils.readFileToByteArray(download);
          content = Base64.encodeBase64String(bytes);
          filename = download.getName();
          mimeType = new Tika().detect(bytes, filename);
          int index = filename.lastIndexOf(".");
          if (index > -1 && index < filename.length()) {
            extension = filename.substring(index + 1).toLowerCase();
            filename = filename.substring(0, index);
          }
        } catch (Throwable t) {
          Log.exception(t);
        } finally {
          for (File cur : list) {
            FileUtils.deleteQuietly(cur);
          }
        }
      }
    }
  }

  private static void fetch(BrowserDriver driver, Request req, Query query, Query recQuery,
      SearchResults results, int depth, SearchResults recResults,
      Map<String, Object> cache) throws ActionFailed {
    boolean terminate = false;
    try {
      String origHandle = driver.getWindowHandle();
      String origUrl = driver.getCurrentUrl();
      String newHandle = null;
      if (query.fetchCached) {
        newHandle = BrowserUtil.newWindow(driver, depth == 0);
      }
      try {
        for (int i = query.currentResult(); i < results.size(); i++) {
          initDownloadCache();
          if (query.requireResultAnchor && !isUrlValid(results.get(i).url)
              && UrlUtil.uriScheme.matcher(results.get(i).url).matches()) {
            results.get(i).close();
            query.markResult(i + 1);
            continue;
          }
          if (ScreenSlicerBatch.isCancelled(req.runGuid)) {
            return;
          }
          Log.info("Fetching URL " + results.get(i).url + ". Cached: " + query.fetchCached, false);
          try {
            results.get(i).pageHtml = getHelper(driver, query.throttle,
                CommonUtil.parseFragment(results.get(i).urlNode, false), results.get(i).url, query.fetchCached,
                req.runGuid, query.fetchInNewWindow, depth == 0 && query == null,
                query == null ? null : query.postFetchClicks);
            Downloaded downloaded = new Downloaded();
            results.get(i).pageBinary = downloaded.content;
            results.get(i).pageBinaryMimeType = downloaded.mimeType;
            results.get(i).pageBinaryExtension = downloaded.extension;
            results.get(i).pageBinaryFilename = downloaded.filename;
            if (!CommonUtil.isEmpty(results.get(i).pageHtml)) {
              try {
                results.get(i).pageText = NumWordsRulesExtractor.INSTANCE.getText(results.get(i).pageHtml);
              } catch (Throwable t) {
                results.get(i).pageText = null;
                Log.exception(t);
              }
            }
            if (recQuery != null) {
              recResults.addPage(scrape(recQuery, req, depth + 1, false, cache));
            }
            if (query.collapse) {
              results.get(i).close();
            }
            query.markResult(i + 1);
          } catch (Retry r) {
            terminate = true;
            throw r;
          } catch (Fatal f) {
            terminate = true;
            throw f;
          } catch (Throwable t) {
            terminate = true;
            throw new ActionFailed(t);
          }
          try {
            if (!driver.getWindowHandle().equals(origHandle)) {
              driver.close();
              driver.switchTo().window(origHandle);
              driver.switchTo().defaultContent();
            } else if (!query.fetchInNewWindow) {
              BrowserUtil.get(driver, origUrl, true, depth == 0);
              SearchResults.revalidate(driver, false);
            }
          } catch (Retry r) {
            terminate = true;
            throw r;
          } catch (Fatal f) {
            terminate = true;
            throw f;
          } catch (Throwable t) {
            terminate = true;
            throw new ActionFailed(t);
          }
        }
      } catch (Retry r) {
        terminate = true;
        throw r;
      } catch (Fatal f) {
        terminate = true;
        throw f;
      } catch (Throwable t) {
        terminate = true;
        throw new ActionFailed(t);
      } finally {
        if (!terminate) {
          if (!query.fetchInNewWindow || (query.fetchCached && origHandle.equals(newHandle))) {
            if (query.fetchInNewWindow) {
              Log.exception(new Throwable("Failed opening new window"));
            }
            BrowserUtil.get(driver, origUrl, true, depth == 0);
          } else {
            BrowserUtil.handleNewWindows(driver, origHandle, depth == 0);
          }
        }
      }
    } catch (Retry r) {
      terminate = true;
      throw r;
    } catch (Fatal f) {
      terminate = true;
      throw f;
    } catch (Throwable t) {
      terminate = true;
      throw new ActionFailed(t);
    } finally {
      if (!terminate) {
        BrowserUtil.driverSleepRand(query.throttle);
      }
    }
  }

  private static String getHelper(final BrowserDriver driver, final boolean throttle,
      final Node urlNode, final String url, final boolean p_cached, final String runGuid,
      final boolean toNewWindow, final boolean init, final HtmlNode[] postFetchClicks) {
    if (!CommonUtil.isEmpty(url) || urlNode != null) {
      final Object resultLock = new Object();
      final String initVal;
      final String[] result;
      synchronized (resultLock) {
        initVal = Random.next();
        result = new String[] { initVal };
      }
      final AtomicBoolean started = new AtomicBoolean();
      Thread thread = new Thread(new Runnable() {
        @Override
        public void run() {
          boolean terminate = false;
          started.set(true);
          boolean cached = p_cached;
          String newHandle = null;
          String origHandle = null;
          try {
            origHandle = driver.getWindowHandle();
            String content = null;
            if (!cached) {
              try {
                BrowserUtil.get(driver, url, urlNode, false, toNewWindow, init);
              } catch (Retry r) {
                terminate = true;
                throw r;
              } catch (Fatal f) {
                terminate = true;
                throw f;
              } catch (Throwable t) {
                if (urlNode != null) {
                  BrowserUtil.newWindow(driver, init);
                }
                BrowserUtil.get(driver, url, false, init);
              }
              if (urlNode != null) {
                newHandle = driver.getWindowHandle();
              }
              BrowserUtil.doClicks(driver, postFetchClicks, null, null);
              content = driver.getPageSource();
              if (CommonUtil.isEmpty(content)) {
                cached = true;
              }
            }
            if (cached) {
              if (ScreenSlicerBatch.isCancelled(runGuid)) {
                return;
              }
              try {
                BrowserUtil.get(driver, toCacheUrl(url, false), false, init);
              } catch (Retry r) {
                terminate = true;
                throw r;
              } catch (Fatal f) {
                terminate = true;
                throw f;
              } catch (Throwable t) {
                BrowserUtil.get(driver, toCacheUrl(url, true), false, init);
              }
              content = driver.getPageSource();
            }
            content = NodeUtil.clean(content, driver.getCurrentUrl()).outerHtml();
            if (WebApp.DEBUG) {
              try {
                FileUtils.writeStringToFile(new File("./" + System.currentTimeMillis() + ".log.fetch"), content, "utf-8");
              } catch (IOException e) {}
            }
            //TODO make iframes work
            //            if (!CommonUtil.isEmpty(content)) {
            //              Document doc = Jsoup.parse(content);
            //              Elements docFrames = doc.getElementsByTag("iframe");
            //              List<WebElement> iframes = driver.findElementsByTagName("iframe");
            //              int cur = 0;
            //              for (WebElement iframe : iframes) {
            //                try {
            //                  driver.switchTo().frame(iframe);
            //                  String frameSrc = driver.getPageSource();
            //                  if (!CommonUtil.isEmpty(frameSrc) && cur < docFrames.size()) {
            //                    docFrames.get(cur).html(
            //                        Util.outerHtml(Jsoup.parse(frameSrc).body().childNodes()));
            //                  }
            //                } catch (Throwable t) {
            //                  Log.exception(t);
            //                }
            //                ++cur;
            //              }
            //              driver.switchTo().defaultContent();
            //              content = doc.outerHtml();
            //            }
            synchronized (resultLock) {
              result[0] = content;
            }
          } catch (Retry r) {
            terminate = true;
            throw r;
          } catch (Fatal f) {
            terminate = true;
            throw f;
          } catch (Throwable t) {
            Log.exception(t);
          } finally {
            synchronized (resultLock) {
              if (initVal.equals(result[0])) {
                result[0] = null;
              }
            }
            if (!terminate) {
              BrowserUtil.driverSleepRand(throttle);
              if (init && newHandle != null && origHandle != null) {
                try {
                  BrowserUtil.handleNewWindows(driver, origHandle, true);
                } catch (Retry r) {
                  throw r;
                } catch (Fatal f) {
                  throw f;
                } catch (Throwable t) {
                  Log.exception(t);
                }
              }
            }
          }
        }
      });
      thread.start();
      try {
        while (!started.get()) {
          try {
            Thread.sleep(WAIT);
          } catch (Throwable t) {}
        }
        thread.join(HANG_TIME);
        synchronized (resultLock) {
          if (initVal.equals(result[0])) {
            Log.exception(new Exception("Browser is hanging"));
            try {
              thread.interrupt();
            } catch (Throwable t) {
              Log.exception(t);
            }
            throw new Retry();
          }
          return result[0];
        }
      } catch (Retry r) {
        throw r;
      } catch (Fatal f) {
        throw f;
      } catch (Throwable t) {
        Log.exception(t);
      }
    }
    return null;
  }

  public static String get(Fetch fetch, Request req) {
    if (!isUrlValid(fetch.url)) {
      return null;
    }
    long myThread = latestThread.incrementAndGet();
    while (myThread != curThread.get() + 1
        || !done.compareAndSet(true, false)) {
      try {
        Thread.sleep(WAIT);
      } catch (Exception e) {
        Log.exception(e);
      }
    }
    if (!req.continueSession) {
      restart(req);
    }
    Log.info("Get URL " + fetch.url + ". Cached: " + fetch.fetchCached, false);
    String resp = "";
    try {
      resp = getHelper(driver, fetch.throttle, null, fetch.url, fetch.fetchCached, req.runGuid, true, true, fetch.postFetchClicks);
    } catch (Retry r) {
      throw r;
    } catch (Fatal f) {
      throw f;
    } catch (Throwable t) {
      Log.exception(t);
    } finally {
      curThread.incrementAndGet();
      done.set(true);
    }
    return resp;
  }

  private static SearchResults filterResults(SearchResults results, String[] whitelist,
      String[] patterns, HtmlNode[] urlNodes, UrlTransform[] urlTransforms, boolean forExport) {
    if (results == null) {
      return SearchResults.newInstance(true);
    }
    SearchResults ret;
    results = UrlUtil.transformUrls(results, urlTransforms, forExport);
    if ((whitelist == null || whitelist.length == 0)
        && (patterns == null || patterns.length == 0)
        && (urlNodes == null || urlNodes.length == 0)) {
      ret = results;
    } else {
      List<Result> filtered = new ArrayList<Result>();
      for (int i = 0; i < results.size(); i++) {
        if (!NodeUtil.isResultFiltered(results.get(i), whitelist, patterns, urlNodes)) {
          filtered.add(results.get(i));
        }
      }
      if (filtered.isEmpty() && !results.isEmpty()) {
        Log.warn("Filtered every url, e.g., " + results.get(0).url);
      }
      ret = SearchResults.newInstance(true, filtered, results);
    }
    return ret;
  }

  public static List<HtmlNode> loadForm(FormLoad context, Request req) throws ActionFailed {
    if (!isUrlValid(context.site)) {
      return new ArrayList<HtmlNode>();
    }
    long myThread = latestThread.incrementAndGet();
    while (myThread != curThread.get() + 1
        || !done.compareAndSet(true, false)) {
      try {
        Thread.sleep(WAIT);
      } catch (Exception e) {
        Log.exception(e);
      }
    }
    if (!req.continueSession) {
      restart(req);
    }
    try {
      List<HtmlNode> ret = null;
      try {
        ret = QueryForm.load(driver, context, true);
      } catch (Retry r) {
        throw r;
      } catch (Fatal f) {
        throw f;
      } catch (Throwable t) {
        if (!req.continueSession) {
          restart(req);
        }
        ret = QueryForm.load(driver, context, true);
      }
      return ret;
    } finally {
      curThread.incrementAndGet();
      done.set(true);
    }
  }

  private static void handlePage(Request req, Query query, int page, int depth,
      SearchResults allResults, SearchResults newResults, SearchResults recResults,
      List<String> resultPages, Map<String, Object> cache) throws ActionFailed, End {
    if (query.extract) {
      if (newResults.isEmpty()) {
        SearchResults tmpResults;
        try {
          tmpResults = ProcessPage.perform(driver, page, query);
        } catch (Retry r) {
          SearchResults.revalidate(driver, true);
          tmpResults = ProcessPage.perform(driver, page, query);
        }
        tmpResults = filterResults(tmpResults, query.urlWhitelist, query.urlPatterns,
            query.urlMatchNodes, query.urlTransforms, false);
        if (allResults.isDuplicatePage(tmpResults)) {
          throw new End();
        }
        if (query.results > 0 && allResults.size() + tmpResults.size() > query.results) {
          int remove = allResults.size() + tmpResults.size() - query.results;
          for (int i = 0; i < remove && !tmpResults.isEmpty(); i++) {
            tmpResults.remove(tmpResults.size() - 1);
          }
        }
        newResults.addPage(tmpResults);
      }
      if (query.fetch) {
        fetch(driver, req, query,
            query.keywordQuery == null ? (query.formQuery == null ? null : query.formQuery) : query.keywordQuery,
            newResults, depth, recResults, cache);
      }
      if (query.collapse) {
        for (int i = 0; i < newResults.size(); i++) {
          newResults.get(i).close();
        }
      }
      allResults.addPage(newResults);
    } else {
      resultPages.add(NodeUtil.clean(driver.getPageSource(), driver.getCurrentUrl()).outerHtml());
    }
  }

  public static List<Result> scrape(Query query, Request req) {
    long myThread = latestThread.incrementAndGet();
    while (myThread != curThread.get() + 1
        || !done.compareAndSet(true, false)) {
      try {
        Thread.sleep(WAIT);
      } catch (Exception e) {
        Log.exception(e);
      }
    }
    if (!req.continueSession) {
      restart(req);
    }
    try {
      Map<String, Object> cache = new HashMap<String, Object>();
      SearchResults ret = null;
      for (int i = 0; i < MAX_INIT; i++) {
        try {
          ret = scrape(query, req, 0, i + 1 == MAX_INIT, cache);
          Log.info("Scrape finished");
          return ret.drain();
        } catch (Fatal f) {
          Log.exception(f);
          Log.warn("Reinitializing state and resuming scrape...");
          restart(req);
        }
      }
      return null;
    } finally {
      curThread.incrementAndGet();
      done.set(true);
    }
  }

  private static SearchResults scrape(Query query, Request req, int depth,
      boolean fallback, Map<String, Object> cache) {
    CommonUtil.clearStripCache();
    NodeUtil.clearOuterHtmlCache();
    SearchResults results;
    SearchResults recResults;
    List<String> resultPages;
    if (cache.containsKey(Integer.toString(depth))) {
      Map<String, Object> curCache = (Map<String, Object>) cache.get(Integer.toString(depth));
      results = (SearchResults) curCache.get("results");
      recResults = (SearchResults) curCache.get("recResults");
      resultPages = (List<String>) curCache.get("resultPages");
    } else {
      Map<String, Object> curCache = new HashMap<String, Object>();
      cache.put(Integer.toString(depth), curCache);
      results = SearchResults.newInstance(false);
      curCache.put("results", results);
      recResults = SearchResults.newInstance(false);
      curCache.put("recResults", recResults);
      resultPages = new ArrayList<String>();
      curCache.put("resultPages", resultPages);
    }
    try {
      if (ScreenSlicerBatch.isCancelled(req.runGuid)) {
        throw new Cancelled();
      }
      if (query.isFormQuery()) {
        Log.info("FormQuery for URL " + query.site, false);
        try {
          QueryForm.perform(driver, (FormQuery) query, depth == 0);
        } catch (Throwable e) {
          if (depth == 0) {
            restart(req);
          }
          QueryForm.perform(driver, (FormQuery) query, depth == 0);
        }
      } else {
        Log.info("KewordQuery for URL " + query.site + ". Query: " + ((KeywordQuery) query).keywords, false);
        try {
          QueryKeyword.perform(driver, (KeywordQuery) query, depth == 0);
        } catch (Throwable e) {
          if (depth == 0) {
            restart(req);
          }
          QueryKeyword.perform(driver, (KeywordQuery) query, depth == 0);
        }
      }
      if (ScreenSlicerBatch.isCancelled(req.runGuid)) {
        throw new Cancelled();
      }
      String priorProceedLabel = null;
      for (int page = 1; (page <= query.pages || query.pages <= 0)
          && (results.size() < query.results || query.results <= 0); page++) {
        if (ScreenSlicerBatch.isCancelled(req.runGuid)) {
          throw new Cancelled();
        }
        if (page > 1) {
          if (!query.fetch) {
            try {
              BrowserUtil.driverSleepRand(query.throttle);
            } catch (Throwable t) {
              Log.exception(t);
            }
          }
          Log.info("Proceeding to page " + page);
          try {
            priorProceedLabel = Proceed.perform(driver, query.proceedClicks, page, priorProceedLabel);
          } catch (Retry r) {
            SearchResults.revalidate(driver, true);
            priorProceedLabel = Proceed.perform(driver, query.proceedClicks, page, priorProceedLabel);
          }
          if (ScreenSlicerBatch.isCancelled(req.runGuid)) {
            throw new Cancelled();
          }
        }
        if (query.currentPage() + 1 == page) {
          SearchResults newResults = SearchResults.newInstance(true);
          try {
            handlePage(req, query, page, depth, results, newResults, recResults, resultPages, cache);
          } catch (Retry r) {
            SearchResults.revalidate(driver, true);
            handlePage(req, query, page, depth, results, newResults, recResults, resultPages, cache);
          }
          query.markPage(page);
          query.markResult(0);
        }
      }
      query.markPage(0);
    } catch (End e) {
      Log.info("Reached end of results", false);
    } catch (Cancelled c) {
      Log.info("Cancellation requested.");
    } catch (Throwable t) {
      if (fallback) {
        Log.warn("Too many errors. Finishing scrape...");
      } else {
        throw new Fatal(t);
      }
    }
    cache.remove(Integer.toString(depth));
    if (query.extract) {
      if (recResults.isEmpty()) {
        return filterResults(results, query.urlWhitelist,
            query.urlPatterns, query.urlMatchNodes, query.urlTransforms, true);
      }
      if (query.collapse) {
        for (int i = 0; i < results.size(); i++) {
          results.get(i).remove();
        }
      }
      return recResults;
    }
    List<Result> pages = new ArrayList<Result>();
    for (String page : resultPages) {
      Result r = new Result();
      r.html = page;
      pages.add(r);
    }
    return SearchResults.newInstance(false, pages, null);
  }

  private static boolean isUrlValid(String url) {
    return !CommonUtil.isEmpty(url) && (url.startsWith("https://") || url.startsWith("http://"));
  }

  public static List<Result> scrape(String url, final String query, final int pages, final String mapKey1, final String mapKey2) {
    if (!isUrlValid(url)) {
      return new ArrayList<Result>();
    }
    if (!done.compareAndSet(true, false)) {
      return null;
    }
    restart(new Request());
    CommonUtil.clearStripCache();
    NodeUtil.clearOuterHtmlCache();
    List<Result> results = new ArrayList<Result>();
    final KeywordQuery keywordQuery = new KeywordQuery();
    try {
      synchronized (progressLock) {
        progress1Key = mapKey1;
        progress2Key = mapKey2;
        progress1 = "Page 1 progress: performing search query...";
        progress2 = "Page 2 progress: waiting for prior page extraction to finish...";
      }
      push(mapKey1, null);
      keywordQuery.site = url;
      keywordQuery.keywords = query;
      QueryKeyword.perform(driver, keywordQuery, true);
      synchronized (progressLock) {
        progress1 = "Page 1 progress: extracting results...";
      }
      results.addAll(ProcessPage.perform(driver, 1, keywordQuery).drain());
      synchronized (progressLock) {
        progress1 = "";
      }
    } catch (Throwable t) {
      Log.exception(t);
      push(mapKey1, results);
      synchronized (progressLock) {
        progress1 = "";
        progress2 = "Page 2 progress: prior page extraction was not completed.";
      }
      done.set(true);
      return results;
    }
    try {
      push(mapKey2, null);
      push(mapKey1, results);
    } catch (Throwable t) {
      Log.exception(t);
      synchronized (progressLock) {
        progress1 = "";
        progress2 = "Page 2 progress: prior page extraction was not completed.";
      }
      done.set(true);
      return results;
    }
    new Thread(new Runnable() {
      @Override
      public void run() {
        List<Result> next = new ArrayList<Result>();
        try {
          synchronized (progressLock) {
            progress2 = "Page 2 progress: getting page...";
          }
          Proceed.perform(driver, null, 2, query);
          synchronized (progressLock) {
            progress2 = "Page 2 progress: extracting results...";
          }
          next.addAll(ProcessPage.perform(driver, 2, keywordQuery).drain());
        } catch (End e) {
          Log.info("Reached end of results", false);
        } catch (Throwable t) {
          Log.exception(t);
        }
        finally {
          push(mapKey2, next);
          synchronized (progressLock) {
            progress2 = "";
          }
          done.set(true);
        }
      }
    }).start();
    return results;
  }
}
